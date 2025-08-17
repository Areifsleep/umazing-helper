// MainActivity.kt (Fixed)
package com.example.umazing_helper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {
    companion object {
        private const val CHANNEL = "uma_screen_capture"
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1000
        private const val TAG = "MainActivity"
    }

    private lateinit var permissionManager: PermissionManager
    private lateinit var screenCaptureService: ScreenCaptureService
    private lateinit var overlayServiceController: OverlayServiceController
    private var pendingResult: MethodChannel.Result? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeServices()
        AppLogger.d(TAG, "MainActivity created")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        setupMethodChannel(flutterEngine)
        AppLogger.d(TAG, "Flutter engine configured")
    }

    private fun initializeServices() {
        permissionManager = PermissionManager(this)
        screenCaptureService = ScreenCaptureService(this) 
        overlayServiceController = OverlayServiceController(this)
    }

    private fun setupMethodChannel(flutterEngine: FlutterEngine) {
        val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel.setMethodCallHandler(AppMethodCallHandler(this))
        AppLogger.d(TAG, "Method channel setup completed")
    }

    // Permission management
    private var isServiceStarted = false
    
    fun requestScreenCapturePermission(result: MethodChannel.Result) {
        if (!permissionManager.isScreenCaptureSupported()) {
            result.error("UNSUPPORTED", "Screen capture requires API level 21+", null)
            return
        }

        // Step 1: Start the foreground service FIRST
        try {
            AppLogger.d(TAG, "Starting MediaProjectionService before requesting permission...")
            MediaProjectionService.startService(this)
            isServiceStarted = true
            
            // Step 2: Wait a moment for service to fully start, then request permission
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    pendingResult = result
                    val captureIntent = permissionManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
                    AppLogger.d(TAG, "Screen capture permission request sent")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Error requesting screen capture permission", e)
                    result.error("REQUEST_ERROR", e.message, null)
                    pendingResult = null
                }
            }, 500) // 500ms delay to ensure service is fully started
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start MediaProjectionService", e)
            result.error("SERVICE_START_ERROR", e.message, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    AppLogger.d(TAG, "Screen capture permission granted, creating MediaProjection...")
                    
                    // Step 3: Create MediaProjection and store it (service is already running)
                    val mediaProjection = permissionManager.createMediaProjection(resultCode, data)
                    MediaProjectionService.setMediaProjection(mediaProjection)
                    
                    pendingResult?.success(true)
                    AppLogger.d(TAG, "MediaProjection created and stored successfully")
                    
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to create MediaProjection", e)
                    pendingResult?.success(false)
                }
            } else {
                pendingResult?.success(false)
                AppLogger.w(TAG, "Screen capture permission denied")
                
                // Stop service if permission denied
                if (isServiceStarted) {
                    MediaProjectionService.stop(this)
                    isServiceStarted = false
                }
            }
            pendingResult = null
        }
    }

    private fun handleScreenCapturePermissionResult(resultCode: Int, data: Intent?) {
        val success = resultCode == Activity.RESULT_OK && data != null
        
        if (success) {
            try {
                val mediaProjection = permissionManager.createMediaProjection(resultCode, data!!)
                MediaProjectionManager.getInstance().setMediaProjection(mediaProjection)
                pendingResult?.success(true)
                AppLogger.d(TAG, "Screen capture permission granted")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error creating MediaProjection", e)
                pendingResult?.error("PROJECTION_ERROR", e.message, null)
            }
        } else {
            AppLogger.d(TAG, "Screen capture permission denied")
            pendingResult?.success(false)
        }
        
        pendingResult = null
    }

    // Screen capture operations
    fun captureScreen(result: MethodChannel.Result) {
        lifecycleScope.launch {
            try {
                when (val captureResult = screenCaptureService.captureScreen()) {
                    is CaptureResult.Success -> {
                        result.success(captureResult.imageData)
                        AppLogger.d(TAG, "Screen captured successfully: ${captureResult.imageData.size} bytes")
                    }
                    is CaptureResult.Error -> {
                        result.error("CAPTURE_ERROR", captureResult.message, null)
                        AppLogger.e(TAG, "Screen capture failed: ${captureResult.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Unexpected error during screen capture", e)
                result.error("UNKNOWN_ERROR", e.message, null)
            }
        }
    }

    // Overlay management
    fun startOverlayService(result: MethodChannel.Result) {
        try {
            overlayServiceController.startOverlay()
            result.success("Overlay service started successfully")
            AppLogger.d(TAG, "Overlay service started")
        } catch (e: SecurityException) {
            result.error("PERMISSION_DENIED", e.message, null)
            AppLogger.e(TAG, "Overlay permission denied", e)
        } catch (e: Exception) {
            result.error("START_ERROR", e.message, null)
            AppLogger.e(TAG, "Failed to start overlay service", e)
        }
    }

    fun stopOverlayService(result: MethodChannel.Result) {
        try {
            overlayServiceController.stopOverlay()
            result.success("Overlay service stopped successfully")
            AppLogger.d(TAG, "Overlay service stopped")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error stopping overlay service", e)
            result.error("STOP_ERROR", e.message, null)
        }
    }

    // Status checks
    fun hasScreenCapturePermission(): Boolean {
        return MediaProjectionManager.getInstance().hasActiveProjection()
    }

    fun hasOverlayPermission(): Boolean {
        return permissionManager.hasOverlayPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceStarted) {
            MediaProjectionService.stop(this)
        }
        AppLogger.d(TAG, "MainActivity destroyed")
    }
}