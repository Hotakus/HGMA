package com.hotakus.hgma

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.hotakus.hgma.databinding.ActivityMainBinding
import androidx.core.app.ActivityCompat.startActivityForResult

import android.location.LocationManager

import android.R.attr.name
import android.app.AlertDialog
import android.content.*
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.R.attr.name
import android.graphics.drawable.Drawable
import android.os.Handler
import android.widget.AdapterView
import androidx.core.view.isVisible
import java.util.*
import kotlin.concurrent.timerTask


class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    private val bt = BT()

    private var indicatorRed: Drawable? = null
    private var indicatorGreen: Drawable? = null

    private var bundle: Bundle? = null
    lateinit var binding: ActivityMainBinding
    private fun bindingInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    private var pf: Boolean = false
    private var connFlag: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        bundle = savedInstanceState
        super.onCreate(savedInstanceState)
        bindingInit()
        setContentView(binding.root)


        // Get resources
        indicatorRed = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_r, null)
        indicatorGreen = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_g, null)


        binding.moreBtn.setOnClickListener {
            if (pf) {
                binding.btExtraArea.visibility = GONE
                binding.connProgressBar.visibility = GONE
            } else {
                binding.btExtraArea.visibility = VISIBLE
                binding.connProgressBar.visibility = VISIBLE
            }
            pf = !pf
        }

        val textView = binding.proLink
        val testString = "<a href='https://github.com/Hotakus/HGMA'>项目地址</a>"
        textView.movementMethod = LinkMovementMethod.getInstance()
        val htmlString = Html.fromHtml(testString, Html.FROM_HTML_MODE_LEGACY)
        textView.text = htmlString


        // TODO:
        val data = mutableListOf("CPU", "GPU", "Memory", "HardDisk", "HardDisk", "HardDisk")
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, data)
        binding.spinner1.adapter = adapter2
        binding.spinner2.adapter = adapter2
        binding.spinner3.adapter = adapter2
        binding.spinner4.adapter = adapter2


        binding.btList.onItemClickListener =
            AdapterView.OnItemClickListener() { parent, _, position, _ ->
                binding.btName.text = parent.getItemAtPosition(position).toString()


            }


        binding.btList.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.sv.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {

                }
                MotionEvent.ACTION_CANCEL -> {
                    binding.sv.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }


        /* BT and location begin */
        val msg = "因 Android 10 启动蓝牙需要同时启动“定位”才能正常使用，" +
                "所以请进设置给本应用开启定位后手动授权定位权限。以便正常使用蓝牙功能。"
        val ad = AlertDialog.Builder(this)
            .setMessage(msg)
            .setTitle("蓝牙与定位权限")
            .setPositiveButton("了解") { _, _ ->
                val ll = LocationLogic()
                ll.locationInit(this, bundle, locationManager)
            }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            ad.create().show()
        }

        bt.btInit(this, bundle)
        registerReceiver(BT.receiver, BT.filter)

        binding.btConnBtn.setOnClickListener {
            val name = binding.btName.text.toString()

            if (BT.btConnFlag) {
                bt.btDisConnect()
            } else {
                if (name.compareTo(getString(R.string.unknown_bluetooth)) != 0) {
                    bt.btConnect(name)
                }
            }
        }

        indicatorRed?.setBounds(0, 0, indicatorRed!!.minimumWidth, indicatorRed!!.minimumHeight)
        indicatorGreen?.setBounds(
            0,
            0,
            indicatorGreen!!.minimumWidth,
            indicatorGreen!!.minimumHeight
        )
        binding.btName.setCompoundDrawables(indicatorRed, null, null, null)


        // btConnBtn 可见性 控制线程
        Thread {
            while (true) {
                binding.btConnBtn.isClickable = BT.btClickableFlag
                Thread.sleep(100)
            }
        }.start()

        // btConnBtn 连接状态 控制线程
        Thread {
            while (true) {
                if (BT.btConnFlag) {
                    binding.btConnBtn.text = getString(R.string.unconnect)
                    indicatorHandler.sendEmptyMessage(0)
                } else {
                    binding.btConnBtn.text = getString(R.string.connect)
                    indicatorHandler.sendEmptyMessage(1)
                }
                Thread.sleep(100)
            }
        }.start()

        // 蓝牙扫描
        binding.btScan.setOnClickListener {
            when {
                !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                    ad.create().show()
                }

                else -> {
                    if (!BT.btScanFlag) {
                        binding.btName.text = getString(R.string.unknown_bluetooth)
                        bt.btInit(this, bundle)
                        bt.btScan()
                    } else {
                        "正在扫描，请稍等...".showToast(this)
                    }
                }
            }
        }
        /* BT and location end */

    }

    private val indicatorHandler: Handler = Handler {

        when(it.what) {
            0 -> {
                binding.btName.setCompoundDrawables(indicatorGreen, null, null, null)
            }
            1 -> {
                binding.btName.setCompoundDrawables(indicatorRed, null, null, null)
            }
        }

        false
    }


    override fun onDestroy() {
        super.onDestroy()

        bt.btDisConnect()
        bt.btDisConnect()

        unregisterReceiver(BT.receiver)
    }

}


