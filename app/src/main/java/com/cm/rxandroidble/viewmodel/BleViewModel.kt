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
import com.cm.rxandroidble.data.SecureLocalData
import com.cm.rxandroidble.data.SecureLocalDataStore
import com.cm.rxandroidble.model.SleepDataModel
import com.cm.rxandroidble.util.Event
import com.cm.rxandroidble.util.L
import com.cm.rxandroidble.util.Util
import com.cm.rxandroidble.util.extension.SleepType
import com.cm.rxandroidble.util.extension.YYMMDDHHMMSS
import com.cm.rxandroidble.util.extension.byteArrayToHex
import com.cm.rxandroidble.util.extension.isMatchMeasureFormat
import com.cm.rxandroidble.util.extension.isMatchRawFormat
import com.cm.rxandroidble.util.extension.isSleepDataFormat
import com.cm.rxandroidble.util.extension.isSleepTimeFormat
import com.cm.rxandroidble.util.extension.removeFirstChar
import com.cm.rxandroidble.util.extension.removeLastChar
import com.cm.rxandroidble.util.extension.toHexString
import com.cm.rxandroidble.util.extension.withTimeoutAndCallback
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.RxBleDevice
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.scan.ScanFilter
import com.polidea.rxandroidble3.scan.ScanResult
import com.polidea.rxandroidble3.scan.ScanSettings
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.Timer
import kotlin.concurrent.schedule


