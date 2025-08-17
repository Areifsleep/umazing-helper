// ScreenCaptureService.kt
package com.example.umazing_helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ScreenCaptureService(private val context: Context) {
    
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    suspend fun captureScreen(): CaptureResult = withContext(Dispatchers.IO) {
        try {
            val projection = MediaProjectionManager.getInstance().getProjection()
                ?: return@withContext CaptureResult.Error("No media projection available")
            
            val dimensions = getScreenDimensions()
            val imageData = performCapture(projection, dimensions)
            
            CaptureResult.Success(imageData)
        } catch (e: Exception) {
            CaptureResult.Error("Screen capture failed: ${e.message}", e)
        } finally {
            cleanup()
        }
    }
    
    private suspend fun performCapture(
        projection: MediaProjection, 
        dimensions: ScreenDimensions
    ): ByteArray = suspendCancellableCoroutine { continuation ->
        
        try {
            imageReader = ImageReader.newInstance(
                dimensions.width,
                dimensions.height,
                PixelFormat.RGBA_8888,
                1
            )
            
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenCapture-${System.currentTimeMillis()}",
                dimensions.width,
                dimensions.height,
                dimensions.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                null
            )
            
            imageReader!!.setOnImageAvailableListener({
                try {
                    val image = imageReader!!.acquireLatestImage()
                    if (image != null) {
                        val bitmap = imageToBitmap(image)
                        val byteArray = bitmapToByteArray(bitmap)
                        image.close()
                        
                        if (continuation.isActive) {
                            continuation.resume(byteArray)
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception("Failed to acquire image"))
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }, Handler(Looper.getMainLooper()))
            
            // Set up cancellation
            continuation.invokeOnCancellation {
                cleanup()
            }
            
        } catch (e: Exception) {
            cleanup()
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    private fun getScreenDimensions(): ScreenDimensions {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        return ScreenDimensions(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            density = metrics.densityDpi
        )
    }
    
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    
    private fun cleanup() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            virtualDisplay = null
            imageReader = null
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error during cleanup", e)
        }
    }
}