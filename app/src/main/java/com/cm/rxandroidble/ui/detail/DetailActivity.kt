package com.cm.rxandroidble.ui.detail


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.cm.rxandroidble.R
import com.cm.rxandroidble.databinding.ActivityDetailBinding
import com.cm.rxandroidble.ui.widget.LineChartGradientHelper


class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    private val chartHelper : LineChartGradientHelper by lazy { LineChartGradientHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        chartHelper.setup(binding.graphicView)
    }






}