package com.cm.rxandroidble.viewmodel

import androidx.lifecycle.viewModelScope
import autodispose2.autoDispose
import com.cm.rxandroidble.BleRepository
import com.cm.rxandroidble.data.SecureLocalDataStore
import com.cm.rxandroidble.util.extension.SleepType
import com.cm.rxandroidble.util.extension.byteArrayToHex
import com.cm.rxandroidble.util.extension.extractCharSleepType
import com.cm.rxandroidble.util.extension.isMatchSleepFormat
import com.cm.rxandroidble.util.extension.toHexString
import com.cm.rxandroidble.util.uber.AutoDisposeViewModel
import com.github.mikephil.charting.utils.L
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.Charset

internal class SleepModeViewModel(
    private val repository: BleRepository,
    private val secureLocalDataStore: SecureLocalDataStore,
) : AutoDisposeViewModel() {


    private val _event = MutableSharedFlow<SleepModeEvent>()
    val event: Flow<SleepModeEvent> = _event.asSharedFlow()


    fun send(data: String) {
        val sendByteData = data.toByteArray(Charset.defaultCharset())
        val str: String = sendByteData.byteArrayToHex()

        Timber.i("sendByteData $str")

        repository.writeData(sendByteData)
            ?.doOnDispose {
                L.i("writeData Disposing subscription from the ViewModel")
            }
            ?.autoDispose(this)?.subscribe(
                {
                    Timber.i("writtenBytes success")
                }, { error ->
                    L.e("send error $error")
                }
            )
    }

    fun setDeviceNotification() {


        repository.enableNotifyObservable().doOnDispose {
                L.i("bleNotification Disposing subscription from the ViewModel")
            }.autoDispose(this)?.subscribe { bytes ->
                //Sleep Packet
                com.cm.rxandroidble.util.L.i(":::::::SleepModeViewModel notification.. " + bytes.toHexString())
                val packet = String(bytes)

                if (packet.isMatchSleepFormat()) {
                    val sleepEvent = packet.extractCharSleepType()

                    when (SleepType.of(sleepEvent)) {
                        SleepType.START_SLEEP -> {
                            emitSleepModeEvent(SleepModeEvent.EVENT_SLEEP_START)
                        }

                        SleepType.STOP_SLEEP -> {
                            emitSleepModeEvent(SleepModeEvent.EVENT_SLEEP_STOP)
                        }

                        else -> {}
                    }
                }

            }
    }

    private fun emitSleepModeEvent(event: SleepModeEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }
}

internal sealed class SleepModeEvent {
    object EVENT_SLEEP_START : SleepModeEvent()
    object EVENT_SLEEP_STOP : SleepModeEvent()
}