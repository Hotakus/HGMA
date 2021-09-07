package com.hotakus.hgma

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
import com.hotakus.hgma.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private fun bindingInit() {
        binding = ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingInit()
        setContentView(binding.root)

        val icon = ResourcesCompat.getDrawable(resources, R.drawable.indicator_led_r, null)
        icon?.setBounds(0, 0, icon.minimumWidth, icon.minimumHeight)
        binding.btName.setCompoundDrawables(icon, null, null, null)

        val data = mutableListOf("HelloWorld", "HellGateMonitorBT","HelloWorld", "HellGateMonitorBT", "HelloWorld", "HellGateMonitorBT")
        binding.btList.adapter = ArrayAdapter(this, R.layout.my_list, data)


        val anim = ScaleAnimation(this, null)

    }
}