package com.hotakus.hgma

import android.bluetooth.BluetoothDevice
import java.util.*

data class btInfo(
    val btName: String?,
    val connected: Boolean,
    val uuid: UUID?,
    val btMacAddress: String?,
    val btRSSI: Short?,
    val btDevice: BluetoothDevice?
)
