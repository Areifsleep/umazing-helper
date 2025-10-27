// EventResultOverlay.kt - Shows event result as overlay on top of other apps
package com.example.umazing_helper

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

class EventResultOverlay(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    
    data class EventResult(
        val eventName: String,
        val characterName: String?,
        val eventType: String,
        val confidence: Double,
        val options: Map<String, String>
    )
    
    fun showResult(result: EventResult) {
        removeOverlay() // Remove any existing overlay
        
        // Only show success modal if confidence >= 95%
        if (result.confidence < 0.94) {
            showErrorDialog(result.confidence)
            return
        }
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or  // Allow outside touches
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // Detect outside touches
            PixelFormat.TRANSLUCENT
        )
        
        // Position at top with margin
        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.y = 80 // Top margin in pixels (adjust as needed)
        
        // Create overlay view programmatically
        overlayView = createResultView(result)
        
        // Add touch listener to detect outside clicks
        overlayView?.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                removeOverlay()
                true
            } else {
                false
            }
        }
        
        try {
            windowManager.addView(overlayView, layoutParams)
            AppLogger.d("EventResultOverlay", "✅ Result overlay displayed (confidence: ${(result.confidence * 100).toInt()}%)")
        } catch (e: Exception) {
            AppLogger.e("EventResultOverlay", "Failed to show result overlay", e)
        }
    }
    
    private fun showErrorDialog(confidence: Double) {
        removeOverlay()
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        
        layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams.y = 80
        
        overlayView = createErrorView(confidence)
        
        // Add touch listener to detect outside clicks
        overlayView?.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                removeOverlay()
                true
            } else {
                false
            }
        }
        
        try {
            windowManager.addView(overlayView, layoutParams)
            AppLogger.d("EventResultOverlay", "❌ Error overlay displayed (confidence: ${(confidence * 100).toInt()}%)")
        } catch (e: Exception) {
            AppLogger.e("EventResultOverlay", "Failed to show error overlay", e)
        }
    }
    
    private fun createResultView(result: EventResult): View {
        // Create main container WITHOUT semi-transparent background
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.TRANSPARENT) // No background
            
            // Close overlay when clicking outside
            setOnClickListener {
                removeOverlay()
            }
        }
        
        // Create card container with rounded corners
        val cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // Add rounded corners
            val drawable = GradientDrawable()
            drawable.setColor(Color.WHITE)
            drawable.cornerRadius = 16f
            background = drawable
            elevation = 12f
            
            // Prevent clicks from propagating to parent (don't close on card click)
            setOnClickListener {
                // Do nothing - stops propagation
            }
        }
        
        // Event name title
        val titleText = TextView(context).apply {
            text = result.eventName
            textSize = 20f
            setTextColor(0xFF212121.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        cardContainer.addView(titleText)
        
        // Character name (if exists)
        if (result.characterName != null) {
            val characterText = TextView(context).apply {
                text = result.characterName
                textSize = 14f
                setTextColor(0xFF757575.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }
            cardContainer.addView(characterText)
        }
        
        // Event type badge (removed confidence display)
        val eventTypeBadge = TextView(context).apply {
            text = getEventTypeLabel(result.eventType)
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(getEventTypeColor(result.eventType))
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            // Add rounded corners to badge
            val badgeDrawable = GradientDrawable()
            badgeDrawable.setColor(getEventTypeColor(result.eventType))
            badgeDrawable.cornerRadius = 20f
            background = badgeDrawable
        }
        cardContainer.addView(eventTypeBadge)
        
        // Divider
        val divider = View(context).apply {
            setBackgroundColor(0xFFE0E0E0.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 8, 0, 16)
            }
        }
        cardContainer.addView(divider)
        
        // Options header
        val optionsHeader = TextView(context).apply {
            text = "Options:"
            textSize = 16f
            setTextColor(0xFF424242.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
        cardContainer.addView(optionsHeader)
        
        // Scrollable options container
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = 16
            }
        }
        
        val optionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // Sort options to display in correct order: Top Option → Middle Option → Bottom Option → Option 1-5 → Choice 1-10
        val sortedOptions = result.options.entries.sortedWith(compareBy { entry ->
            val key = entry.key.lowercase()
            when {
                key.contains("top option") -> 0
                key.contains("middle option") -> 1
                key.contains("bottom option") -> 2
                key.matches(Regex("option\\s*\\d+", RegexOption.IGNORE_CASE)) -> {
                    val num = key.filter { it.isDigit() }.toIntOrNull() ?: 99
                    100 + num // Options come after Top/Middle/Bottom
                }
                key.matches(Regex("choice\\s*\\d+", RegexOption.IGNORE_CASE)) -> {
                    val num = key.filter { it.isDigit() }.toIntOrNull() ?: 99
                    200 + num // Choices come after Options
                }
                else -> 300 // Other options at the end
            }
        })
        
        // Add each option in sorted order
        sortedOptions.forEach { (key, value) ->
            val optionCard = createOptionCard(key, value)
            optionsLayout.addView(optionCard)
        }
        
        if (result.options.isEmpty()) {
            val noOptionsText = TextView(context).apply {
                text = "No options available for this event"
                textSize = 14f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
            }
            optionsLayout.addView(noOptionsText)
        }
        
        scrollView.addView(optionsLayout)
        cardContainer.addView(scrollView)
        
        // Close button
        val closeButton = Button(context).apply {
            text = "Close"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Add rounded corners and color to button
            val buttonDrawable = GradientDrawable()
            buttonDrawable.setColor(0xFF2196F3.toInt())
            buttonDrawable.cornerRadius = 8f
            background = buttonDrawable
            setPadding(24, 19, 24, 19) // Reduced vertical padding from 16 to 12
            setOnClickListener {
                removeOverlay()
            }
        }
        cardContainer.addView(closeButton)
        
        mainContainer.addView(cardContainer)
        
        return mainContainer
    }
    
    private fun createErrorView(confidence: Double): View {
        // Create main container
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.TRANSPARENT)
            
            // Close overlay when clicking outside
            setOnClickListener {
                removeOverlay()
            }
        }
        
        // Create error card - centered content
        val cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER // Center all children
            
            val drawable = GradientDrawable()
            drawable.setColor(Color.WHITE)
            drawable.cornerRadius = 16f
            background = drawable
            elevation = 12f
            
            setOnClickListener {
                // Prevent clicks from closing overlay
            }
        }
        
        // Error icon (❌ emoji) - centered
        val errorIcon = TextView(context).apply {
            text = "❌"
            textSize = 64f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }
        cardContainer.addView(errorIcon)
        
        // Error title - centered, big, red
        val titleText = TextView(context).apply {
            text = "Event Not Recognized"
            textSize = 24f
            setTextColor(0xFFF44336.toInt()) // Red
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        cardContainer.addView(titleText)
        
        // Close button - full width
        val closeButton = Button(context).apply {
            text = "Close"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            val buttonDrawable = GradientDrawable()
            buttonDrawable.setColor(0xFFF44336.toInt()) // Red
            buttonDrawable.cornerRadius = 8f
            background = buttonDrawable
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                removeOverlay()
            }
        }
        cardContainer.addView(closeButton)
        
        mainContainer.addView(cardContainer)
        
        return mainContainer
    }
    
    private fun createOptionCard(optionKey: String, optionValue: String): View {
        val cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            
            // Add rounded corners and background
            val drawable = GradientDrawable()
            drawable.setColor(0xFFE3F2FD.toInt())
            drawable.cornerRadius = 8f
            drawable.setStroke(1, 0xFFBBDEFB.toInt())
            background = drawable
            elevation = 2f
        }
        
        // Option key/name
        val keyText = TextView(context).apply {
            text = optionKey
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 6
            }
        }
        cardContainer.addView(keyText)
        
        // Option value/effect
        val valueText = TextView(context).apply {
            text = cleanOptionText(optionValue)
            textSize = 13f
            setTextColor(0xFF424242.toInt())
            setLineSpacing(0f, 1.4f)
        }
        cardContainer.addView(valueText)
        
        return cardContainer
    }
    
    private fun cleanOptionText(text: String): String {
        return text
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\r\n", "\n")
            .trim()
    }
    
    private fun getEventTypeLabel(type: String): String {
        return when (type) {
            "support_card" -> "Support Card Event"
            "uma_event" -> "Uma Character Event"
            "career_event" -> "Career Event"
            "race" -> "Race"
            else -> "Unknown Event"
        }
    }
    
    private fun getEventTypeColor(type: String): Int {
        return when (type) {
            "support_card" -> 0xFF9C27B0.toInt() // Purple
            "uma_event" -> 0xFF2196F3.toInt()     // Blue
            "career_event" -> 0xFFFF9800.toInt()  // Orange
            "race" -> 0xFF4CAF50.toInt()          // Green
            else -> 0xFF9E9E9E.toInt()            // Grey
        }
    }
    
    private fun getConfidenceColor(confidence: Double): Int {
        return when {
            confidence >= 0.9 -> 0xFF4CAF50.toInt()  // Green
            confidence >= 0.75 -> 0xFF2196F3.toInt() // Blue
            confidence >= 0.6 -> 0xFFFF9800.toInt()  // Orange
            else -> 0xFFF44336.toInt()               // Red
        }
    }
    
    fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                AppLogger.d("EventResultOverlay", "Result overlay removed")
            } catch (e: Exception) {
                AppLogger.e("EventResultOverlay", "Error removing result overlay", e)
            }
        }
        overlayView = null
    }
}
