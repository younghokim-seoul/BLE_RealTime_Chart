package com.cm.rxandroidble.viewmodel


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cm.rxandroidble.BleRepository
import com.cm.rxandroidble.MyApplication
import com.cm.rxandroidble.util.Event
import com.cm.rxandroidble.util.SampleDataSet.dump
import com.cm.rxandroidble.util.Util
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.schedule


class BleViewModel(private val repository: BleRepository) : ViewModel() {

    private lateinit var mScanSubscription: Disposable
    private var mNotificationSubscription: Disposable? = null
    private var mWriteSubscription: Disposable? = null
    private lateinit var connectionStateDisposable: Disposable

    val TAG = "BleViewModel"

    // View Databinding
    var statusTxt = ObservableField("Press the Scan button to start Ble Scan.")

    //test 시 false변경
    var scanVisible = ObservableBoolean(true)
    var readTxt = MutableLiveData("")
    var connectedTxt = ObservableField("")
    var isScanning = ObservableBoolean(false)
    var isConnecting = ObservableBoolean(false)
    var isConnect = ObservableBoolean(false)
    var isNotify = ObservableBoolean(false)


    private val _actionState = MutableSharedFlow<ActionState>(extraBufferCapacity = 1)
    val actionState: SharedFlow<ActionState> get() = _actionState.asSharedFlow()


    private val _measureState = MutableSharedFlow<MeasureState>(extraBufferCapacity = 1)
    val measureState: SharedFlow<MeasureState> get() = _measureState.asSharedFlow()


    data class ActionState(val readData: Double)
    data class MeasureState(val bpm: String, val brps: String, val sec: String)

    var isRead = false


    private val _bleException = MutableLiveData<Event<Int>>()
    val bleException: LiveData<Event<Int>>
        get() = _bleException

    private val _listUpdate = MutableLiveData<Event<ArrayList<ScanResult>?>>()
    val listUpdate: LiveData<Event<ArrayList<ScanResult>?>>
        get() = _listUpdate


    // scan results
    private var scanResults: ArrayList<ScanResult>? = ArrayList()
    private val rxBleClient: RxBleClient = RxBleClient.create(MyApplication.applicationContext())
//
//    val latestY: Flow<Double> = flow {
//        for (y in dump) {
//            emit(y.toDouble())
//            delay(10)
//        }
//    }



    private fun removeFirstChar(str: String?): String? {
        return str?.replaceFirst("^.".toRegex(), "")
    }

    private fun removeLastChar(str: String?): String? {
        return str?.replaceFirst(".$".toRegex(), "")
    }

    /**
     *  Start BLE Scan
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun onClickScan() {
        startScan()
    }

    fun startScan() {
        scanVisible.set(true)
        //scan filter
        val scanFilter: ScanFilter = ScanFilter.Builder()
//            .setServiceUuid(ParcelUuid(UUID.fromString(SERVICE_STRING)))
            //.setDeviceName("")
            .build()
        // scan settings
        // set low power scan mode
        val settings: ScanSettings = ScanSettings.Builder()
            //.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()


        scanResults = ArrayList() //list 초기화

        mScanSubscription = rxBleClient.scanBleDevices(settings, scanFilter)
            .subscribe({ scanResult ->
                addScanResult(scanResult)
            }, { throwable ->
                if (throwable is BleScanException) {
                    _bleException.postValue(Event(throwable.reason))
                } else {
                    Util.showNotification("UNKNOWN ERROR")
                }

            })


        isScanning.set(true)

        Timer("SettingUp", false).schedule(4000) { stopScan() }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScan() {
        mScanSubscription.dispose()
        isScanning.set(false)
        statusTxt.set("Scan finished. Click on the name to connect to the device.")

        scanResults = ArrayList() //list 초기화
    }

    /**
     * Add scan result
     */
    private fun addScanResult(result: ScanResult) {
        // get scanned device
        val device = result.bleDevice
        // get scanned device MAC address
        val deviceAddress = device.macAddress
        // add the device to the result list
        for (dev in scanResults!!) {
            if (dev.bleDevice.macAddress == deviceAddress) return
        }
        scanResults?.add(result)
        // log
        statusTxt.set("add scanned device: $deviceAddress")
        _listUpdate.postValue(Event(scanResults))
    }


