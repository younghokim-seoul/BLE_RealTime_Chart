package com.cm.rxandroidble.util.extension

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis


fun LineChart.setting() {
    this.setDrawGridBackground(true);
    this.setBackgroundColor(Color.BLACK);
    this.setGridBackgroundColor(Color.BLACK);
    this.description.isEnabled = true
    val des = this.description
    des.apply {
        isEnabled = true
        text = "Real-Time DATA"
        textSize= 15f
        textColor = Color.WHITE
    }


// touch gestures (false-비활성화)
    this.setTouchEnabled(true)

// scaling and dragging (false-비활성화)
    this.isDragEnabled = true
    this.isScaleXEnabled = true


//auto scale
    this.isAutoScaleMinMaxEnabled = true

// if disabled, scaling can be done on x- and y-axis separately
    this.setPinchZoom(false)


//X축
    this.xAxis.setDrawGridLines(true)
    this.xAxis.setDrawAxisLine(false)

    this.xAxis.isEnabled = true
    this.xAxis.setDrawGridLines(false)



//Legend
    val l: Legend = this.getLegend()
    l.isEnabled = false
    l.formSize = 10f // set the size of the legend forms/shapes

    l.textSize = 12f
    l.textColor = Color.WHITE

//Y축
    val leftAxis: YAxis = this.axisLeft
    leftAxis.isEnabled = true
    leftAxis.textColor = Color.GRAY
    leftAxis.setDrawGridLines(true)
    leftAxis.gridColor =  Color.GRAY

    val rightAxis: YAxis = this.axisRight
    rightAxis.isEnabled = false

// don't forget to refresh the drawing
    this.invalidate()
}

