package com.hotakus.hgma

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.hotakus.hgma.databinding.ActivityMainBinding
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    private val bt = BT()
    private val btMsgFramework = BtMsgFramework.instance
    private val ll = LocationLogic()

    private var indicatorRed: Drawable? = null
    private var indicatorGreen: Drawable? = null

    private var bundle: Bundle? = null
    lateinit var binding: ActivityMainBinding
    private fun bindingInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    private var pf: Boolean = false

    private fun touchListener(motionEvent: MotionEvent): Boolean {
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
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        bundle = savedInstanceState
        super.onCreate(savedInstanceState)
        bindingInit()
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        ll.locationInit(this, this, bundle, locationManager)
        checkLocationPermission()
        ll.locationRegister()

        btMsgFramework.btMsgInit(this)

        binding.wifiOpenBtn.setOnClickListener {
            val dt = 4
            val ssid = binding.ssidEditline.text
            val passwd = binding.passwdEditline.text

            if (ssid.isEmpty()) {
                "SSID不可为空".showToast(this)
                return@setOnClickListener
            }
            if (passwd.isEmpty()) {
                "密码不可为空".showToast(this)
                return@setOnClickListener
            }

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            val wifiConfJson =
                "{ \"Header\": \"${BT.hgmHeader}\", \"DataType\": \"$dt\", \"Data\": { \"ssid\": \"$ssid\", \"password\": \"$passwd\" } }"
            Log.i(TAG, wifiConfJson)

            Thread {
                bt.sendHgmData(wifiConfJson, 200)
            }.start()
        }

        binding.wifiCloseBtn.setOnClickListener {

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            val dt = 5
            val wifiConfJson =
                "{ \"Header\": \"${BT.hgmHeader}\", \"DataType\": \"$dt\", \"Data\": \"\"}"
            Log.i(TAG, wifiConfJson)

            Thread {
                bt.sendHgmData(wifiConfJson, 200)
            }.start()
        }

        // Get resources
        indicatorRed = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_r, null)
        indicatorGreen = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_g, null)


        binding.moreBtn.setOnClickListener {
            val expandTxt = getString(R.string.expand)
            val collapseTxt = getString(R.string.collapse)
            if (pf) {
                binding.btExtraArea.visibility = GONE
                binding.moreBtn.text = expandTxt
            } else {
                binding.btExtraArea.visibility = VISIBLE
                binding.moreBtn.text = collapseTxt
            }
            pf = !pf
        }

        val textView = binding.proLink
        val testString = "<a href='https://github.com/Hotakus/HGMA'>项目地址</a>"
        textView.movementMethod = LinkMovementMethod.getInstance()
        val htmlString = Html.fromHtml(testString, Html.FROM_HTML_MODE_LEGACY)
        textView.text = htmlString

        val data = mutableListOf("CPU", "GPU", "Memory", "HardDisk", "Network", "Fans")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, data)
        binding.spinner1.adapter = adapter
        binding.spinner2.adapter = adapter
        binding.spinner3.adapter = adapter
        binding.spinner4.adapter = adapter

        binding.btList.onItemClickListener =
            AdapterView.OnItemClickListener() { parent, _, position, _ ->
                binding.btName.text = parent.getItemAtPosition(position).toString()


            }


        binding.btList.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            touchListener(motionEvent)
        }
        binding.btMsgFrame.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            touchListener(motionEvent)
        }


        /* BT and location begin */
        bt.btInit(this, bundle)
        registerReceiver(BT.receiver, BT.filter)

        Thread {
            while (true) {
                if (BT.btConnDone) {
                    pbvHandler.sendEmptyMessage(1)
                } else {
                    pbvHandler.sendEmptyMessage(0)
                }
                Thread.sleep(500)
            }
        }.start()

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
            checkLocationPermission()
            when {
                !locationManager.isProviderEnabled(LocationLogic.provider) -> {
                    checkLocationPermission()
                }
                else -> {
                    if (!BT.btScanFlag) {
                        binding.btName.text = getString(R.string.unknown_bluetooth)
                        bt.btInit(this, bundle)
                        bt.btDisConnect()
                        bt.btScan()
                    } else {
                        "正在扫描，请稍等...".showToast(this)
                    }
                }
            }
        }

        /* BT and location end */
        binding.biliBtn.setOnClickListener {
            val dt = 7
            val uid = binding.biliUidEditline.text
            if (uid.isEmpty()) {
                "UID不能为空".showToast(this)
                return@setOnClickListener
            }

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            val str =
                "{\"Header\": \"${BT.hgmHeader}\", \"DataType\": \"$dt\", \"Data\": { \"uid\": \"$uid\"}}"

            Thread {
                bt.sendHgmData(str, 200)
            }.start()
        }

        binding.hardwareConfBtn.setOnClickListener {
            val dt = 8

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }
        }

        val txt = getString(R.string.null_text)
        binding.locateBtn.setOnClickListener {
            val rst = checkLocationPermission()

            if (rst) {
                ll.getLastLocation()
                binding.longitudeValue.text = ll.longitude
                binding.latitudeValue.text = ll.latitude
            } else {
                binding.longitudeValue.text = txt
                binding.latitudeValue.text = txt
            }
        }

        binding.weatherBtn.setOnClickListener {
            val dt = 6
            val lat = binding.latitudeValue.text.toString()
            val lon = binding.longitudeValue.text.toString()
            val key = binding.appKeyEditline.text.toString()

            if (lat.compareTo(txt) == 0 || lon.compareTo(txt) == 0) {
                "未定位，无法配置".showToast(this)
                return@setOnClickListener
            }

            if (key.isEmpty()) {
                "和风天气App key未填写".showToast(this)
                return@setOnClickListener
            }

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            val str = "{\"Header\": \"${BT.hgmHeader}\", " +
                    "\"DataType\": \"$dt\", " +
                    "\"Data\": {\"lat\":\"$lat\", \"lon\":\"$lon\", \"key\":\"$key\"}}"

            bt.sendHgmData(str, 200);

            // TODO:
        }

        binding.btMsgSendBtn.setOnClickListener {
            val dt = 3
            val isHgmMode = binding.HgmModeCheckBtn.isChecked
            var msg = binding.btMsgSender.text.toString()
            var sender = getString(R.string.sender)

            if (msg.isEmpty())
                return@setOnClickListener

            if (!BT.btConnFlag)
                sender = "${0x2757.toChar()}" + sender

            if (isHgmMode)
                msg = "{\"Header\": \"${BT.hgmHeader}\", \"DataType\": \"$dt\", \"Data\":\"$msg\"}"

            bt.sendData(msg)
            btMsgFramework.updateMsg(sender, msg, BtMsgFramework.MSG_TYPE_SEND)
        }

        binding.btCleanBtn.setOnClickListener {
            btMsgFramework.btMsgClear()
        }

        // onCreate end
    }


    private fun checkLocationPermission(): Boolean {
        val isProviderEnabled =
            LocationLogic.locationManager?.isProviderEnabled(LocationLogic.provider)
        if (!isProviderEnabled!!) {
            val msg = "因 Android 10 启动蓝牙需要同时启动“定位”才能正常使用，" +
                    "所以请进设置给本应用开启定位后手动授权定位权限。以便正常使用蓝牙功能。"
            val ad = AlertDialog.Builder(this)
                .setMessage(msg)
                .setTitle("蓝牙与定位权限")
                .setPositiveButton("了解") { _, _ ->
                    startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1)
                }
            ad.create().show()

            return false
        }

        if (this.checkSelfPermission(
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && this.checkSelfPermission(
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION
                ), 2
            )

            return false
        }

        return true
    }

    private val indicatorHandler: Handler = Handler {
        when (it.what) {
            0 -> {
                binding.btName.setCompoundDrawables(indicatorGreen, null, null, null)
            }
            1 -> {
                binding.btName.setCompoundDrawables(indicatorRed, null, null, null)
            }
        }
        false
    }

    private val pbvHandler = Handler {
        when (it.what) {
            0 -> {
                binding.connProgressBar.visibility = VISIBLE
            }
            1 -> {
                binding.connProgressBar.visibility = GONE
            }
        }
        false
    }


    override fun onDestroy() {
        super.onDestroy()

        ll.locationUnregister()
        bt.btDisConnect()
        unregisterReceiver(BT.receiver)


    }


}











