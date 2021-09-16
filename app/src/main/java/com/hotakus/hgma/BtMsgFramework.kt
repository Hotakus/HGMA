package com.hotakus.hgma

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.IllegalArgumentException

class BtMsgFramework : AppCompatActivity() {

    private var rv: RecyclerView? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var activity: Activity? = null

        @SuppressLint("StaticFieldLeak")
        val instance: BtMsgFramework = BtMsgFramework()

        val btMsgList = ArrayList<BtMsg>()
        var btMsgAdapter = BtMsgAdapter(btMsgList)

        var MSG_MAX_COUNT: Int = 100

        const val MSG_TYPE_SEND: Int = 0
        const val MSG_TYPE_RECEIVED: Int = 1
    }

    fun btMsgInit(_activity: Activity?) {
        activity = _activity

        rv = activity?.findViewById(R.id.btMsgFrame)
        rv?.layoutManager = LinearLayoutManager(activity)
        rv?.adapter = btMsgAdapter
    }

    private fun updateAdapter() {
        rv?.adapter?.notifyItemInserted(btMsgList.size - 1)
    }

    fun updateMsg(user: String, msg: String?, msgType: Int) {
        // Remove the stale msg from msg list
        if (btMsgList.size == MSG_MAX_COUNT) {
            btMsgList.removeAt(0)
            rv?.adapter?.notifyItemRemoved(0)
        }

        if (msg == null)
            return

        btMsgList.add(BtMsg("$user ${DateUtil.nowDateTime}", msg, msgType))
        updateAdapter()

        // Scroll to bottom
        rv?.scrollToPosition(btMsgList.size - 1)
    }

    fun btMsgClear() {
        btMsgList.clear()
        btMsgAdapter = BtMsgAdapter(btMsgList)
        rv?.adapter = btMsgAdapter
    }


    data class BtMsg(val info: String, val msg: String, val msgType: Int)

    class BtMsgAdapter(_list: ArrayList<BtMsg>) :
        RecyclerView.Adapter<BtMsgAdapter.ViewHolder>() {

        private val list = _list

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val si: TextView = view.findViewById(R.id.btMsgSenderInfo)
            val sm: TextView = view.findViewById(R.id.btMsgText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                MSG_TYPE_SEND -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.rv_bt_msg_right, parent, false)
                    ViewHolder(view)
                }
                MSG_TYPE_RECEIVED -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.rv_bt_msg_left, parent, false)
                    ViewHolder(view)
                }
                else -> throw IllegalArgumentException()
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.si.text = list[position].info
            holder.sm.text = list[position].msg
        }

        override fun getItemViewType(position: Int): Int {
            return list[position].msgType
        }

        override fun getItemCount(): Int = list.size
    }


}