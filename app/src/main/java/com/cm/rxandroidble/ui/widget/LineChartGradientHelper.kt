package com.cm.rxandroidble.ui.widget

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.Utils
import kotlin.math.abs
import kotlin.random.Random

class LineChartGradientHelper(private val context: Context) {


    fun setup(chart: LineChart) {
        chart.apply {
            setupChartMainParameters()
            setupLabelX()
            setupLabelY()
            data = generateRandomNumbers()
            animateXY(10, 1000) // animate start draw value
            setupLegend() // must call after draw
            customRenderer()
        }
    }


    private fun generateRandomNumbers(): LineData {
        val entry  = mutableListOf<Entry>()
        val chartData = LineData()
        for (i in 0..100) {
            val mappedValue = when (Random.nextInt(1, 101)) { // 1부터 4까지 랜덤 숫자 생성
                in 1..25 -> 1    // 1-25 maps to 1
                in 26..50 -> 2   // 26-50 maps to 2
                in 51..75 -> 3   // 51-75 maps to 3
                else -> 4        // 76-100 maps to 4
            }
            entry.add(Entry(i.toFloat(), mappedValue.toFloat()))
        }
        val dataSet = LineDataSet(entry,"")
        dataSet.setDrawValues(false)
        dataSet.lineWidth = Utils.convertDpToPixel(0.5f)
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
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(false)
        axisRight.setDrawGridLines(false)
    }

    private fun LineChart.setupLabelX() {
//        val xAxisLabel = getMonthList()
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
//            setValueFormatter { value, _ ->
//                try {
//                    xAxisLabel[abs(value.toDouble().toInt())]
//                } catch (e: Exception) {
//                    ""
//                }
//            }
            setDrawAxisLine(false)
            labelCount = 12
            granularity = 1f
        }
    }

    private fun LineChart.setupLabelY() {
        axisRight.apply {
            setDrawAxisLine(false)
            setValueFormatter { value, _ ->
                try {
                    "$${String.format("%.1f", value)}"
                } catch (e: Exception) {
                    "$value"
                }
            }
            isEnabled = true
        }
        axisRight.isEnabled = true
        axisLeft.isEnabled = false
    }

    private fun LineChart.setupLegend() {
        val l: Legend = legend
        l.isEnabled = false
        l.form = LegendForm.LINE
    }

    private fun LineChart.customRenderer() {
        val renderer = renderer

        if(renderer is MyLineChartRenderer){
            val medium = 1
            val larger = 2
            val limit = 3

            val colors = intArrayOf(
                Color.parseColor("#fa7069"),
                Color.parseColor("#faa369"),
                Color.parseColor("#facd69"),
                Color.parseColor("#d3d8dc")
            )

            renderer.setHeartLine(true,medium, larger, limit, colors)
        }
    }
}