// // ScreenCaptureService.kt (Complete fixed version)
// package com.example.umazing_helper

// import android.content.Context
// import android.graphics.Bitmap
// import android.graphics.PixelFormat
// import android.hardware.display.DisplayManager
// import android.hardware.display.VirtualDisplay
// import android.media.Image
// import android.media.ImageReader
// import android.media.projection.MediaProjection
// import android.os.Handler
// import android.os.Looper
// import android.util.DisplayMetrics
// import android.view.WindowManager
// import kotlinx.coroutines.*
// import java.io.ByteArrayOutputStream
// import java.nio.ByteBuffer
// import kotlin.coroutines.resume
// import kotlin.coroutines.resumeWithException
// import kotlin.coroutines.suspendCoroutine

// class ScreenCaptureService(private val context: Context) {
    
//     private var virtualDisplay: VirtualDisplay? = null
//     private var imageReader: ImageReader? = null
//     private var captureHandler: Handler? = null
    
//     // Fixed: Complete captureScreen method
//     suspend fun captureScreen(): CaptureResult = withContext(Dispatchers.IO) {
//         try {
//             // Get MediaProjection from persistent service
//             val projection = MediaProjectionManager.getInstance().getProjection()
//                 ?: return@withContext CaptureResult.Error("MediaProjection service not available - restart app and grant permission")
            
//             if (!MediaProjectionManager.getInstance().isProjectionValid()) {
//                 return@withContext CaptureResult.Error("MediaProjection is invalid - restart app and grant permission")
//             }        
//             AppLogger.d("ScreenCaptureService", "Using MediaProjection from persistent service")
            
//             val dimensions = getScreenDimensions()

//             delay(500)

//             val imageData = performCapture(projection, dimensions)
            
//             AppLogger.d("ScreenCaptureService", "Persistent service capture successful: ${imageData.size} bytes")
//             CaptureResult.Success(imageData)
            
//         } catch (e: Exception) {
//             AppLogger.e("ScreenCaptureService", "Persistent service capture failed", e)
//             CaptureResult.Error("Screen capture failed: ${e.message}", e)
//         } finally {
//             // Only clean up capture resources, keep MediaProjection in service
//             cleanup()
//         }
//     }
    
//     // Fixed: Add missing performCapture method
//     private suspend fun performCapture(
//         projection: MediaProjection, 
//         dimensions: ScreenDimensions
//     ): ByteArray = suspendCoroutine { continuation ->
        
//         try {
//             // Create handler for ImageReader callbacks
//             captureHandler = Handler(Looper.getMainLooper())
            
//             // Create ImageReader with safe parameters
//             imageReader = ImageReader.newInstance(
//                 dimensions.width,
//                 dimensions.height,
//                 PixelFormat.RGBA_8888,
//                 1 // Only 1 image to prevent buffer issues
//             ).apply {
//                 var imageProcessed = false
                
//                 setOnImageAvailableListener({ reader ->
//                     if (imageProcessed) return@setOnImageAvailableListener
                    
//                     var image: Image? = null
//                     try {
//                         image = reader.acquireLatestImage()
//                         if (image != null) {
//                             imageProcessed = true
                            
//                             // Convert image to bitmap safely
//                             val bitmap = convertImageToBitmapSafely(image, dimensions)
//                             val bytes = bitmapToByteArray(bitmap)
                            
//                             continuation.resume(bytes)
//                         } else {
//                             if (!imageProcessed) {
//                                 imageProcessed = true
//                                 continuation.resumeWithException(Exception("Failed to acquire image"))
//                             }
//                         }
//                     } catch (e: Exception) {
//                         if (!imageProcessed) {
//                             imageProcessed = true
//                             continuation.resumeWithException(e)
//                         }
//                     } finally {
//                         image?.close()
//                     }
//                 }, captureHandler)
//             }
            
//             // Create VirtualDisplay
//             virtualDisplay = projection.createVirtualDisplay(
//                 "ScreenCapture-${System.currentTimeMillis()}",
//                 dimensions.width,
//                 dimensions.height,
//                 dimensions.density,
//                 DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                 imageReader!!.surface,
//                 null,
//                 null
//             )
            