internal class BleViewModel constructor(
    private val repository: BleRepository,
    private val secureLocalDataStore: SecureLocalDataStore,
) : ViewModel() {

    private var mScanSubscription: Disposable? = null
    private var mNotificationSubscription: Disposable? = null
    private var mWriteSubscription: Disposable? = null
    private lateinit var connectionStateDisposable: Disposable

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


    private val _event = MutableSharedFlow<MainEvent>()
    val event: Flow<MainEvent> = _event.asSharedFlow()


    data class ActionState(val readData: Double)
    data class MeasureState(val bpm: String, val brps: String, val sec: String)


    var isRead = false


    private var sleepDataModel = MainEvent.EVENT_DATA_REQ_COMPLETE("", "", ArrayList())


    private val _bleException = MutableLiveData<Event<Int>>()
    val bleException: LiveData<Event<Int>>
        get() = _bleException

    private val _listUpdate = MutableLiveData<Event<ArrayList<ScanResult>?>>()
    val listUpdate: LiveData<Event<ArrayList<ScanResult>?>>
        get() = _listUpdate


    private var sleepDataList: ArrayList<SleepDataModel> = ArrayList()

    // scan results
    private var scanResults: ArrayList<ScanResult>? = ArrayList()
    private val rxBleClient: RxBleClient = RxBleClient.create(MyApplication.applicationContext())

    private lateinit var timerJob: CompletableDeferred<Unit>

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
            .filter { model -> !model.bleDevice.name.isNullOrEmpty() }
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
        mScanSubscription?.dispose()
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
        // connect

        // register connectionStateListener
        connectionStateDisposable = device.observeConnectionStateChanges()
            .subscribe(
                { connectionState ->
                    connectionStateListener(device, connectionState)
                }
            ) { throwable ->
                throwable.printStackTrace()
            }

        repository.connectDevice(device) {
            registerNotification()
        }


    }


    private fun connectionStateListener(
        device: RxBleDevice,
        connectionState: RxBleConnection.RxBleConnectionState
    ) {
        L.i(":::connectionStateListener 타이밍 체크 " + connectionState)
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
                connectionStateDisposable.dispose()
            }

            RxBleConnection.RxBleConnectionState.DISCONNECTING -> {

            }
        }
    }


    private fun registerNotification() {
        L.i(":::::::registerNotification.. " + repository.enableNotifyObservable())
        mNotificationSubscription = repository.enableNotifyObservable().subscribe(
            { bytes ->
                // Given characteristic has been changes, here is the value.

                L.i(":::::::BleViewModel notification.. " + bytes.toHexString())
                val packet = String(bytes)
                readTxt.postValue(packet)

                if (packet.isMatchRawFormat()) {

                    try {
                        val lastRemove = packet.removeFirstChar()
                        val firstRemove = lastRemove.removeLastChar()

                        firstRemove.let {
                            viewModelScope.launch {
                                _actionState.emit(ActionState(readData = it.toDouble()))
                            }

                        }

                    } catch (e: Exception) {

                    }
                }


                //R123123123E
                if (packet.isMatchMeasureFormat()) {
                    val lastRemove = packet.removeFirstChar()
                    val firstRemove = lastRemove.removeLastChar()


                    firstRemove.let {
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


                if (packet.isSleepTimeFormat()) {
                    if (packet.startsWith("T1")) {
                        val coordinatePacket = packet.substring(2, packet.length - 1);
                        sleepDataModel = sleepDataModel.copy(startTime = coordinatePacket)
                    }

                    if (packet.startsWith("T2")) {
                        val coordinatePacket = packet.substring(2, packet.length - 1);
                        sleepDataModel = sleepDataModel.copy(endTime = coordinatePacket)
                    }
                }

                if (packet.isSleepDataFormat()) {
                    val lastRemove = packet.removeFirstChar()
                    val coordinatePacket = lastRemove.removeLastChar()

                    Timber.i(":::coordinatePacket => $coordinatePacket")

                    val sleepLevelIndex = 1
                    val durationStartIndex = 2

                    val sleepLevel =
                        coordinatePacket.substring(sleepLevelIndex, sleepLevelIndex + 1)
                    val isLast = coordinatePacket.substring(coordinatePacket.length - 1) == "0"
                    val duration =
                        coordinatePacket.substring(durationStartIndex, durationStartIndex + 4)
                            .toInt()

                    val model = SleepDataModel(sleepLevel = sleepLevel, isLast = isLast)

                    sleepDataList.add(model)

                    if (duration > 1) {
                        for (idx in 0 until duration.minus(1)) {
                            sleepDataList.add(model)
                        }
                    }
                    sleepDataModel = sleepDataModel.copy(items = sleepDataList)


                    if (isLast) {
                        timerJob.complete(Unit)
                    }

                }
            }, { throwable ->
                // Handle an error here
                throwable.printStackTrace()
                repository.disconnectDevice()
                L.i(":::::::error.. $throwable")
            })
    }

    // notify toggle
    fun onClickNotify() {
        if (!isRead) {
            isRead = true
            isNotify.set(true)
            val time = LocalDateTime.now().YYMMDDHHMMSS(SleepType.SLEEP_DATA_START)
            Timber.i("실시간 측정 시작 $time")
            send(time)
        } else {
            isRead = false
            isNotify.set(false)
            val time = LocalDateTime.now().YYMMDDHHMMSS(SleepType.SLEEP_DATA_STOP)
            Timber.i("실시간 측정 종료 $time")
            send(time)
            emitMainEvent(MainEvent.EVENT_CLEAR)
        }

    }


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
                val str: String = writeBytes.byteArrayToHex()
            }, { throwable ->
                // Handle an error here.
                throwable.printStackTrace()
            })
        }


    }

    fun checkTimeOut(packet: String) {
        emitMainEvent(MainEvent.EVENT_START_SLEEP)
        timerJob = CompletableDeferred()

        viewModelScope.launch {
            withTimeoutAndCallback(
                timeoutMillis = 10000, // 10-second timeout
                operation = {
                    sleepDataList.clear()
                    send(packet)
                },
                onTimeout = {
                    Timber.i("Timeout occurred")
                    emitMainEvent(MainEvent.EVENT_DATA_REQ_TIME_OUT)
                },
                onCompletion = {
                    Timber.i("Message sent successfully")
                    emitMainEvent(sleepDataModel)
                },
                job = timerJob
            )
        }

    }

    fun send(data: String) {
        val sendByteData = data.toByteArray(Charset.defaultCharset())
        val str: String = sendByteData.byteArrayToHex()
        Timber.i("sendByteData $str")

        mWriteSubscription = repository.writeData(sendByteData)?.subscribe({ writeBytes ->
            // Written data.
            val str: String = writeBytes.byteArrayToHex()
            Timber.i("writtenBytes", str)
        }, { throwable ->
            // Handle an error here.
            throwable.printStackTrace()
        })
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


    override fun onCleared() {
        super.onCleared()
        mScanSubscription?.dispose()
        repository.disconnectDevice()
        mWriteSubscription?.dispose()
        connectionStateDisposable.dispose()
    }

    private fun emitMainEvent(event: MainEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }
}

internal sealed class MainEvent {
    object EVENT_CLEAR : MainEvent()
    object EVENT_STOP_SLEEP : MainEvent()
    object EVENT_START_SLEEP : MainEvent()

    data class EVENT_DATA_REQ_COMPLETE(
        val startTime: String,
        val endTime: String,
        val items: ArrayList<SleepDataModel>
    ) : MainEvent()

    object EVENT_DATA_REQ_TIME_OUT : MainEvent()
}