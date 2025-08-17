package com.example.umazing_helper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class OverlayService : Service() {
    
    private lateinit var overlayManager: OverlayManager
    private lateinit var screenCaptureService: ScreenCaptureService

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLogger.d("OverlayService", "onCreate")
        
        initializeServices()
        createOverlay()
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
        // Use coroutine scope from service
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                AppLogger.d("OverlayService", "Starting screen capture...")
                
                when (val result = screenCaptureService.captureScreen()) {
                    is CaptureResult.Success -> {
                        AppLogger.d("OverlayService", "Screen captured: ${result.imageData.size} bytes")
                        // You can save the image or send it somewhere
                        showToast("✅ Screen captured successfully!")
                    }
                    is CaptureResult.Error -> {
                        AppLogger.e("OverlayService", "Capture failed: ${result.message}")
                        showToast("❌ Capture failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("OverlayService", "Unexpected error during capture", e)
                showToast("❌ Capture error: ${e.message}")
            }
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("OverlayService", "onDestroy")
        
        try {
            overlayManager.removeOverlay()
        } catch (e: Exception) {
            AppLogger.e("OverlayService", "Error removing overlay", e)
        }
    }
}