//             AppLogger.d("ScreenCaptureService", "VirtualDisplay created: ${dimensions.width}x${dimensions.height}")
            
//         } catch (e: Exception) {
//             continuation.resumeWithException(e)
//         }
//     }
    
//     // Fixed: Add missing convertImageToBitmapSafely method
//     private fun convertImageToBitmapSafely(image: Image, dimensions: ScreenDimensions): Bitmap {
//         try {
//             val planes = image.planes
//             val buffer = planes[0].buffer
//             val pixelStride = planes[0].pixelStride
//             val rowStride = planes[0].rowStride
//             val rowPadding = rowStride - pixelStride * dimensions.width
            
//             AppLogger.d("ScreenCaptureService", "Image info - pixelStride: $pixelStride, rowStride: $rowStride, rowPadding: $rowPadding")
            
//             // Calculate safe bitmap dimensions
//             val bitmapWidth = dimensions.width + rowPadding / pixelStride
//             val bitmapHeight = dimensions.height
            
//             // Validate buffer size
//             val expectedSize = rowStride * bitmapHeight
//             val actualSize = buffer.remaining()
            
//             if (actualSize < expectedSize) {
//                 throw Exception("Buffer too small: expected $expectedSize, got $actualSize")
//             }
            
//             // Create bitmap with safe parameters
//             val bitmap = Bitmap.createBitmap(
//                 bitmapWidth,
//                 bitmapHeight,
//                 Bitmap.Config.ARGB_8888
//             )
            
//             // Copy pixels safely
//             buffer.rewind()
//             bitmap.copyPixelsFromBuffer(buffer)
            
//             // Crop to actual dimensions if needed
//             return if (rowPadding == 0) {
//                 bitmap
//             } else {
//                 val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, dimensions.width, dimensions.height)
//                 bitmap.recycle() // Clean up original bitmap
//                 croppedBitmap
//             }
            
//         } catch (e: Exception) {
//             AppLogger.e("ScreenCaptureService", "Error converting image to bitmap", e)
//             throw e
//         }
//     }
    
//     // Fixed: Add missing bitmapToByteArray method
//     private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
//         return try {
//             val stream = ByteArrayOutputStream()
//             bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//             val bytes = stream.toByteArray()
            
//             // Clean up bitmap
//             bitmap.recycle()
            
//             AppLogger.d("ScreenCaptureService", "Bitmap converted to ${bytes.size} bytes")
//             bytes
//         } catch (e: Exception) {
//             AppLogger.e("ScreenCaptureService", "Error converting bitmap to bytes", e)
//             throw e
//         }
//     }
    
//     // Fixed: Add missing getScreenDimensions method
//     private fun getScreenDimensions(): ScreenDimensions {
//         val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//         val displayMetrics = DisplayMetrics()
//         windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
//         val dimensions = ScreenDimensions(
//             width = displayMetrics.widthPixels,
//             height = displayMetrics.heightPixels,
//             density = displayMetrics.densityDpi
//         )
        
//         AppLogger.d("ScreenCaptureService", "Screen dimensions: ${dimensions.width}x${dimensions.height} @ ${dimensions.density}dpi")
//         return dimensions
//     }
    
//     // Fixed: Add missing cleanup method
//     private fun cleanup() {
//         try {
//             virtualDisplay?.release()
//             virtualDisplay = null
            
//             imageReader?.close()
//             imageReader = null
            
//             captureHandler?.removeCallbacksAndMessages(null)
//             captureHandler = null
            
//             AppLogger.d("ScreenCaptureService", "Cleanup completed")
//         } catch (e: Exception) {
//             AppLogger.e("ScreenCaptureService", "Error during cleanup", e)
//         }
//     }
// }
// ScreenCaptureService.kt (Fixed with retry logic for Android 13+)
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

class ScreenCaptureService(private val context: Context) {
    
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureHandler: Handler? = null
    
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