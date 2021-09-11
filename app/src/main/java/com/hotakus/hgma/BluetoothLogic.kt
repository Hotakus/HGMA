package com.hotakus.hgma

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.IOException
import java.util.*

private val TAG = "BluetoothLogic"

private lateinit var activity: Activity
private var bundle: Bundle? = null

class BT : AppCompatActivity() {

    private lateinit var mmSocket: BluetoothSocket

    companion object {
        var btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bi = mutableListOf<btInfo>()
        val btNameList = mutableListOf<String>()
        var btScanFlag: Boolean = false
        var btConnFlag: Boolean = false
        var btClickableFlag: Boolean = true

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

        // Create a BroadcastReceiver for ACTION_FOUND.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // Discovery has found a device. Get the BluetoothDevice
                        // object and its info from the Intent.
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.extras!!.getShort(BluetoothDevice.EXTRA_RSSI)
                        val uuid: UUID? = device?.uuids?.get(0)?.uuid
                        val deviceName = device?.name
                        val deviceHardwareAddress = device?.address // MAC address

                        bi.add(btInfo(deviceName, uuid, deviceHardwareAddress, rssi, device))
                        Log.i(TAG, bi[bi.size - 1].toString())

                        if (deviceName != null) {
                            btNameList.add(deviceName)
                            val adapter = ArrayAdapter(activity, R.layout.my_list, btNameList)
                            val btl = activity.findViewById<ListView>(R.id.btList)
                            btl.adapter = adapter
                        }
                    }
                }
            }
        }

        private var connectThread: ConnectThread? = null
        private var btSocket: BluetoothSocket? = null

        private val toastHandler: Handler = Handler {
            when (it.what) {
                0 -> {
                    "蓝牙扫描结束".showToast(activity)
                }
                1 -> {
                    "蓝牙连接成功".showToast(activity)
                }
                2 -> {
                    "蓝牙无法连接客户端".showToast(activity)
                }
                3 -> {
                    "蓝牙无法关闭连接".showToast(activity)
                }
                4 -> {
                    "蓝牙关闭".showToast(activity)
                }
            }
            false
        }

        class BtCommunityManager : Thread() {
            override fun run() {
                var timeout = 8000
                while (!btSocket?.isConnected!!) {
                    sleep(100)
                    if (timeout > 0)
                        timeout -= 100
                    else
                        break
                }
                if (timeout > 0) {
                    toastHandler.sendEmptyMessage(1)
                    btConnFlag = true
                    btClickableFlag = true
                } else {
                    toastHandler.sendEmptyMessage(2)

                    connectThread?.cancel()
                    connectThread = null
                    btCommunityManager = null
                    btConnFlag = false

                    btClickableFlag = true
                }

            }
        }
        private var btCommunityManager: BtCommunityManager? = null
    }

    fun btInit(a: Activity, b: Bundle?): Boolean {

        activity = a
        bundle = b

        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, 1, b)
        }

        return true
    }

    fun btScan(): Boolean {
        if (btScanFlag) {
            return false
        }

        bi.clear()
        btNameList.clear()

        when (btAdapter.startDiscovery()) {
            false -> {
                val str = "蓝牙扫描开启失败"
                str.showToast(activity)
                Log.e("BluetoothLogic", str)
                return false
            }
            true -> {
                btScanFlag = true
                // 10min close bt scan
                Timer().schedule(CloseBtScan(), 10000)
            }
        }

        return true
    }

    fun btConnect(bn: String) {

        btClickableFlag = false

        var btDevice: BluetoothDevice?

        for (i in bi) {
            if (i.btName?.compareTo(bn) == 0) {
                btDevice = i.btDevice
                // i.toString().showToast(activity)
                ConnectThread(i).start()
            }

        }
    }

    fun btDisConnect() {
        connectThread?.cancel()
        connectThread = null
        btCommunityManager = null
        btConnFlag = false

        toastHandler.sendEmptyMessage(4)
    }


    inner class ConnectThread(_bi: btInfo) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            _bi.btDevice?.createRfcommSocketToServiceRecord(_bi.uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.

            if(btAdapter.isDiscovering)
                btAdapter.cancelDiscovery()

            if (btConnFlag) {
                connectThread?.cancel()
                connectThread = null
                btCommunityManager = null
                btConnFlag = false
            }

            mmSocket?.use { socket ->
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    socket.connect()
                    btSocket = socket

                    if (btCommunityManager == null) {
                        btCommunityManager = BtCommunityManager()
                        btCommunityManager?.start()
                    }

                } catch (e: IOException) {
                    Log.e(TAG, "Could not connect the client socket", e)
                    toastHandler.sendEmptyMessage(2)
                    cancel()
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                toastHandler.sendEmptyMessage(3)
            }
        }
    }


    class CloseBtScan : TimerTask() {
        override fun run() {
            btAdapter.cancelDiscovery()
            btScanFlag = false
            toastHandler.sendEmptyMessage(0)
        }
    }

}
