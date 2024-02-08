package com.cm.rxandroidble.ui.widget

import android.graphics.Color
import com.cm.rxandroidble.model.SleepDataModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.Utils
import timber.log.Timber
import kotlin.random.Random

class LineChartGradientHelper {


    fun setup(chart: LineChart, sleepArrayList: ArrayList<SleepDataModel>) {
        chart.apply {
            setupChartMainParameters()
            setupLabelX()
            setupLabelY()
            data = sleepArrayList.createDataSet()
//            data = generateRandomNumbers()
            animateXY(10, 1000) // animate start draw value
            setupLegend() // must call after draw
            customRenderer()

        }
    }

    private fun ArrayList<SleepDataModel>.createDataSet(): LineData {
        val entry = mutableListOf<Entry>()
        val chartData = LineData()
        this.forEachIndexed { index, model ->
            entry.add(Entry(index.toFloat(), model.sleepLevel.toFloat()))
        }
        val dataSet = LineDataSet(entry, "")
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = Utils.convertDpToPixel(1f)
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        chartData.addDataSet(dataSet)
        return chartData
    }

    private fun generateRandomNumbers(): LineData {
        val entry = mutableListOf<Entry>()
        val chartData = LineData()
        for (i in 0..50) {
            val mappedValue = when (Random.nextInt(1, 101)) { // 1부터 4까지 랜덤 숫자 생성
                in 1..10 -> 1    // 1-25 maps to 1
                in 26..50 -> 4   // 26-50 maps to 2
//                in 51..75 -> 3   // 51-75 maps to 3
                else -> 4        // 76-100 maps to 4
            }
            entry.add(Entry(i.toFloat(), mappedValue.toFloat()))
        }
        val dataSet = LineDataSet(entry, "")
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = Utils.convertDpToPixel(1f)
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        chartData.addDataSet(dataSet)
        return chartData
    }


    private fun LineChart.setupChartMainParameters() {
        setTouchEnabled(false)
        isDragEnabled = false
        setScaleEnabled(false)
        setPinchZoom(false)
        description = Description().apply { text = "" }
        xAxis.setDrawGridLines(true)
        axisLeft.setDrawGridLines(false)
        axisRight.setDrawGridLines(false)
        setSleepSubLineEnabled(false)

    }

    private fun LineChart.setupLabelX() {
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(true)
            setDrawGridLines(true)
            labelCount = 12
            granularity = 1f
        }
    }

    private fun LineChart.setupLabelY() {

        axisRight.setDrawAxisLine(false)
        axisRight.isEnabled = true
        axisRight.setDrawLabels(false)
        axisLeft.isEnabled = false
        this.isAutoScaleMinMaxEnabled = false
        this.axisRight.axisMinimum = 0f
        this.axisRight.axisMaximum = 5f
        this.axisRight.labelCount = 4

        getLimitLines().forEach {
            this.axisRight.addLimitLine(it)
        }

        this.axisLeft.axisMinimum = 0f
        this.axisLeft.axisMaximum = 5f
        this.axisLeft.labelCount = 4
    }

    private fun getLimitLines(): List<LimitLine> {
        return listOf(
            LimitLine(1f).apply {
                lineWidth = 1f
                enableDashedLine(4F, 10F, 10F)
                lineColor = Color.BLACK
                label = "깊은 수면"
                labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM

            },
            LimitLine(2f).apply {
                lineWidth = 1f
                enableDashedLine(4F, 10F, 10F)
                lineColor = Color.BLACK
                label = "얕은 수면"
                labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
            },
            LimitLine(3f).apply {
                lineWidth = 1f
                enableDashedLine(4F, 10F, 10F)
                lineColor = Color.BLACK
                label = "렘 수면"
                labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
            },
            LimitLine(4f).apply {
                lineWidth = 1f
                enableDashedLine(4F, 10F, 10F)
                lineColor = Color.BLACK
                label = "비 수면"
                labelPosition = LimitLine.LimitLabelPosition.LEFT_BOTTOM
            },
        )

    }

    private fun LineChart.setupLegend() {
        val l: Legend = legend
        l.isEnabled = false
        l.form = LegendForm.LINE

    }

    private fun LineChart.customRenderer() {
        val renderer = renderer



        if (renderer is MyLineChartRenderer) {

            val thresholds = getThreshold()

            val medium = thresholds.first
            val larger = thresholds.second
            val limit = thresholds.third

            val colors = intArrayOf(
                Color.parseColor("#FF7F50"),
                Color.parseColor("#00CED1"),
                Color.parseColor("#1E90FF"),
                Color.parseColor("#00008B")
            )

            renderer.setHeartLine(true, medium, larger, limit, colors)
        }
    }

    private fun getThreshold(): Triple<Int, Int, Int> {
        return Triple(1, 2, 3)
    }
}