package com.hotakus.hgma

import android.content.Context
import android.widget.Toast

fun String.showTaost(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun Int.showTaost(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun Float.showTaost(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this.toString(), duration).show()
}
