package com.cm.rxandroidble.ui.sleep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.cm.rxandroidble.R
import com.cm.rxandroidble.databinding.ActivitySleepSettingBinding
import com.cm.rxandroidble.util.Util
import com.cm.rxandroidble.util.extension.SleepType
import com.cm.rxandroidble.util.extension.YYMMDDHHMMSS
import com.cm.rxandroidble.viewmodel.SleepModeEvent
import com.cm.rxandroidble.viewmodel.SleepModeViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.time.LocalDateTime

class SleepModeActivity : AppCompatActivity() {

    private val viewModel by viewModel<SleepModeViewModel>()
    private lateinit var binding: ActivitySleepSettingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sleep_setting)

        onObserve()
        setUiEvent()
        startSleepMode()

    }

    private fun setUiEvent() {
        binding.buttonEvent()
    }

    private fun startSleepMode() {
        val time = LocalDateTime.now().YYMMDDHHMMSS(SleepType.START_SLEEP)
        Timber.i("수면 측정 모드 시작 $time")
        viewModel.send(time)
        viewModel.setDeviceNotification()
    }

    private fun ActivitySleepSettingBinding.buttonEvent() {
        this.stopSleepMode.setOnClickListener {
            val time = LocalDateTime.now().YYMMDDHHMMSS(SleepType.STOP_SLEEP)
            Timber.i("수면 종료 $time")
            viewModel.send(time)
        }
    }

    private fun onObserve() {
        viewModel.apply {

            event.onEach {
                when (it) {
                    SleepModeEvent.EVENT_SLEEP_START -> {
                        Util.showNotification("수면 측정이 시작 되었습니다.")
                    }

                    SleepModeEvent.EVENT_SLEEP_STOP -> {
                        Util.showNotification("수면 측정이 종료 되었습니다.")
                        finish()
                    }

                }

            }.launchIn(lifecycleScope)
        }
    }
}