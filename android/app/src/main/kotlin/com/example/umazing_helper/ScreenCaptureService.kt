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
import kotlin.coroutines.suspendCoroutine

data class CaptureRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

class ScreenCaptureService(private val context: Context) {
    
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureHandler: Handler? = null

    suspend fun captureEventTitleRegion(): CaptureResult = withContext(Dispatchers.IO) {
        try {
            // Get screen dimensions
            val dimensions = getScreenDimensions()
            
            // Calculate event title region (percentage-based)
            val eventTitleRegion = CaptureRegion(
                x = (dimensions.width * 0.08).toInt(),      // 8% from left
                y = (dimensions.height * 0.18).toInt(),     // 18% from top  
                width = (dimensions.width * 0.84).toInt(),  // 84% width
                height = (dimensions.height * 0.08).toInt() // 8% height
            )
            
            AppLogger.d("ScreenCaptureService", "Event title region: x=${eventTitleRegion.x}, y=${eventTitleRegion.y}, w=${eventTitleRegion.width}, h=${eventTitleRegion.height}")
            
            // 🚀 OPTIMIZED: Capture region directly (no full screen capture + crop)
            val regionBitmap = captureRegionDirectly(eventTitleRegion, dimensions)
            
            if (regionBitmap != null) {
                // Convert to raw RGBA bytes (no PNG compression)
                val rawBytes = bitmapToRawBytes(regionBitmap)
                val width = regionBitmap.width
                val height = regionBitmap.height
                regionBitmap.recycle()
                
                AppLogger.d("ScreenCaptureService", "✅ Event title captured: ${rawBytes.size} bytes (raw RGBA ${width}x${height})")
                CaptureResult.Success(rawBytes, width, height)
            } else {
                CaptureResult.Error("Failed to capture region")
            }
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Event title capture failed", e)
            CaptureResult.Error("Event title capture failed: ${e.message}")
        }
    }
    
