package com.hotakus.hgma

import android.bluetooth.BluetoothAdapter
import android.location.LocationManager
import android.app.Activity
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService


class LocationLogic() : AppCompatActivity() {

    companion object Co : AppCompatActivity() {
        private val ll = LocationLogic()
        var locationManager : LocationManager? = null
    }

    fun getInstance() : LocationLogic {
        return ll
    }

    fun locationInit(activity: Activity?, bundle: Bundle?, lm : LocationManager) {
        if (locationManager == null) {
            locationManager = lm
        }

        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled!!) {
            val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ActivityCompat.startActivityForResult(activity!!, enableLocationIntent, 1, bundle)
        }
    }

}