    fun onClickDisconnect() {
        repository.disconnectDevice()
    }


    fun connectDevice(device: RxBleDevice) {
        // register connectionStateListener
        connectionStateDisposable = device.observeConnectionStateChanges()
            .subscribe(
                { connectionState ->
                    connectionStateListener(device, connectionState)
                }
            ) { throwable ->
                throwable.printStackTrace()
            }

        // connect
        repository.connectDevice(device)
    }


    private fun connectionStateListener(
        device: RxBleDevice,
        connectionState: RxBleConnection.RxBleConnectionState
    ) {


        when (connectionState) {
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                isConnect.set(true)
                isConnecting.set(false)
                scanVisible.set(false)
                connectedTxt.set("${device.macAddress} Connected.")
            }
            RxBleConnection.RxBleConnectionState.CONNECTING -> {
                isConnecting.set(true)
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                isConnect.set(false)
                isConnecting.set(false)
                scanVisible.set(true)
                scanResults = ArrayList()
                _listUpdate.postValue(Event(scanResults))
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTING -> {

            }
        }
    }


    // notify toggle
    fun onClickNotify() {

        if (!isRead) {
            mNotificationSubscription = repository.bleNotification()
                ?.subscribe({ bytes ->
                    // Given characteristic has been changes, here is the value.
                    val packet = String(bytes)
//                    readTxt.postValue(byteArrayToHex(bytes))
                    readTxt.postValue(packet)

                    if (packet.isMatchRawFormat()) {

                        try {
                            val lastRemove = removeFirstChar(packet)
                            val firstRemove = removeLastChar(lastRemove)

                            firstRemove?.let {
                                viewModelScope.launch {
                                    _actionState.emit(ActionState(readData = it.toDouble()))
                                }

                            }

                        } catch (e: Exception) {

                        }
                    }


                    //R123123123E
                    if (packet.isMatchMeasureFormat()) {
                        val lastRemove = removeFirstChar(packet)
                        val firstRemove = removeLastChar(lastRemove)


                        firstRemove?.let {
                            val list = it.chunked(3)

                            try {
                                viewModelScope.launch {
                                    _measureState.emit(
                                        MeasureState(
                                            bpm = list[0],
                                            brps = list[1],
                                            sec = list[2]
                                        )
                                    )
                                }
                            } catch (e: Exception) {

                            }

                        }
                    }

                    isRead = true
                    isNotify.set(true)

                }, { throwable ->
                    // Handle an error here
                    throwable.printStackTrace()
                    repository.disconnectDevice()
                    isRead = false
                    isNotify.set(false)
                })
        } else {
            isRead = false
            isNotify.set(false)
            mNotificationSubscription?.dispose()
        }

    }

    private fun String.isMatchRawFormat(): Boolean = this.startsWith("S") && this.endsWith("E")
    private fun String.isMatchMeasureFormat(): Boolean = this.startsWith("R") && this.endsWith("E")


    // write
    fun writeData(data: String, type: String) {

        var sendByteData: ByteArray? = null
        when (type) {
            "string" -> {
                sendByteData = data.toByteArray(Charset.defaultCharset())
            }
            "byte" -> {
                if (data.length % 2 != 0) {
                    Util.showNotification("Byte Size Error")
                    return
                }
                sendByteData = hexStringToByteArray(data)
            }
        }
        if (sendByteData != null) {
            mWriteSubscription = repository.writeData(sendByteData)?.subscribe({ writeBytes ->
                // Written data.
                val str: String = byteArrayToHex(writeBytes)
                Log.d("writtenBytes", str)
                viewModelScope.launch {
                    Util.showNotification("`$str` is written.")
                }
            }, { throwable ->
                // Handle an error here.
                throwable.printStackTrace()
            })
        }


    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }


    private fun byteArrayToHex(a: ByteArray): String {
        val sb = java.lang.StringBuilder(a.size * 2)
        for (b in a) sb.append(String.format("%02x", b))
        return sb.toString()
    }


    override fun onCleared() {
        super.onCleared()
        mScanSubscription.dispose()
        repository.disconnectDevice()
        mWriteSubscription?.dispose()
        connectionStateDisposable.dispose()
    }


}