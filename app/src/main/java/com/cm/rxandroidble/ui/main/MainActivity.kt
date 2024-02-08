package com.cm.rxandroidble.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cm.rxandroidble.R
import com.cm.rxandroidble.databinding.ActivityMainBinding
import com.cm.rxandroidble.ui.adapter.BleListAdapter
import com.cm.rxandroidble.ui.detail.DetailActivity
import com.cm.rxandroidble.ui.dialog.WriteDialog
import com.cm.rxandroidble.ui.sleep.SleepModeActivity
import com.cm.rxandroidble.util.Util
import com.cm.rxandroidble.util.extension.SleepType
import com.cm.rxandroidble.util.extension.YYMMDDHHMMSS
import com.cm.rxandroidble.util.extension.removeFirstChar
import com.cm.rxandroidble.util.extension.removeLastChar
import com.cm.rxandroidble.util.extension.setting
import com.cm.rxandroidble.util.extension.startActivity
import com.cm.rxandroidble.viewmodel.BleViewModel
import com.cm.rxandroidble.viewmodel.MainEvent
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.scan.ScanResult
import com.skydoves.bundler.intentOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {

    private val mainViewModel by viewModel<BleViewModel>()
    private var adapter: BleListAdapter? = null

    private var requestEnableBluetooth = false
    private var askGrant = false

    private lateinit var binding: ActivityMainBinding


    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val PERMISSIONS_S_ABOVE = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val LOCATION_PERMISSION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val REQUEST_ALL_PERMISSION = 1
        val REQUEST_LOCATION_PERMISSION = 2
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = mainViewModel

        binding.rvBleList.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvBleList.layoutManager = layoutManager


        adapter = BleListAdapter()
        binding.rvBleList.adapter = adapter
        adapter?.setItemClickListener(object : BleListAdapter.ItemClickListener {
            override fun onClick(view: View, scanResult: ScanResult?) {
                val device = scanResult?.bleDevice
                if (device != null) {
                    mainViewModel.connectDevice(device)
                }
            }
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermissions(this, PERMISSIONS_S_ABOVE)) {
                requestPermissions(PERMISSIONS_S_ABOVE, REQUEST_ALL_PERMISSION)
            }
        } else {
            if (!hasPermissions(this, PERMISSIONS)) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSION)
            }
        }

        initObserver(binding)
        chartSetting(binding)
        buttonEvent()

    }





    private fun buttonEvent() {
        binding.btnSleepMode.setOnClickListener {
            startActivity<SleepModeActivity>()
        }

        binding.btnSleepData.setOnClickListener {
            val reqDataSleep = LocalDateTime.now().YYMMDDHHMMSS(SleepType.DATA_SLEEP_REQ)
            mainViewModel.checkTimeOut(reqDataSleep)

        }
    }


    private fun chartSetting(binding: ActivityMainBinding) {
        binding.chartRealtime.setting()
    }

    private fun initObserver(binding: ActivityMainBinding) {
        mainViewModel.apply {
            bleException.observe(this@MainActivity, Observer {
                it.getContentIfNotHandled()?.let { reason ->
                    mainViewModel.stopScan()
                    bleThrowable(reason)
                }
            })

            listUpdate.observe(this@MainActivity, Observer {
                it.getContentIfNotHandled()?.let { scanResults ->
                    adapter?.setItem(scanResults)
                }
            })

            readTxt.observe(this@MainActivity, Observer {
                binding.txtRead.append("$it\n")
                if ((binding.txtRead.measuredHeight - binding.scroller.scrollY) <=
                    (binding.scroller.height + binding.txtRead.lineHeight)
                ) {
                    binding.scroller.post {
                        binding.scroller.scrollTo(0, binding.txtRead.bottom)
                    }
                }
            })


            measureState.onEach {
                binding.tickerPpg.text = it.bpm
                binding.tickerSec.text = it.sec
                binding.tickerBreath.text = it.brps
            }.launchIn(lifecycleScope)

            actionState.onEach {
                addEntry(it.readData)
            }.launchIn(lifecycleScope)

            event.onEach {
                when (it) {
                    is MainEvent.EVENT_CLEAR -> {
                        binding.txtRead.text = ""
                        binding.chartRealtime.clear()
                    }

                    is MainEvent.EVENT_STOP_SLEEP -> {
                        Util.showNotification("수면 종료 되었습니다.")
                    }

                    is MainEvent.EVENT_START_SLEEP -> {
                        Util.showNotification("데이터 수집을 시작합니다.")
                    }

                    is MainEvent.EVENT_DATA_REQ_COMPLETE -> {
                        Util.showNotification("데이터 수집 완료 되었습니다.")
                        intentOf<DetailActivity> {
                            putExtra("startTime",it.startTime)
                            putExtra("endTime",it.endTime)
                            putExtra("sleepArrayList", it.items)
                            startActivity(this@MainActivity)
                        }
                    }

                    is MainEvent.EVENT_DATA_REQ_TIME_OUT -> {
                        Util.showNotification("데이터 수집 시간 초과")

                    }

                }


            }.launchIn(lifecycleScope)

        }


    }

    private fun bleThrowable(reason: Int) = when (reason) {
        BleScanException.BLUETOOTH_DISABLED -> {
            requestEnableBluetooth = true
            requestEnableBLE()
        }

        BleScanException.LOCATION_PERMISSION_MISSING -> {
            requestPermissions(LOCATION_PERMISSION, REQUEST_LOCATION_PERMISSION)
        }

        else -> {
            Util.showNotification(bleScanExceptionReasonDescription(reason))
        }
    }

    private fun bleScanExceptionReasonDescription(reason: Int): String {
        return when (reason) {
            BleScanException.BLUETOOTH_CANNOT_START -> "Bluetooth cannot start"
            BleScanException.BLUETOOTH_DISABLED -> "Bluetooth disabled"
            BleScanException.BLUETOOTH_NOT_AVAILABLE -> "Bluetooth not available"
            BleScanException.LOCATION_SERVICES_DISABLED -> "Location Services disabled"
            BleScanException.SCAN_FAILED_ALREADY_STARTED -> "Scan failed because it has already started"
            BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Scan failed because application registration failed"
            BleScanException.SCAN_FAILED_INTERNAL_ERROR -> "Scan failed because of an internal error"
            BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Scan failed because feature unsupported"
            BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Scan failed because out of hardware resources"
            BleScanException.UNDOCUMENTED_SCAN_THROTTLE -> "Undocumented scan throttle"
            BleScanException.UNKNOWN_ERROR_CODE -> "Unknown error"
            else -> "Unknown error"
        }
    }

    override fun onResume() {
        super.onResume()
        // finish app if the BLE is not supported
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }

    fun onClickWrite(view: View) {
        val writeDialog = WriteDialog(this@MainActivity, object : WriteDialog.WriteDialogListener {
            override fun onClickSend(data: String, type: String) {
                mainViewModel.writeData(data, type)
            }
        })
        writeDialog.show()
    }


    private val requestEnableBleResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Util.showNotification("Bluetooth기능을 허용하였습니다.")
                mainViewModel.startScan()
            } else {
                Util.showNotification("Bluetooth기능을 켜주세요.")
                mainViewModel.stopScan()
            }
            requestEnableBluetooth = false
        }

    /**
     * Request BLE enable
     */
    private fun requestEnableBLE() {
        val bleEnableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestEnableBleResult.launch(bleEnableIntent)
    }

    private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (context?.let { ActivityCompat.checkSelfPermission(it, permission) }
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    // Permission check
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //when (requestCode) {
        //REQUEST_LOCATION_PERMISSION -> {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(permissions, REQUEST_LOCATION_PERMISSION)
            Toast.makeText(this, "Permissions must be granted!", Toast.LENGTH_SHORT).show()
            askGrant = false
        }
        //}
        //}
    }


    private fun addEntry(num: Double) {
        var data: LineData? = binding.chartRealtime.data
        if (data == null) {
            data = LineData()
            binding.chartRealtime.data = data
        }
        var set = data.getDataSetByIndex(0)
        // set.addEntry(...); // can be called as well
        if (set == null) {
            set = createSet()
            data.addDataSet(set)
        }
        data.addEntry(
            Entry(set.entryCount.toFloat(), num.toFloat()),
            0
        )
        data.notifyDataChanged()

        // let the chart know it's data has changed
        binding.chartRealtime.notifyDataSetChanged()
        binding.chartRealtime.setVisibleXRangeMaximum(100f)
        // this automatically refreshes the chart (calls invalidate())
        binding.chartRealtime.moveViewTo(data.entryCount.toFloat(), 50f, YAxis.AxisDependency.LEFT)
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "")
        set.lineWidth = 1f
        set.setDrawValues(false)
        set.valueTextColor = Color.WHITE
        set.color = Color.WHITE
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.setDrawCircles(false)
        set.highLightColor = Color.rgb(190, 190, 190)
        return set
    }


}