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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hotakus.hgma.databinding.ActivityMainBinding
import java.util.*
import kotlin.collections.ArrayList

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
    private var connFlag: Boolean = false


    fun touchListener(view: View, motionEvent: MotionEvent): Boolean {
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
            val dt = "0"
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
                "{ \"Header\": \"Hgm\", \"DataType\": \"$dt\", \"Data\": { \"ssid\": \"$ssid\", \"password\": \"$passwd\" } }"
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

            val dt = "1"
            val wifiConfJson = "{ \"Header\": \"Hgm\", \"DataType\": \"$dt\", \"Data\": \"\"}"
            Log.i(TAG, wifiConfJson)

            Thread {
                bt.sendHgmData(wifiConfJson, 100)
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


        binding.btList.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            touchListener(view, motionEvent)
        }
        binding.btMsgFrame.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            touchListener(view, motionEvent)
        }


        /* BT and location begin */
        bt.btInit(this, bundle)
        registerReceiver(BT.receiver, BT.filter)

        var pbvFlag = false
        val pbv = Thread {
            while (true) {
                if (pbvFlag && !BT.btConnFlag) {
                    pbvHandler.sendEmptyMessage(0)
                    pbvFlag = false
                } else if (BT.btConnFlag) {
                    pbvHandler.sendEmptyMessage(1)
                }
                Thread.sleep(100)
            }
        }
        pbv.start()

        binding.btConnBtn.setOnClickListener {
            val name = binding.btName.text.toString()

            if (BT.btConnFlag) {
                bt.btDisConnect()
            } else {
                if (name.compareTo(getString(R.string.unknown_bluetooth)) != 0) {
                    bt.btConnect(name)
                    pbvFlag = true
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


        // btConnBtn 可点击性 控制线程
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
            val uid = binding.biliUidEditline.text
            if (uid.isEmpty()) {
                "UID不能为空".showToast(this)
                return@setOnClickListener
            }

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            val str = "{\"Header\": \"Hgm\", \"DataType\": \"3\", \"Data\": { \"uid\": \"$uid\"}}"

            Thread {
                bt.sendHgmData(str, 100)
            }.start()
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
            val lat = binding.latitudeValue.text.toString()
            val lon = binding.longitudeValue.text.toString()

            if (lat.compareTo(txt) == 0 || lon.compareTo(txt) == 0) {
                "未定位，无法配置".showToast(this)
                return@setOnClickListener
            }

            if (binding.appKeyEditline.text.isEmpty()) {
                "和风天气App key未填写".showToast(this)
                return@setOnClickListener
            }

            if (!BT.btConnFlag) {
                "蓝牙未连接".showToast(this)
                return@setOnClickListener
            }

            // TODO:
        }

        binding.btMsgSendBtn.setOnClickListener {
            val isHgmMode = binding.HgmModeCheckBtn.isChecked
            var msg = binding.btMsgSender.text.toString()
            var sender = getString(R.string.sender)

            if (msg.isEmpty())
                return@setOnClickListener

            if (!BT.btConnFlag)
                sender = "${0x2757.toChar()}" + sender

            if (isHgmMode)
                msg = "{\"Header\": \"Hgm\", \"DataType\": \"6\", \"Data\":\"$msg\"}"

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











