package com.hotakus.hgma

import android.content.Context

/**
 * dp转换px
 */
fun dp2px(context: Context, dpValue: Float): Float {
    return context.resources.displayMetrics.density * dpValue
}

/**
 * px转换dp
 */
fun px2dp(context: Context, pxValue: Float): Float {
    return pxValue / context.resources.displayMetrics.density
}