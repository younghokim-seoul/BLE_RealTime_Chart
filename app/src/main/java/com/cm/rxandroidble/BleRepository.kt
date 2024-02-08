package com.cm.rxandroidble

import com.cm.rxandroidble.util.L
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.RxBleDevice
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.UUID


class BleRepository {

    var rxBleConnection: RxBleConnection? = null
    private var mConnectSubscription: Disposable? = null

    private lateinit var notificationObservable: Observable<ByteArray>

    /**
     * Connect & Discover Services
     * @Saved rxBleConnection
     */
    fun connectDevice(
        device: RxBleDevice,
        onComplete: () -> Unit
    ) {
        mConnectSubscription = device.establishConnection(false) // <-- autoConnect flag
            .flatMapSingle {
                // All GATT operations are done through the rxBleConnection.
                L.i(":::타이밍 체크")
                rxBleConnection = it
                // Discover services
                it.discoverServices()
                Single.just(it)
            }.flatMap {
                it.setupNotification(UUID.fromString(CHARACTERISTIC_RESPONSE_STRING))
            }
            .subscribe({
                notificationObservable = it
                onComplete.invoke()
            }, {

            })
    }



    fun disconnectDevice() {
        mConnectSubscription?.dispose()
    }


    fun enableNotifyObservable() = notificationObservable

    /**
     * Notification
     */
    fun bleNotification() = rxBleConnection
        ?.setupNotification(UUID.fromString(CHARACTERISTIC_RESPONSE_STRING))
        ?.doOnNext { notificationObservable ->
            // Notification has been set up
            notificationObservable
        }
        ?.flatMap { notificationObservable -> notificationObservable }

    /**
     * Read
     */
    fun bleRead() =
        rxBleConnection?.readCharacteristic(UUID.fromString(CHARACTERISTIC_RESPONSE_STRING))


    /**
     * Write Data
     */
    fun writeData(sendByteData: ByteArray) = rxBleConnection?.writeCharacteristic(
        UUID.fromString(CHARACTERISTIC_COMMAND_STRING),
        sendByteData
    )


}