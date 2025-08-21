// OverlayManager.kt (Updated with drag functionality)
package com.example.umazing_helper

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.Button
import kotlin.math.abs

class OverlayManager(private val context: Context) {
    
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private var overlayView: View? = null
    private var onScanClickListener: (() -> Unit)? = null
    
    // Drag-related variables
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private val CLICK_DRAG_TOLERANCE = 10f // Distance threshold for click vs drag
    
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
        
        AppLogger.d("OverlayManager", "✅ Draggable overlay created successfully")
    }
    
    private fun setupOverlayView() {
        overlayView?.let { view ->
            // Find the scan button in your layout
            val scanButton = view.findViewById<Button>(R.id.scanButton)
            
            scanButton?.let { button ->
                // Set up touch listener for drag functionality
                button.setOnTouchListener(createDragTouchListener())
                
                AppLogger.d("OverlayManager", "🖱️ Drag functionality added to scan button")
            } ?: run {
                AppLogger.e("OverlayManager", "❌ Scan button not found in overlay layout")
            }
        }
    }
    
    private fun createDragTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            val layoutParams = overlayView?.layoutParams as? WindowManager.LayoutParams
                ?: return@OnTouchListener false
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    
                    // Store initial positions
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    
                    AppLogger.d("OverlayManager", "👆 Touch started at: ${event.rawX}, ${event.rawY}")
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    // Calculate movement distance
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val distance = kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())
                    
                    // Start dragging if moved beyond threshold
                    if (distance > CLICK_DRAG_TOLERANCE) {
                        isDragging = true
                        
                        // Update overlay position
                        layoutParams.x = initialX + deltaX.toInt()
                        layoutParams.y = initialY + deltaY.toInt()
                        
                        // Apply position update
                        windowManager.updateViewLayout(overlayView, layoutParams)
                        
                        AppLogger.d("OverlayManager", "🖱️ Dragging to: ${layoutParams.x}, ${layoutParams.y}")
                    }
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // This was a click, not a drag
                        AppLogger.d("OverlayManager", "👆 Click detected - triggering scan")
                        onScanClickListener?.invoke()
                    } else {
                        // This was a drag - save final position
                        AppLogger.d("OverlayManager", "🖱️ Drag ended at: ${layoutParams.x}, ${layoutParams.y}")
                        saveOverlayPosition(layoutParams.x, layoutParams.y)
                    }
                    
                    isDragging = false
                    true
                }
                
                else -> false
            }
        }
    }
    
    private fun addOverlayToWindow() {
        val layoutParams = createLayoutParams()
        
        // Restore saved position or use default
        val savedPosition = getSavedOverlayPosition()
        layoutParams.x = savedPosition.first
        layoutParams.y = savedPosition.second
        
        AppLogger.d("OverlayManager", "📍 Overlay positioned at: ${layoutParams.x}, ${layoutParams.y}")
        
        windowManager.addView(overlayView, layoutParams)
    }
    
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
    
    private fun saveOverlayPosition(x: Int, y: Int) {
        try {
            val prefs = context.getSharedPreferences("overlay_settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("overlay_x", x)
                .putInt("overlay_y", y)
                .apply()
            
            AppLogger.d("OverlayManager", "💾 Overlay position saved: $x, $y")
        } catch (e: Exception) {
            AppLogger.e("OverlayManager", "Failed to save overlay position", e)
        }
    }
    
    private fun getSavedOverlayPosition(): Pair<Int, Int> {
        return try {
            val prefs = context.getSharedPreferences("overlay_settings", Context.MODE_PRIVATE)
            val x = prefs.getInt("overlay_x", 100) // Default x position
            val y = prefs.getInt("overlay_y", 200) // Default y position
            
            AppLogger.d("OverlayManager", "📂 Loaded overlay position: $x, $y")
            Pair(x, y)
        } catch (e: Exception) {
            AppLogger.e("OverlayManager", "Failed to load overlay position", e)
            Pair(100, 200) // Default position
        }
    }
    
    fun removeOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
                AppLogger.d("OverlayManager", "✅ Draggable overlay removed")
            }
        } catch (e: Exception) {
            AppLogger.e("OverlayManager", "❌ Error removing draggable overlay", e)
        }
    }
}