package com.hotakus.hgma

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

        val bg = binding.MainLinearLayout.background
        val cf = bg.colorFilter

    }
}