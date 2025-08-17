// AppLogger.kt
package com.example.umazing_helper
import android.util.Log

object AppLogger {
    private const val TAG_PREFIX = "UmaHelper"
    private var isDebugEnabled = true
    
    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d("$TAG_PREFIX-$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX-$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX-$tag", message)
        }
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX-$tag", message)
    }
    
    fun w(tag: String, message: String) {
        Log.w("$TAG_PREFIX-$tag", message)
    }
}