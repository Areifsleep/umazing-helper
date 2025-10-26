// OverlayService.kt (Cleaned up version)
package com.example.umazing_helper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter

import android.app.ActivityManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import com.example.umazing_helper.MediaProjectionManager

class OverlayService : Service() {
    
    private lateinit var overlayManager: OverlayManager
    private lateinit var screenCaptureService: ScreenCaptureService
    private lateinit var mediaProjectionManager: MediaProjectionManager // <-- STEP 1: Add this
    private var isCapturing = false
    private var tokenRevocationReceiver: BroadcastReceiver? = null


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("OverlayService", "onCreate")

        initializeServices()
        setupTokenRevocationReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d("OverlayService", "onStartCommand")
        
        // Check if MediaProjection is available
        if (!MediaProjectionManager.getInstance().hasActiveProjection()) {
            AppLogger.e("OverlayService", "No MediaProjection available, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        createOverlay()
        return START_STICKY
    }

    private fun setupTokenRevocationReceiver() {
        tokenRevocationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.example.umazing_helper.TOKEN_REVOKED" -> {
                        AppLogger.w("OverlayService", "🚨 Received token revocation broadcast")
                        handleTokenRevocation()
                    }
                }
            }
        }
        
        val filter = IntentFilter("com.example.umazing_helper.TOKEN_REVOKED")
        ContextCompat.registerReceiver(this, tokenRevocationReceiver, filter,
            ContextCompat.RECEIVER_NOT_EXPORTED)

        AppLogger.d("OverlayService", "📡 Token revocation receiver registered")
    }
    
    private fun initializeServices() {
        overlayManager = OverlayManager(this)
        screenCaptureService = ScreenCaptureService(this)
    }
    
    private fun createOverlay() {
        try {
            overlayManager.createOverlay {
                performScreenCapture()
            }
            AppLogger.d("OverlayService", "Overlay created successfully")
        } catch (e: SecurityException) {
            AppLogger.e("OverlayService", "Overlay permission denied", e)
            stopSelf()
        } catch (e: Exception) {
            AppLogger.e("OverlayService", "Failed to create overlay", e)
            stopSelf()
        }
    }

    
    private fun performScreenCapture() {
        if (isCapturing) {
            AppLogger.d("OverlayService", "Capture already in progress, ignoring")
            showToast("⏳ Capture in progress...")
            return
        }
        
        isCapturing = true
    

        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Debug: Check app's memory usage
                val runtime = Runtime.getRuntime()
                val maxMemory = runtime.maxMemory() / 1024 / 1024        // Max heap size for your app
                val totalMemory = runtime.totalMemory() / 1024 / 1024    // Current allocated heap
                val freeMemory = runtime.freeMemory() / 1024 / 1024      // Free within allocated heap
                val usedMemory = totalMemory - freeMemory                // Actually used memory
                
                AppLogger.d("OverlayService", "📊 APP MEMORY STATUS:")
                AppLogger.d("OverlayService", "   Max heap size: ${maxMemory}MB")
                AppLogger.d("OverlayService", "   Allocated heap: ${totalMemory}MB") 
                AppLogger.d("OverlayService", "   Used memory: ${usedMemory}MB")
                AppLogger.d("OverlayService", "   Free in heap: ${freeMemory}MB")
                AppLogger.d("OverlayService", "   Usage: ${(usedMemory.toFloat() / maxMemory * 100).toInt()}%")
                
                // Also check system memory
                val memInfo = android.app.ActivityManager.MemoryInfo()
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.getMemoryInfo(memInfo)
                
                val systemAvailable = memInfo.availMem / 1024 / 1024
                val systemTotal = memInfo.totalMem / 1024 / 1024
                
                AppLogger.d("OverlayService", "🏠 SYSTEM MEMORY STATUS:")
                AppLogger.d("OverlayService", "   Total device RAM: ${systemTotal}MB")
                AppLogger.d("OverlayService", "   Available system: ${systemAvailable}MB")
                AppLogger.d("OverlayService", "   System usage: ${((systemTotal - systemAvailable).toFloat() / systemTotal * 100).toInt()}%")
                
                if (freeMemory < 10) {
                    AppLogger.w("OverlayService", "🚨 APP HEAP ALMOST FULL!")
                    showToast("⚠️ App memory low: ${freeMemory}MB free of ${maxMemory}MB heap")
                }
                
            AppLogger.d("OverlayService", "🔍 Starting screen analysis (${freeMemory}MB free)...")
            
                
                // Double-check MediaProjection is still available
                if (!MediaProjectionManager.getInstance().hasActiveProjection()) {
                    AppLogger.e("OverlayService", "MediaProjection lost during capture")
                    showToast("❌ Screen capture permission lost")
                    return@launch
                }

                if (MediaProjectionManager.getInstance().isTokenRevoked()) {
                    AppLogger.e("OverlayService", "🚨 MediaProjection token was REVOKED!")
                    showToast("🚨 Permission revoked - restart app to capture again")
                    return@launch
                }

                if (!MediaProjectionManager.getInstance().isProjectionValid()) {
                    AppLogger.e("OverlayService", "❌ MediaProjection is invalid - attempting recreation")
                    showToast("❌ Screen capture permission expired")
                    
                    // Optionally try to recreate (if you have permission data stored)
                    // Or prompt user to restart app
                    return@launch
                }
                
                when (val result = screenCaptureService.captureEventTitleRegion()) {
                is CaptureResult.Success -> {
                    val sizeKB = result.imageData.size / 1024
                    AppLogger.d("OverlayService", "✅ Raw RGBA captured: ${sizeKB}KB (${result.width}x${result.height})")
                    
                    // No duplicate validation - trust ScreenCaptureService validation
                    sendImageToFlutter(result.imageData, result.width, result.height)
                    showToast("🔍 Analyzing event title...")
                }
                is CaptureResult.Error -> {
                    AppLogger.e("OverlayService", "Event title capture failed: ${result.message}")
                    showToast("❌ Capture failed: ${result.message}")
                }
            }
            } catch (e: Exception) {
                AppLogger.e("OverlayService", "Unexpected error during capture", e)
                showToast("❌ Capture error: ${e.message}")
            } finally {
                isCapturing = false
                AppLogger.d("OverlayService", "Capture finished, ready for next")
            }
        }
    }
    
    private fun sendImageToFlutter(imageData: ByteArray, width: Int, height: Int) {
        try {
            // Get MainActivity instance to send via MethodChannel
            val mainActivity = MainActivity.instance
            if (mainActivity != null) {
                mainActivity.runOnUiThread {
                    mainActivity.sendImageToFlutter(imageData, width, height)
                }
                AppLogger.d("OverlayService", "📤 Raw RGBA sent to Flutter (${imageData.size} bytes, ${width}x${height})")
            } else {
                AppLogger.e("OverlayService", "MainActivity instance not available")
                showToast("❌ Cannot send to Flutter")
            }
        } catch (e: Exception) {
            AppLogger.e("OverlayService", "Failed to send image to Flutter", e)
            showToast("❌ Processing failed")
        }
    }

    private fun handleTokenRevocation() {
        AppLogger.w("OverlayService", "🛑 Handling token revocation")
        showToast("🚨 Screen capture permission revoked")
        
        // Stop any ongoing captures
        isCapturing = false
        
        // Optionally stop the overlay service since it can't capture anymore
        stopSelf()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("OverlayService", "onDestroy")
        
        try {
            // Unregister token revocation receiver
            tokenRevocationReceiver?.let {
                unregisterReceiver(it)
                AppLogger.d("OverlayService", "📡 Token revocation receiver unregistered")
            }
            
            overlayManager.removeOverlay()
            AppLogger.d("OverlayService", "Overlay removed successfully")
        } catch (e: Exception) {
            AppLogger.e("OverlayService", "Error removing overlay", e)
        }
    }
}