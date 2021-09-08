package com.hotakus.hgma

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import com.hotakus.hgma.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private fun bindingInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    private fun _svl(view : View, motionEvent : MotionEvent) : Boolean {
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

    private var pf : Boolean = false
    private var pf2 : Boolean = false
    private var cvbtWidthCollapseDp : Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingInit()
        setContentView(binding.root)

        val icon = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_r, null)
        icon?.setBounds(0, 0, icon.minimumWidth, icon.minimumHeight)
        binding.btName.setCompoundDrawables(icon, null, null, null)

        val data = mutableListOf("CPU", "GPU", "Memory", "HardDisk", "HardDisk", "HardDisk")
        val adapter = ArrayAdapter(this, R.layout.my_list, data)
        binding.btList.adapter = adapter



        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, data)
        binding.spinner1.adapter = adapter2
        binding.spinner2.adapter = adapter2
        binding.spinner3.adapter = adapter2
        binding.spinner4.adapter = adapter2



        binding.connBtn.setOnClickListener {
            if (pf) {
                binding.connProgressBar.visibility = INVISIBLE
            } else {
                binding.connProgressBar.visibility = VISIBLE
            }
            pf = !pf
        }


        val cvbtParams = binding.CardViewBT.layoutParams
        val cvbtWidthCollapseDp = px2dp(this, cvbtParams.height.toFloat())

        binding.moreBtn.setOnClickListener {

            val cvbtExpandDp = if (pf2) {
                cvbtWidthCollapseDp
            } else {
                cvbtWidthCollapseDp + 500
            }
            pf2 = !pf2
            cvbtParams.height = dp2px(this, cvbtExpandDp).toInt()
            binding.CardViewBT.layoutParams = cvbtParams
        }


        binding.btList.setOnTouchListener {  view : View, motionEvent : MotionEvent ->
            _svl(view, motionEvent)
        }


    }
}