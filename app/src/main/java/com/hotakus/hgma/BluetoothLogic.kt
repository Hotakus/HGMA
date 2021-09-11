package com.hotakus.hgma

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import java.util.*


private lateinit var activity: Activity
private var bundle: Bundle? = null

class BT : AppCompatActivity() {

    companion object {
        var btAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bi = mutableListOf<btInfo>()
        val btNameList = mutableListOf<String>()
        var btScanFlag: Boolean = false

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
                        val uuid : UUID?  = device?.uuids?.get(0)?.uuid
                        val deviceName = device?.name
                        val deviceHardwareAddress = device?.address // MAC address

                        bi.add(btInfo(deviceName, uuid, deviceHardwareAddress, rssi, device))
                        Log.i("MainActivity", bi[bi.size - 1].toString())

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


    fun btConnect(bn : String) {

        val btToConnName = bn   //"HellGateMonitorBT"
        var btDevice : BluetoothDevice?

        for (i in bi) {
            if (i.btName?.compareTo(btToConnName) == 0) {
                btDevice = i.btDevice
                "a".showToast(activity)
                //val bcs = btDevice?.createRfcommSocketToServiceRecord(i.uuid)
            }

        }
    }

}


private val alterHandle: Handler = Handler {
    "蓝牙扫描结束".showToast(activity)
    false
}

class CloseBtScan : TimerTask() {
    override fun run() {
        BT.btAdapter.cancelDiscovery()
        BT.btScanFlag = false
        alterHandle.sendEmptyMessage(0)
    }
}