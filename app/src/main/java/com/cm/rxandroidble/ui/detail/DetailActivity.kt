package com.cm.rxandroidble.ui.detail


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.cm.rxandroidble.R
import com.cm.rxandroidble.databinding.ActivityDetailBinding
import com.cm.rxandroidble.model.SleepDataModel
import com.cm.rxandroidble.ui.widget.LineChartGradientHelper
import com.cm.rxandroidble.util.extension.calPercentage
import com.cm.rxandroidble.util.extension.formatMinutes
import com.cm.rxandroidble.util.extension.sleepTimeSplit
import com.hitanshudhawan.spannablestringparser.spannify
import com.skydoves.bundler.bundle
import com.skydoves.bundler.bundleArrayList
import timber.log.Timber


class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private val chartHelper: LineChartGradientHelper by lazy { LineChartGradientHelper() }
    private val startTime: String by bundle("startTime", "")
    private val endTime: String by bundle("endTime", "")
    private val sleepArrayList by bundleArrayList<SleepDataModel>("sleepArrayList")


    private val stageCountArray: Array<Int> = Array(4) { 0 }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)

        binding.buttonSetting()
        binding.progressSetting()

        drawChart()
        drawProgress()
    }


    private fun setStartTime() {
        if(startTime.isNotEmpty()){
            val startTimes = startTime.sleepTimeSplit()
            val year = startTimes.first()
            val month = startTimes[1]
            val day = startTimes[2]
            val hour = startTimes[3]
            val minute = startTimes.last()

            binding.tvStartTime.text = "$hour{`시` < text-size:12dp />} $minute{`분` < text-size:12dp />} ".spannify()
            binding.tvStartDate.text = year + "년 " + month + "월 " + day + "일"
        }

    }

    private fun setEndTime() {
        if(endTime.isNotEmpty()){
            val endTimes = endTime.sleepTimeSplit()
            val year = endTimes.first()
            val month = endTimes[1]
            val day = endTimes[2]
            val hour = endTimes[3]
            val minute = endTimes.last()

            binding.tvEndTime.text = "$hour{`시` < text-size:12dp />} $minute{`분` < text-size:12dp />} ".spannify()
            binding.tvEndDate.text = year + "년 " + month + "월 " + day + "일"
        }
    }


    private fun drawChart() {
        sleepArrayList?.let { chartHelper.setup(binding.graphicView, it) }
    }

    private fun drawProgress() {
        sleepArrayList?.let {
            val totalSize = it.size

            val stage1Count = it.count { model -> model.sleepLevel == "1" }
            val stage1Percent = stage1Count.toFloat().calPercentage(totalSize)

            stageCountArray[0] = stage1Count

            val stage2Count = it.count { model -> model.sleepLevel == "2" }
            val stage2Percent = stage2Count.toFloat().calPercentage(totalSize)

            stageCountArray[1] = stage2Count

            val stage3Count = it.count { model -> model.sleepLevel == "3" }
            val stage3Percent = stage3Count.toFloat().calPercentage(totalSize)

            stageCountArray[2] = stage3Count

            val stage4Count = it.count { model -> model.sleepLevel == "4" }
            val stage4Percent = stage4Count.toFloat().calPercentage(totalSize)

            stageCountArray[3] = stage4Count

            binding.state1Progress.progress = stage4Percent
            binding.state2Progress.progress = stage3Percent
            binding.state3Progress.progress = stage2Percent
            binding.state4Progress.progress = stage1Percent


            setStartTime()
            setEndTime()
        }
    }

    private fun ActivityDetailBinding.buttonSetting() {
        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun ActivityDetailBinding.progressSetting() {
        state1Progress.setOnProgressChangeListener {
            state1Progress.labelText = "비수면 ${it.toInt()}% (${stageCountArray[3].formatMinutes()})"
        }
        state2Progress.setOnProgressChangeListener {
            state2Progress.labelText = "렘 수면 ${it.toInt()}% (${stageCountArray[2].formatMinutes()})"
        }
        state3Progress.setOnProgressChangeListener {
            state3Progress.labelText =
                "얕은 수면 ${it.toInt()}% (${stageCountArray[1].formatMinutes()})"
        }
        state4Progress.setOnProgressChangeListener {
            state4Progress.labelText =
                "깊은 수면 ${it.toInt()}% (${stageCountArray[0].formatMinutes()})"
        }
    }

}