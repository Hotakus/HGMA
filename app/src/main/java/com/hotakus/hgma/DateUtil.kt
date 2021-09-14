package com.hotakus.hgma

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.util.*

object DateUtil {
    val nowDateTime: String
        @SuppressLint("SimpleDateFormat")
        get() {
            val sdf = SimpleDateFormat("MM/dd/HH:mm:ss")
            return sdf.format(Date())
        }

    @get:SuppressLint("SimpleDateFormat")
    val nowTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss")
            return sdf.format(Date())
        }

    val year : Year
        get() = Year.now()

    val month : YearMonth
        get() = YearMonth.now()

    val day : MonthDay
        get() = MonthDay.now()

}