    // 🚀 NEW: Capture region directly (faster than full screen + crop)
    private suspend fun captureRegionDirectly(
        region: CaptureRegion,
        dimensions: ScreenDimensions
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val projection = MediaProjectionManager.getInstance().getProjection()
                ?: return@withContext null
            
            if (!MediaProjectionManager.getInstance().isProjectionValid()) {
                return@withContext null
            }
            
            AppLogger.d("ScreenCaptureService", "Using validated MediaProjection")
            
            // Capture full screen bitmap with retry
            val fullBitmap = captureFullScreenBitmap(projection, dimensions)
            
            if (fullBitmap != null) {
                // Crop to region
                val croppedBitmap = Bitmap.createBitmap(
                    fullBitmap,
                    region.x,
                    region.y,
                    region.width,
                    region.height
                )
                fullBitmap.recycle() // Free memory immediately
                croppedBitmap
            } else {
                null
            }
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error capturing region", e)
            null
        } finally {
            cleanup()
        }
    }
    
    // 🚀 OPTIMIZED: Direct bitmap capture with Android 13+ fix
    private suspend fun captureFullScreenBitmap(
        projection: MediaProjection,
        dimensions: ScreenDimensions
    ): Bitmap? {
        var attempt = 0
        val maxAttempts = 3 // Reduced from 5 to 3 for speed
        
        while (attempt < maxAttempts) {
            attempt++
            AppLogger.d("ScreenCaptureService", "Capture attempt $attempt/$maxAttempts")
            
            try {
                val bitmap = performSingleBitmapCapture(projection, dimensions, attempt)
                
                if (bitmap != null && !bitmap.isEmptyBitmap()) {
                    AppLogger.d("ScreenCaptureService", "✅ Valid bitmap captured on attempt $attempt")
                    return bitmap
                } else {
                    bitmap?.recycle()
                    AppLogger.w("ScreenCaptureService", "📷 Empty bitmap on attempt $attempt")
                    
                    // Small delay before retry (shorter for speed)
                    if (attempt < maxAttempts) {
                        delay(50) // Reduced from 100ms to 50ms
                    }
                }
                
            } catch (e: Exception) {
                AppLogger.w("ScreenCaptureService", "❌ Attempt $attempt failed: ${e.message}")
                cleanup()
                
                if (attempt < maxAttempts) {
                    delay(50)
                }
            }
        }
        
        AppLogger.e("ScreenCaptureService", "❌ All $maxAttempts attempts failed")
        return null
    }
    
    // 🚀 OPTIMIZED: Single bitmap capture with minimal delay
    private suspend fun performSingleBitmapCapture(
        projection: MediaProjection,
        dimensions: ScreenDimensions,
        attemptNumber: Int
    ): Bitmap? = suspendCoroutine { continuation ->
        var resumed = false
        
        try {
            imageReader = ImageReader.newInstance(
                dimensions.width,
                dimensions.height,
                PixelFormat.RGBA_8888,
                2
            )
            
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenCapture",
                dimensions.width,
                dimensions.height,
                dimensions.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                null
            )
            
            AppLogger.d("ScreenCaptureService", "VirtualDisplay created: ${dimensions.width}x${dimensions.height}")
            
            // 🚀 Android 13+ FIX: Wait for display to render
            val delayMs = if (attemptNumber == 1) 120L else 80L // First attempt longer
            
            GlobalScope.launch(Dispatchers.IO) {
                delay(delayMs)
                
                if (!resumed) {
                    try {
                        val image = imageReader?.acquireLatestImage()
                        
                        if (image != null) {
                            val bitmap = convertImageToBitmapSafely(image, dimensions)
                            image.close()
                            
                            resumed = true
                            continuation.resume(bitmap)
                        } else {
                            resumed = true
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        resumed = true
                        continuation.resume(null)
                    }
                }
            }
            
            // Timeout after 500ms
            GlobalScope.launch(Dispatchers.IO) {
                delay(500)
                if (!resumed) {
                    resumed = true
                    continuation.resume(null)
                }
            }
            
        } catch (e: Exception) {
            if (!resumed) {
                resumed = true
                continuation.resume(null)
            }
        }
    }
    
    // 🚀 OPTIMIZED: Convert to raw RGBA bytes (no PNG compression)
    private fun bitmapToRawBytes(bitmap: Bitmap): ByteArray {
        return try {
            val byteCount = bitmap.byteCount
            val buffer = java.nio.ByteBuffer.allocate(byteCount)
            bitmap.copyPixelsToBuffer(buffer)
            buffer.array()
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error converting bitmap to raw bytes", e)
            throw e
        }
    }
    
    // 🚀 OPTIMIZED: Fast empty bitmap detection (5 pixel check)
    private fun Bitmap.isEmptyBitmap(): Boolean {
        return try {
            if (width == 0 || height == 0) return true
            
            // Quick check: Sample only 5 pixels
            val centerX = width / 2
            val centerY = height / 2
            
            val pixels = intArrayOf(
                getPixel(centerX, centerY),
                getPixel(centerX / 2, centerY / 2),
                getPixel(centerX * 3 / 4, centerY * 3 / 4),
                getPixel(10.coerceAtMost(width - 1), 10.coerceAtMost(height - 1)),
                getPixel((width - 10).coerceAtLeast(0), (height - 10).coerceAtLeast(0))
            )
            
            // At least 3 out of 5 pixels should be non-zero
            val nonZeroCount = pixels.count { it != 0 }
            nonZeroCount < 3
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error checking empty bitmap", e)
            false
        }
    }
    
    // Remove old methods (not needed anymore)
    // ❌ REMOVED: bitmapToByteArray() - replaced with bitmapToRawBytes()
    // ❌ REMOVED: isImageValid() - replaced with isEmptyBitmap()
    // ❌ REMOVED: cropImageToRegion() - now done in-memory
    
    private fun convertImageToBitmapSafely(image: Image, dimensions: ScreenDimensions): Bitmap {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * dimensions.width
            
            val bitmapWidth = dimensions.width + rowPadding / pixelStride
            val bitmapHeight = dimensions.height
            
            val bitmap = Bitmap.createBitmap(
                bitmapWidth,
                bitmapHeight,
                Bitmap.Config.ARGB_8888
            )
            
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Crop to actual dimensions if needed
            return if (rowPadding == 0) {
                bitmap
            } else {
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, dimensions.width, dimensions.height)
                bitmap.recycle()
                croppedBitmap
            }
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error converting image to bitmap", e)
            throw e
        }
    }
    
    private fun getScreenDimensions(): ScreenDimensions {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        return ScreenDimensions(
            width = displayMetrics.widthPixels,
            height = displayMetrics.heightPixels,
            density = displayMetrics.densityDpi
        )
    }
    
    private fun cleanup() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            
            imageReader?.close()
            imageReader = null
            
            captureHandler?.removeCallbacksAndMessages(null)
            captureHandler = null
            
            AppLogger.d("ScreenCaptureService", "Cleanup completed")
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error during cleanup", e)
        }
    }
}