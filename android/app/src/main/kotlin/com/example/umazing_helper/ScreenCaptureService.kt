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
                y = (dimensions.height * 0.18).toInt(),     // 25% from top  
                width = (dimensions.width * 0.84).toInt(),  // 84% width
                height = (dimensions.height * 0.08).toInt() // 8% height
            )
            
            AppLogger.d("ScreenCaptureService", "Event title region: x=${eventTitleRegion.x}, y=${eventTitleRegion.y}, w=${eventTitleRegion.width}, h=${eventTitleRegion.height}")
            
            // Capture full screen first
            val fullScreenResult = captureScreen()
            
            if (fullScreenResult is CaptureResult.Success) {
                // Crop to event title region
                val croppedImageData = cropImageToRegion(fullScreenResult.imageData, eventTitleRegion)
                AppLogger.d("ScreenCaptureService", "Event title capture successful: ${croppedImageData.size} bytes")
                CaptureResult.Success(croppedImageData)
            } else {
                fullScreenResult
            }
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Event title capture failed", e)
            CaptureResult.Error("Event title capture failed: ${e.message}")
        }
    }

    private fun cropImageToRegion(imageData: ByteArray, region: CaptureRegion): ByteArray {
        return try {
            // Convert bytes to bitmap
            val fullBitmap = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: throw Exception("Failed to decode image data")
            
            // Validate region bounds
            val validRegion = CaptureRegion(
                x = maxOf(0, minOf(region.x, fullBitmap.width - 1)),
                y = maxOf(0, minOf(region.y, fullBitmap.height - 1)),
                width = maxOf(1, minOf(region.width, fullBitmap.width - region.x)),
                height = maxOf(1, minOf(region.height, fullBitmap.height - region.y))
            )
            
            // Crop bitmap to region
            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap,
                validRegion.x,
                validRegion.y,
                validRegion.width,
                validRegion.height
            )
            
            // Convert cropped bitmap back to bytes
            val stream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val croppedBytes = stream.toByteArray()
            
            // Cleanup
            fullBitmap.recycle()
            croppedBitmap.recycle()
            
            AppLogger.d("ScreenCaptureService", "Cropped to event title region: ${croppedBytes.size} bytes")
            croppedBytes
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error cropping to event title region", e)
            throw e
        }
    }
    
    suspend fun captureScreen(): CaptureResult = withContext(Dispatchers.IO) {
        try {
            val projection = MediaProjectionManager.getInstance().getProjection()
                ?: return@withContext CaptureResult.Error("MediaProjection not available")
            
            if (!MediaProjectionManager.getInstance().isProjectionValid()) {
                return@withContext CaptureResult.Error("MediaProjection is invalid")
            }
            
            AppLogger.d("ScreenCaptureService", "Using validated MediaProjection")
            
            val dimensions = getScreenDimensions()
            val imageData = performCaptureWithRetry(projection, dimensions)
            
            AppLogger.d("ScreenCaptureService", "Capture successful: ${imageData.size} bytes")
            CaptureResult.Success(imageData)
            
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Capture failed", e)
            CaptureResult.Error("Screen capture failed: ${e.message}")
        } finally {
            cleanup()
        }
    }
    
    // ✅ New: Retry logic for Android 13+ empty buffer issue
    private suspend fun performCaptureWithRetry(
        projection: MediaProjection, 
        dimensions: ScreenDimensions,
        maxRetries: Int = 5
    ): ByteArray {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                AppLogger.d("ScreenCaptureService", "Capture attempt ${attempt + 1}/$maxRetries")
                
                // Create fresh VirtualDisplay for each attempt
                val imageData = performSingleCapture(projection, dimensions)
                
                // Validate the captured image
                if (isImageValid(imageData)) {
                    AppLogger.d("ScreenCaptureService", "✅ Valid image captured on attempt ${attempt + 1}")
                    return imageData
                } else {
                    AppLogger.w("ScreenCaptureService", "⚠️ Empty/invalid image on attempt ${attempt + 1}, retrying...")
                    
                    // Small delay before retry
                    delay(100)
                }
                
            } catch (e: Exception) {
                lastException = e
                AppLogger.w("ScreenCaptureService", "❌ Attempt ${attempt + 1} failed: ${e.message}")
                
                // Cleanup before retry
                cleanup()
                delay(200)
            }
        }
        
        throw Exception("Failed to capture valid image after $maxRetries attempts. Last error: ${lastException?.message}")
    }
    
    // ✅ Updated: Single capture with timeout
    private suspend fun performSingleCapture(
        projection: MediaProjection, 
        dimensions: ScreenDimensions
    ): ByteArray = withTimeout(2000) { // 2 second timeout
        suspendCoroutine { continuation ->
            try {
                captureHandler = Handler(Looper.getMainLooper())
                
                // Create fresh ImageReader for this attempt
                imageReader = ImageReader.newInstance(
                    dimensions.width,
                    dimensions.height,
                    PixelFormat.RGBA_8888,
                    2 // Allow 2 images for better reliability
                )
                
                var imageProcessed = false
                var emptyImageCount = 0
                
                imageReader!!.setOnImageAvailableListener({ reader ->
                    if (imageProcessed) return@setOnImageAvailableListener
                    
                    var image: Image? = null
                    try {
                        image = reader.acquireLatestImage()
                        if (image != null) {
                            val bitmap = convertImageToBitmapSafely(image, dimensions)
                            
                            // ✅ Check if bitmap is empty (Android 13+ bug)
                            if (bitmap.isEmptyBitmap()) {
                                emptyImageCount++
                                AppLogger.w("ScreenCaptureService", "📷 Empty bitmap received (count: $emptyImageCount)")
                                
                                if (emptyImageCount >= 3) {
                                    imageProcessed = true
                                    continuation.resumeWithException(Exception("Too many empty bitmaps"))
                                }
                                // Don't stop listener, wait for next image
                                return@setOnImageAvailableListener
                            }
                            
                            imageProcessed = true
                            val bytes = bitmapToByteArray(bitmap)
                            continuation.resume(bytes)
                        }
                    } catch (e: Exception) {
                        if (!imageProcessed) {
                            imageProcessed = true
                            continuation.resumeWithException(e)
                        }
                    } finally {
                        image?.close()
                    }
                }, captureHandler)
                
                // Create VirtualDisplay for this capture
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
                
                AppLogger.d("ScreenCaptureService", "VirtualDisplay created: ${dimensions.width}x${dimensions.height}")
                
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
    
    // ✅ Add empty bitmap detection
    // ScreenCaptureService.kt (Fixed isEmptyBitmap method)
    private fun Bitmap.isEmptyBitmap(): Boolean {
        return try {
            // ✅ Fix: Handle nullable config
            val bitmapConfig = this.config ?: Bitmap.Config.ARGB_8888
            val emptyBitmap = Bitmap.createBitmap(width, height, bitmapConfig)
            val result = this.sameAs(emptyBitmap)
            emptyBitmap.recycle()
            result
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error checking empty bitmap", e)
            false
        }
    }
    
    // ✅ Add image validation
    private fun isImageValid(imageData: ByteArray): Boolean {
        if (imageData.size < 1000) {
            AppLogger.d("ScreenCaptureService", "❌ Image too small: ${imageData.size} bytes")
            return false
        }
        
        // Check if image is mostly zeros (blank/black)
        var nonZeroBytes = 0
        val sampleSize = minOf(1000, imageData.size)
        
        for (i in 0 until sampleSize) {
            if (imageData[i] != 0.toByte()) {
                nonZeroBytes++
            }
        }
        
        val nonZeroPercentage = (nonZeroBytes.toFloat() / sampleSize * 100)
        AppLogger.d("ScreenCaptureService", "📊 Image validation: ${nonZeroPercentage.toInt()}% non-zero bytes")
        
        return nonZeroPercentage > 5 // At least 5% should be non-zero
    }
    
    // ... rest of your existing methods (convertImageToBitmapSafely, bitmapToByteArray, etc.)
    
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
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            bitmap.recycle()
            AppLogger.d("ScreenCaptureService", "Bitmap converted to ${bytes.size} bytes")
            bytes
        } catch (e: Exception) {
            AppLogger.e("ScreenCaptureService", "Error converting bitmap to bytes", e)
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