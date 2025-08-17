// OverlayServiceController.kt
package com.example.umazing_helper
import android.content.Context
import android.content.Intent

class OverlayServiceController(private val context: Context) {
    
    fun startOverlay() {
        if (!PermissionManager.hasOverlayPermission(context)) {
            throw SecurityException("Overlay permission required")
        }
        
        val intent = Intent(context, OverlayService::class.java)
        context.startService(intent)
    }
    
    fun stopOverlay() {
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }
}