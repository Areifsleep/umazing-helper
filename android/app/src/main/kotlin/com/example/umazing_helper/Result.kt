// Results.kt
package com.example.umazing_helper

sealed class CaptureResult {
    data class Success(
        val imageData: ByteArray,
        val width: Int,
        val height: Int
    ) : CaptureResult()
    data class Error(val message: String, val throwable: Throwable? = null) : CaptureResult()
}

data class ScreenDimensions(
    val width: Int,
    val height: Int,
    val density: Int
)