package com.example.umazing_helper

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class AppMethodCallHandler(private val mainActivity: MainActivity) : MethodChannel.MethodCallHandler {
    
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        AppLogger.d("MethodCallHandler", "Received method call: ${call.method}")
        
        try {
            when (call.method) {
                "test" -> handleTest(result)
                "hasScreenCapturePermission" -> handleHasScreenCapturePermission(result)
                "hasOverlayPermission" -> handleHasOverlayPermission(result)
                "requestScreenCapturePermission" -> handleRequestScreenCapturePermission(result)
                "captureScreen" -> handleCaptureScreen(result)
                "startOverlayService" -> handleStartOverlayService(result)
                "stopOverlayService" -> handleStopOverlayService(result)
                "showEventResult" -> handleShowEventResult(call, result)
                "updateScanButtonAppearance" -> handleUpdateScanButtonAppearance(call, result)
                "launchUmaMusume" -> handleLaunchUmaMusume(result)
                else -> {
                    AppLogger.w("MethodCallHandler", "Unknown method: ${call.method}")
                    result.notImplemented()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("MethodCallHandler", "Error handling method call: ${call.method}", e)
            result.error("HANDLER_ERROR", "Error handling method call: ${e.message}", null)
        }
    }

    private fun handleTest(result: MethodChannel.Result) {
        val captureStatus = if (mainActivity.hasScreenCapturePermission()) "OK" else "MISSING"
        val overlayStatus = if (mainActivity.hasOverlayPermission()) "OK" else "MISSING"
        val response = "Method channel working! Capture: $captureStatus, Overlay: $overlayStatus"
        result.success(response)
    }

    private fun handleHasScreenCapturePermission(result: MethodChannel.Result) {
        result.success(mainActivity.hasScreenCapturePermission())
    }

    private fun handleHasOverlayPermission(result: MethodChannel.Result) {
        result.success(mainActivity.hasOverlayPermission())
    }

    private fun handleRequestScreenCapturePermission(result: MethodChannel.Result) {
        mainActivity.requestScreenCapturePermission(result)
    }

    private fun handleCaptureScreen(result: MethodChannel.Result) {
        mainActivity.captureScreen(result)
    }

    private fun handleStartOverlayService(result: MethodChannel.Result) {
        mainActivity.startOverlayService(result)
    }

    private fun handleStopOverlayService(result: MethodChannel.Result) {
        mainActivity.stopOverlayService(result)
    }

    private fun handleShowEventResult(call: MethodCall, result: MethodChannel.Result) {
        try {
            val eventName = call.argument<String>("eventName") ?: ""
            val characterName = call.argument<String>("characterName")
            val eventType = call.argument<String>("eventType") ?: "unknown"
            val confidence = call.argument<Double>("confidence") ?: 0.0
            val options = call.argument<Map<String, String>>("options") ?: emptyMap()
            
            mainActivity.showEventResult(eventName, characterName, eventType, confidence, options)
            result.success(true)
            AppLogger.d("MethodCallHandler", "Event result overlay shown")
        } catch (e: Exception) {
            AppLogger.e("MethodCallHandler", "Error showing event result", e)
            result.error("SHOW_RESULT_ERROR", e.message, null)
        }
    }
    
    private fun handleUpdateScanButtonAppearance(call: MethodCall, result: MethodChannel.Result) {
        try {
            val size = call.argument<Double>("size")?.toFloat() ?: 60.0f
            val opacity = call.argument<Double>("opacity")?.toFloat() ?: 0.9f
            
            AppLogger.d("MethodCallHandler", "🎨 Updating scan button: size=${size}dp, opacity=$opacity")
            
            // Update the overlay service's scan button appearance
            mainActivity.updateScanButtonAppearance(size, opacity)
            
            result.success(true)
            AppLogger.d("MethodCallHandler", "✅ Scan button appearance updated")
        } catch (e: Exception) {
            AppLogger.e("MethodCallHandler", "Error updating scan button appearance", e)
            result.error("UPDATE_BUTTON_ERROR", e.message, null)
        }
    }
    
    private fun handleLaunchUmaMusume(result: MethodChannel.Result) {
        try {
            val packageName = "com.cygames.umamusume"
            
            AppLogger.d("MethodCallHandler", "🔍 Looking for Uma Musume app: $packageName")
            
            val launchIntent = mainActivity.packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent != null) {
                AppLogger.d("MethodCallHandler", "✅ Found Uma Musume!")
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                mainActivity.startActivity(launchIntent)
                result.success(true)
            } else {
                AppLogger.w("MethodCallHandler", "❌ Uma Musume app not found (package: $packageName)")
                result.success(false)
            }
        } catch (e: Exception) {
            AppLogger.e("MethodCallHandler", "Error launching Uma Musume", e)
            result.error("LAUNCH_APP_ERROR", e.message, null)
        }
    }
}