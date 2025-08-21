package com.example.umazing_helper

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button

class OverlayManager(private val context: Context) {
    
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private var overlayView: View? = null
    private var onScanClickListener: (() -> Unit)? = null
    
    fun createOverlay(onScanClick: () -> Unit) {
        // Fix: Use the companion object method
        if (!PermissionManager.hasOverlayPermission(context)) {
            throw SecurityException("Overlay permission not granted")
        }
        
        onScanClickListener = onScanClick
        
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_layout, null)
        
        setupOverlayView()
        addOverlayToWindow()
    }
    
    private fun setupOverlayView() {
        var lastClickTime = 0L
    
        overlayView?.findViewById<Button>(R.id.scanButton)?.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 2000) { // 2 second cooldown
                android.widget.Toast.makeText(context, "⏳ Please wait 2 seconds between captures", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lastClickTime = currentTime
            
            onScanClickListener?.invoke()
        }
        
        overlayView?.findViewById<Button>(R.id.closeButton)?.setOnClickListener {
            removeOverlay()
        }
    }
    
    private fun addOverlayToWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        try {
            windowManager.addView(overlayView, params)
            AppLogger.d("OverlayManager", "Overlay added to window")
        } catch (e: Exception) {
            AppLogger.e("OverlayManager", "Failed to add overlay", e)
            throw e
        }
    }
    
    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
    
    fun removeOverlay() {
        try {
            overlayView?.let { 
                windowManager.removeView(it)
                AppLogger.d("OverlayManager", "Overlay removed from window")
            }
            overlayView = null
            onScanClickListener = null
        } catch (e: Exception) {
            AppLogger.e("OverlayManager", "Failed to remove overlay", e)
        }
    }
    
    fun isOverlayVisible(): Boolean {
        return overlayView != null
    }
}