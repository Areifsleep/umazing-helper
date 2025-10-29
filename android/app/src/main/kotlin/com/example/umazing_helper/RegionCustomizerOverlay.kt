package com.example.umazing_helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class RegionCustomizerOverlay(private val context: Context) {
    
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private var overlayView: View? = null
    private var selectionBox: View? = null
    private var coordsText: TextView? = null
    
    // Current region coordinates
    private var regionX = 100
    private var regionY = 500
    private var regionWidth = 900
    private var regionHeight = 150
    
    // Drag state
    private var isDragging = false
    private var isResizing = false
    private var activeHandle: ResizeHandle = ResizeHandle.NONE
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialX = 0
    private var initialY = 0
    private var initialWidth = 0
    private var initialHeight = 0
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
    }
    
    // Status bar height for coordinate adjustment
    private var statusBarHeight = 0
    
    enum class ResizeHandle {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        MOVE
    }
    
    fun show() {
        if (overlayView != null) {
            AppLogger.d("RegionCustomizerOverlay", "Overlay already shown")
            return
        }
        
        try {
            AppLogger.d("RegionCustomizerOverlay", "Creating region customizer overlay...")
            
            // Detect status bar height for debugging (not used for coordinate conversion)
            statusBarHeight = getStatusBarHeight()
            
            // Get full screen dimensions
            val screenDimensions = getScreenDimensions()
            
            AppLogger.d("RegionCustomizerOverlay", "📐 Screen info:")
            AppLogger.d("RegionCustomizerOverlay", "   Full screen size: ${screenDimensions.x}x${screenDimensions.y}")
            AppLogger.d("RegionCustomizerOverlay", "   Status bar height: $statusBarHeight px")
            AppLogger.d("RegionCustomizerOverlay", "   ✅ Overlay can reach Y=0 (top of screen)")
            AppLogger.d("RegionCustomizerOverlay", "   ✅ Max Y value: ${screenDimensions.y}")
            
            // Inflate layout
            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.region_customizer_overlay, null)
            
            // Setup views
            setupViews()
            
            // Add to window
            addOverlayToWindow()
            
            AppLogger.d("RegionCustomizerOverlay", "✅ Region customizer overlay shown")
        } catch (e: Exception) {
            AppLogger.e("RegionCustomizerOverlay", "Failed to show overlay", e)
            showToast("❌ Error: ${e.message}")
        }
    }
    
    private fun setupViews() {
        selectionBox = overlayView?.findViewById(R.id.selectionBox)
        coordsText = overlayView?.findViewById(R.id.coordsText)
        
        // Load saved region or use default
        loadSavedRegion()
        
        // Update initial position
        updateSelectionBoxPosition()
        updateCoordinatesDisplay()
        
        // Setup touch listeners for handles
        setupHandleTouchListeners()
        
        // Setup touch listener for moving entire box
        selectionBox?.setOnTouchListener { view, event ->
            handleBoxTouch(view, event)
        }
        
        // Confirm button
        overlayView?.findViewById<Button>(R.id.confirmButton)?.setOnClickListener {
            AppLogger.d("RegionCustomizerOverlay", "Confirm clicked")
            saveRegion()
            hide()
            showToast("✅ Region saved!")
        }
        
        // Cancel button
        overlayView?.findViewById<Button>(R.id.cancelButton)?.setOnClickListener {
            AppLogger.d("RegionCustomizerOverlay", "Cancel clicked")
            hide()
            showToast("Cancelled")
        }
    }
    
    private fun setupHandleTouchListeners() {
        // Top Left Handle
        overlayView?.findViewById<View>(R.id.handleTopLeft)?.setOnTouchListener { view, event ->
            handleResizeTouch(view, event, ResizeHandle.TOP_LEFT)
        }
        
        // Top Right Handle
        overlayView?.findViewById<View>(R.id.handleTopRight)?.setOnTouchListener { view, event ->
            handleResizeTouch(view, event, ResizeHandle.TOP_RIGHT)
        }
        
        // Bottom Left Handle
        overlayView?.findViewById<View>(R.id.handleBottomLeft)?.setOnTouchListener { view, event ->
            handleResizeTouch(view, event, ResizeHandle.BOTTOM_LEFT)
        }
        
        // Bottom Right Handle
        overlayView?.findViewById<View>(R.id.handleBottomRight)?.setOnTouchListener { view, event ->
            handleResizeTouch(view, event, ResizeHandle.BOTTOM_RIGHT)
        }
    }
    
    private fun handleBoxTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                activeHandle = ResizeHandle.MOVE
                dragStartX = event.rawX
                dragStartY = event.rawY
                initialX = regionX
                initialY = regionY
                AppLogger.d("RegionCustomizerOverlay", "Started dragging box at rawY=${event.rawY.toInt()} (absolute screen coordinate)")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = (event.rawX - dragStartX).toInt()
                    val deltaY = (event.rawY - dragStartY).toInt()
                    
                    // Update screen coordinates (used for capture)
                    regionX = initialX + deltaX
                    regionY = initialY + deltaY
                    
                    // Constrain to screen bounds
                    constrainToScreen()
                    
                    updateSelectionBoxPosition()
                    updateCoordinatesDisplay()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                activeHandle = ResizeHandle.NONE
                AppLogger.d("RegionCustomizerOverlay", "Stopped dragging: X=$regionX Y=$regionY")
                return true
            }
        }
        return false
    }
    
    private fun handleResizeTouch(view: View, event: MotionEvent, handle: ResizeHandle): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isResizing = true
                activeHandle = handle
                dragStartX = event.rawX
                dragStartY = event.rawY
                initialX = regionX
                initialY = regionY
                initialWidth = regionWidth
                initialHeight = regionHeight
                AppLogger.d("RegionCustomizerOverlay", "Started resizing with handle: $handle")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing) {
                    val deltaX = (event.rawX - dragStartX).toInt()
                    val deltaY = (event.rawY - dragStartY).toInt()
                    
                    when (activeHandle) {
                        ResizeHandle.TOP_LEFT -> {
                            regionX = initialX + deltaX
                            regionY = initialY + deltaY
                            regionWidth = initialWidth - deltaX
                            regionHeight = initialHeight - deltaY
                        }
                        ResizeHandle.TOP_RIGHT -> {
                            regionY = initialY + deltaY
                            regionWidth = initialWidth + deltaX
                            regionHeight = initialHeight - deltaY
                        }
                        ResizeHandle.BOTTOM_LEFT -> {
                            regionX = initialX + deltaX
                            regionWidth = initialWidth - deltaX
                            regionHeight = initialHeight + deltaY
                        }
                        ResizeHandle.BOTTOM_RIGHT -> {
                            regionWidth = initialWidth + deltaX
                            regionHeight = initialHeight + deltaY
                        }
                        else -> {}
                    }
                    
                    // Constrain minimum size
                    if (regionWidth < 100) regionWidth = 100
                    if (regionHeight < 50) regionHeight = 50
                    
                    // Constrain to screen bounds
                    constrainToScreen()
                    
                    updateSelectionBoxPosition()
                    updateCoordinatesDisplay()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isResizing = false
                activeHandle = ResizeHandle.NONE
                AppLogger.d("RegionCustomizerOverlay", "Stopped resizing: ${regionWidth}x$regionHeight")
                return true
            }
        }
        return false
    }
    
    private fun updateSelectionBoxPosition() {
        val params = selectionBox?.layoutParams as? FrameLayout.LayoutParams
        params?.leftMargin = regionX
        // regionY is already in window coordinates from drag
        // No offset needed - display exactly where user dragged it
        params?.topMargin = regionY
        params?.width = regionWidth
        params?.height = regionHeight
        selectionBox?.layoutParams = params
    }
    
    private fun updateCoordinatesDisplay() {
        // Display the exact coordinates that will be used for capture
        coordsText?.text = "X: $regionX  Y: $regionY\nW: $regionWidth  H: $regionHeight"
    }
    
    private fun loadSavedRegion() {
        if (prefs.contains("flutter.capture_region_x")) {
            regionX = prefs.getInt("flutter.capture_region_x", 100)
            regionY = prefs.getInt("flutter.capture_region_y", 500)  // Absolute screen Y
            regionWidth = prefs.getInt("flutter.capture_region_width", 900)
            regionHeight = prefs.getInt("flutter.capture_region_height", 150)
            
            AppLogger.d("RegionCustomizerOverlay", "📦 Loaded saved region (absolute coordinates):")
            AppLogger.d("RegionCustomizerOverlay", "   X=$regionX Y=$regionY W=$regionWidth H=$regionHeight")
        } else {
            // Use default percentage-based
            val dimensions = getScreenDimensions()
            regionX = (dimensions.x * 0.08).toInt()
            regionY = (dimensions.y * 0.18).toInt()  // Absolute screen Y
            regionWidth = (dimensions.x * 0.84).toInt()
            regionHeight = (dimensions.y * 0.08).toInt()
            
            AppLogger.d("RegionCustomizerOverlay", "📦 Using default region (absolute coordinates):")
            AppLogger.d("RegionCustomizerOverlay", "   X=$regionX Y=$regionY W=$regionWidth H=$regionHeight")
        }
    }
    
    private fun saveRegion() {
        // Save absolute screen coordinates (same as overlay position)
        // MediaProjection captures the FULL screen bitmap including status bar,
        // so we use the exact overlay position for cropping
        prefs.edit().apply {
            putInt("flutter.capture_region_x", regionX)
            putInt("flutter.capture_region_y", regionY)  // Save absolute Y position
            putInt("flutter.capture_region_width", regionWidth)
            putInt("flutter.capture_region_height", regionHeight)
            apply()
        }
        
        AppLogger.d("RegionCustomizerOverlay", "💾 Region saved (absolute screen coordinates):")
        AppLogger.d("RegionCustomizerOverlay", "   X=$regionX Y=$regionY (rawY from touch)")
        AppLogger.d("RegionCustomizerOverlay", "   W=$regionWidth H=$regionHeight")
        AppLogger.d("RegionCustomizerOverlay", "   ℹ️ MediaProjection will crop from these exact coordinates")
    }
    
    private fun constrainToScreen() {
        val dimensions = getScreenDimensions()
        
        // Ensure region stays within screen bounds
        if (regionX < 0) regionX = 0
        if (regionY < 0) regionY = 0
        if (regionX + regionWidth > dimensions.x) {
            regionX = dimensions.x - regionWidth
        }
        if (regionY + regionHeight > dimensions.y) {
            regionY = dimensions.y - regionHeight
        }
    }
    
    private fun addOverlayToWindow() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            
            // Allow overlay to extend to full screen (including status bar area)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        
        windowManager.addView(overlayView, params)
        AppLogger.d("RegionCustomizerOverlay", "Overlay added to window (full screen access)")
    }
    
    fun hide() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
                AppLogger.d("RegionCustomizerOverlay", "✅ Overlay removed")
            }
        } catch (e: Exception) {
            AppLogger.e("RegionCustomizerOverlay", "Error removing overlay", e)
        }
    }
    
    private fun getScreenDimensions(): Point {
        val dimensions = Point()
        windowManager.defaultDisplay.getRealSize(dimensions)
        return dimensions
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        AppLogger.d("RegionCustomizerOverlay", "Status bar height: ${result}px")
        return result
    }
    
    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
