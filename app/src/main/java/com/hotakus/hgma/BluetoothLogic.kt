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
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

private val TAG = "BluetoothLogic"

private lateinit var activity: Activity
private var bundle: Bundle? = null

class BT : AppCompatActivity() {

    companion object {
        var btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bi = mutableListOf<btInfo>()
        val btNameList = mutableListOf<String>()
        var btScanFlag: Boolean = false
        var btConnFlag: Boolean = false
        var btClickableFlag: Boolean = true

        var btNameConnected: String = ""

        private var btDevice: BluetoothDevice? = null
        private var btSocket: BluetoothSocket? = null

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

                        bi.add(btInfo(deviceName, false, uuid, deviceHardwareAddress, rssi, device))
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
        private var connectedThread: ConnectedThread? = null


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
                5 -> {
                    "发送失败".showToast(activity)
                }
                6 -> {
                    "HGM响应失败（无返回或返回格式错误）".showToast(activity)
                }
                7 -> {
                    "Json解析错误".showToast(activity)
                }
                8 -> {
                    "HGM响应成功".showToast(activity)
                }
            }
            false
        }


        // Defines several constants used when transmitting messages between the
        // service and the UI.
        const val MESSAGE_READ: Int = 0
        const val MESSAGE_WRITE: Int = 1
        const val MESSAGE_TOAST: Int = 2

        private val btCommunityHandler = Handler {
            when (it.what) {
                MESSAGE_READ -> {
                    Log.i(TAG, "Bluetooth read [${it.arg1} bytes]")
                }
                MESSAGE_WRITE -> {

                }
                MESSAGE_TOAST -> {
                    it.data["toast"].toString().showToast(activity)
                }
                else -> {
                    "错误方式".showToast(activity)
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

                    // TODO: check bt

                } else {
                    connectThread?.cancel()
                    connectThread = null
                    btCommunityManager = null
                    btConnFlag = false

                    btClickableFlag = true

                    toastHandler.sendEmptyMessage(2)
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
        for (i in bi) {
            if (i.btName?.compareTo(bn) == 0) {
                btDevice = i.btDevice
                btSocket = btDevice?.createRfcommSocketToServiceRecord(i.uuid)

                connectThread = ConnectThread()
                connectThread?.start()

                connectedThread = ConnectedThread()
                connectedThread?.start()

                btNameConnected = bn
            }
        }
    }

    fun btDisConnect() {
        if (btAdapter.isDiscovering)
            btAdapter.cancelDiscovery()
        connectThread?.cancel()

        connectThread = null
        btCommunityManager = null
        btConnFlag = false

        btNameConnected = ""
    }

    inner class ConnectThread : Thread() {

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (btAdapter.isDiscovering)
                btAdapter.cancelDiscovery()

            if (btConnFlag) {
                connectThread?.cancel()
                connectThread = null
                btCommunityManager = null
                btConnFlag = false
            }

            try {
                btSocket?.connect()
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

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                btSocket?.close()
            } catch (e: IOException) {
                toastHandler.sendEmptyMessage(3)
            }
        }
    }


    val btReceiveHandler = Handler {

        BtMsgFramework.instance.updateMsg(
            btNameConnected,
            connectedThread?.getRecvBuffer(),
            BtMsgFramework.MSG_TYPE_RECEIVED
        )

        false
    }


    inner class ConnectedThread() : Thread() {

        private var mmInStream: InputStream = btSocket!!.inputStream
        private var mmOutStream: OutputStream = btSocket!!.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        private var receiveString = ""

        private var closeFlag = false

        fun getRecvBuffer(): String {
            return receiveString
        }

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                if (closeFlag)
                    continue
                if (!btConnFlag)
                    continue

                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    continue
                }

                receiveString = ""
                for (i in 0 until numBytes) {
                    val tmp = mmBuffer[i]
                    if (tmp in 1..127)
                        receiveString += tmp.toInt().toChar()
                }
                Log.i(TAG, "蓝牙接收[$numBytes]: $receiveString")
                btReceiveHandler.sendEmptyMessage(0)

                // Send the obtained bytes to the UI activity.
                val readMsg = btCommunityHandler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    receiveString
                )
                readMsg.sendToTarget()

                sleep(10)
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {

            if (!btConnFlag)
                return

            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = btCommunityHandler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                btCommunityHandler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = btCommunityHandler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer
            )
            writtenMsg.sendToTarget()
        }

        fun cancle() {
            closeFlag = true
        }
    }


    inner class CloseBtScan : TimerTask() {
        override fun run() {
            btAdapter.cancelDiscovery()
            btScanFlag = false
            toastHandler.sendEmptyMessage(0)
        }
    }


    fun sendData(data: String) {
        connectedThread?.write(data.toByteArray())
    }

    fun sendHgmData(data: String, timeout: Long): Boolean {
        connectedThread?.write(data.toByteArray())
        Thread.sleep(timeout)

        val str = connectedThread?.getRecvBuffer()

        try {
            val jo = JSONObject(str!!)
            val header = jo["Header"].toString()
            val d = jo["Data"].toString()

            if (header.compareTo("Hgm") != 0 || d.compareTo("ok") != 0) {
                toastHandler.sendEmptyMessage(6)
                return false
            }

            toastHandler.sendEmptyMessage(8)

        } catch (e: JSONException) {
            Log.e(TAG, "", e)
            toastHandler.sendEmptyMessage(7)
            return false
        }

        return true
    }

}

