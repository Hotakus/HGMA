package com.hotakus.hgma

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat


class LocationLogic : AppCompatActivity() {

    private var registerFlag = false

    private val TAG = "LocationLogic"
    var location : Location? = null
    var latitude : String? = null
    var longitude : String? = null

    companion object Co : AppCompatActivity() {
        var locationManager: LocationManager? = null

        @SuppressLint("StaticFieldLeak")
        private var activity: Activity? = null
        @SuppressLint("StaticFieldLeak")
        private  var context : Context? = null

        var provider : String = LocationManager.NETWORK_PROVIDER
    }

    fun locationInit(_activity: Activity?, _context : Context?, bundle: Bundle?, _locationManager : LocationManager) {
        activity = _activity
        context = _context
        locationManager = _locationManager

        // 自动选择provider
        val providers = locationManager!!.getProviders(true)
        provider = when {
            providers.contains(LocationManager.NETWORK_PROVIDER) -> {
                //如果是网络定位
                Log.i(TAG, "网络定位")
                LocationManager.NETWORK_PROVIDER
            }
            providers.contains(LocationManager.GPS_PROVIDER) -> {
                //如果是GPS定位
                Log.i(TAG, "GPS定位")
                LocationManager.GPS_PROVIDER
            }
            else -> {
                Log.i(TAG, "没有可用的位置提供器")
                LocationManager.NETWORK_PROVIDER
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation() {
        location = locationManager?.getLastKnownLocation(provider)

        val df = DecimalFormat("###.###")
        latitude = df.format(location?.latitude)
        longitude = df.format(location?.longitude)
    }

    @SuppressLint("MissingPermission")
    fun locationRegister() {
        if (!registerFlag) {
            locationManager?.requestLocationUpdates(provider, 1000, 0.1f, locationListener)
            registerFlag = true
        }
    }

    fun locationUnregister() {
        if (registerFlag) {
            locationManager?.removeUpdates(locationListener)
            registerFlag = false
        }

    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(_location: Location?) {
            Log.i(TAG, "位置变化")
            location = _location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.i(TAG, "定位状态变化")

        }

        override fun onProviderEnabled(provider: String?) {
            "定位开启".showToast(activity!!)
        }

        override fun onProviderDisabled(provider: String?) {
            "定位关闭".showToast(activity!!)
        }

    }

}