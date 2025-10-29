package com.example.umazing_helper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class CharacterSelectionOverlay(
    private val context: Context,
    private val characterList: List<String> = emptyList()
) {
    
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private var overlayView: View? = null
    private var allCharacters: List<String> = characterList
    private var filteredCharacters: MutableList<String> = mutableListOf()
    private var listAdapter: BaseAdapter? = null
    private var selectedCharacter: String? = null
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("uma_character_filter", Context.MODE_PRIVATE)
    }
    
    fun showOverlay() {
        if (overlayView != null) {
            AppLogger.d("CharacterSelectionOverlay", "Overlay already shown, ignoring")
            return
        }
        
        try {
            AppLogger.d("CharacterSelectionOverlay", "Starting overlay creation...")
            
            // Check overlay permission first
            if (!PermissionManager.hasOverlayPermission(context)) {
                AppLogger.e("CharacterSelectionOverlay", "❌ Overlay permission not granted")
                showToast("❌ Overlay permission required")
                return
            }
            AppLogger.d("CharacterSelectionOverlay", "✅ Overlay permission granted")
            
            // Initialize characters if not provided in constructor
            if (allCharacters.isEmpty()) {
                AppLogger.d("CharacterSelectionOverlay", "Loading characters from Flutter...")
                loadCharactersFromFlutter()
                // loadCharactersFromFlutter will call createOverlayUI() when data arrives
                return
            } else {
                AppLogger.d("CharacterSelectionOverlay", "Using provided character list: ${allCharacters.size} characters")
                initializeFilteredCharacters()
            }
            
            // Create the overlay UI
            createOverlayUI()
            
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "❌ Failed to show overlay", e)
            showToast("❌ Error: ${e.message}")
            overlayView = null
        }
    }
    
    private fun createOverlayUI() {
        try {
            if (filteredCharacters.isEmpty()) {
                AppLogger.e("CharacterSelectionOverlay", "❌ No characters loaded")
                showToast("❌ Failed to load characters")
                return
            }
            AppLogger.d("CharacterSelectionOverlay", "✅ Characters loaded: ${filteredCharacters.size}")
            
            // Load saved selection
            selectedCharacter = prefs.getString("selected_character", null)
            AppLogger.d("CharacterSelectionOverlay", "Current selection: ${selectedCharacter ?: "All"}")
            
            AppLogger.d("CharacterSelectionOverlay", "Inflating layout...")
            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.character_selection_overlay, null)
            AppLogger.d("CharacterSelectionOverlay", "✅ Layout inflated")
            
            AppLogger.d("CharacterSelectionOverlay", "Setting up overlay view...")
            setupOverlayView()
            AppLogger.d("CharacterSelectionOverlay", "✅ Overlay view setup complete")
            
            AppLogger.d("CharacterSelectionOverlay", "Adding overlay to window...")
            addOverlayToWindow()
            AppLogger.d("CharacterSelectionOverlay", "✅ Overlay added to window")
            
            AppLogger.d("CharacterSelectionOverlay", "✅ Character selection overlay created successfully")
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "❌ Failed to create overlay UI", e)
            showToast("❌ Error: ${e.message}")
            overlayView = null
        }
    }
    
    private fun initializeFilteredCharacters() {
        filteredCharacters.clear()
        filteredCharacters.add("🌐 All Characters (No Filter)")
        filteredCharacters.addAll(allCharacters)
        AppLogger.d("CharacterSelectionOverlay", "✅ Initialized ${filteredCharacters.size} filtered characters")
    }
    
    private fun loadCharactersFromFlutter() {
        try {
            // Request character list from Flutter via method channel
            MainActivity.getMethodChannel()?.let { channel ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    channel.invokeMethod("getCharacterList", null, object : io.flutter.plugin.common.MethodChannel.Result {
                        override fun success(result: Any?) {
                            @Suppress("UNCHECKED_CAST")
                            val characters = result as? List<String>
                            if (characters != null && characters.isNotEmpty()) {
                                allCharacters = characters
                                initializeFilteredCharacters()
                                AppLogger.d("CharacterSelectionOverlay", "✅ Received ${allCharacters.size} characters from Flutter")
                                // Now create the overlay UI
                                createOverlayUI()
                            } else {
                                AppLogger.e("CharacterSelectionOverlay", "❌ No characters received from Flutter")
                                showToast("❌ No characters available")
                            }
                        }
                        
                        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                            AppLogger.e("CharacterSelectionOverlay", "Error getting characters from Flutter: $errorMessage")
                            showToast("❌ Failed to load characters: $errorMessage")
                        }
                        
                        override fun notImplemented() {
                            AppLogger.e("CharacterSelectionOverlay", "getCharacterList not implemented in Flutter")
                            showToast("❌ Character list not available")
                        }
                    })
                }
            } ?: run {
                AppLogger.e("CharacterSelectionOverlay", "Method channel not available")
                showToast("❌ Cannot communicate with Flutter")
            }
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "Failed to request characters from Flutter", e)
            showToast("❌ Error loading characters")
        }
    }
    
    private fun loadCharacters() {
        try {
            AppLogger.d("CharacterSelectionOverlay", "Loading characters from assets...")
            val inputStream = context.assets.open("data/uma_data.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()
            
            AppLogger.d("CharacterSelectionOverlay", "Parsing JSON data...")
            val jsonArray = JSONArray(jsonString)
            val characterSet = mutableSetOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                val character = jsonArray.getJSONObject(i)
                val umaName = character.getString("UmaName")
                characterSet.add(umaName)
            }
            
            allCharacters = characterSet.sorted()
            filteredCharacters.clear()
            filteredCharacters.add("🌐 All Characters (No Filter)")
            filteredCharacters.addAll(allCharacters)
            
            AppLogger.d("CharacterSelectionOverlay", "✅ Loaded ${allCharacters.size} unique characters")
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "❌ Failed to load characters", e)
            allCharacters = emptyList()
            filteredCharacters.clear()
            filteredCharacters.add("🌐 All Characters (No Filter)")
            showToast("❌ Failed to load character list")
        }
    }
    
    private fun setupOverlayView() {
        overlayView?.let { view ->
            val closeButton = view.findViewById<Button>(R.id.closeButton)
            val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
            val characterListView = view.findViewById<ListView>(R.id.characterListView)
            val customizeRegionButton = view.findViewById<Button>(R.id.customizeRegionButton)
            
            AppLogger.d("CharacterSelectionOverlay", "Setting up overlay view with ${filteredCharacters.size} characters")
            
            // Use a simple BaseAdapter instead of ArrayAdapter
            listAdapter = object : BaseAdapter() {
                override fun getCount(): Int = filteredCharacters.size
                
                override fun getItem(position: Int): String = filteredCharacters[position]
                
                override fun getItemId(position: Int): Long = position.toLong()
                
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = LayoutInflater.from(context).inflate(
                        R.layout.character_list_item,
                        parent,
                        false
                    )
                    
                    val characterName = filteredCharacters[position]
                    val nameTextView = view.findViewById<TextView>(R.id.characterNameTextView)
                    val checkmark = view.findViewById<TextView>(R.id.checkmarkTextView)
                    
                    nameTextView.text = characterName
                    
                    // Show checkmark if selected
                    val isSelected = if (position == 0) {
                        selectedCharacter == null
                    } else {
                        characterName == selectedCharacter
                    }
                    
                    checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE
                    
                    // Set click listener directly on the view
                    view.setOnClickListener {
                        handleCharacterSelection(position, characterName)
                    }
                    
                    AppLogger.d("CharacterSelectionOverlay", "Rendered item $position: $characterName")
                    
                    return view
                }
            }
            
            characterListView.adapter = listAdapter
            
            AppLogger.d("CharacterSelectionOverlay", "ListView adapter set successfully")
            
            // Close button
            closeButton.setOnClickListener {
                AppLogger.d("CharacterSelectionOverlay", "Close button clicked")
                removeOverlay()
            }
            
            // Customize Region button
            customizeRegionButton.setOnClickListener {
                AppLogger.d("CharacterSelectionOverlay", "Customize Region button clicked")
                showToast("🔧 Opening region customization...")
                openRegionCustomizer()
            }
            
            // Handle outside touches to close overlay
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_OUTSIDE -> {
                        AppLogger.d("CharacterSelectionOverlay", "Touch outside detected, closing overlay")
                        removeOverlay()
                        true
                    }
                    else -> false
                }
            }
            
            // Search functionality
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString()
                    AppLogger.d("CharacterSelectionOverlay", "Search query: '$query'")
                    filterCharacters(query)
                }
            })
            
            AppLogger.d("CharacterSelectionOverlay", "✅ Overlay view setup completed")
        }
    }
    
    private fun handleCharacterSelection(position: Int, character: String) {
        AppLogger.d("CharacterSelectionOverlay", "========================================")
        AppLogger.d("CharacterSelectionOverlay", "CHARACTER CLICKED!")
        AppLogger.d("CharacterSelectionOverlay", "Position: $position")
        AppLogger.d("CharacterSelectionOverlay", "Character: $character")
        AppLogger.d("CharacterSelectionOverlay", "========================================")
        
        if (position == 0 || character.startsWith("🌐")) {
            // Select "All Characters"
            selectedCharacter = null
            prefs.edit().remove("selected_character").apply()
            showToast("Filter removed - Searching all characters")
            AppLogger.d("CharacterSelectionOverlay", "✅ Filter removed")
        } else {
            // Select specific character
            selectedCharacter = character
            prefs.edit().putString("selected_character", character).apply()
            showToast("Filtering events for: $character")
            AppLogger.d("CharacterSelectionOverlay", "✅ Selected: $character")
        }
        
        // Update RecognitionDataService via method channel
        updateRecognitionService()
        
        // Update Flutter UI state
        updateFlutterUIState()
        
        // Close overlay after selection
        AppLogger.d("CharacterSelectionOverlay", "Closing overlay after selection")
        removeOverlay()
    }
    
    private fun filterCharacters(query: String) {
        filteredCharacters.clear()
        
        if (query.isEmpty()) {
            filteredCharacters.add("🌐 All Characters (No Filter)")
            filteredCharacters.addAll(allCharacters)
        } else {
            val lowerQuery = query.lowercase()
            filteredCharacters.add("🌐 All Characters (No Filter)")
            filteredCharacters.addAll(
                allCharacters.filter { it.lowercase().contains(lowerQuery) }
            )
        }
        
        listAdapter?.notifyDataSetChanged()
    }
    
    private fun updateRecognitionService() {
        // This will be called via method channel to update Flutter side
        try {
            MainActivity.getMethodChannel()?.let { channel ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    channel.invokeMethod("updateCharacterSelection", mapOf(
                        "character" to selectedCharacter
                    ))
                    AppLogger.d("CharacterSelectionOverlay", "✅ Sent character update to Flutter: ${selectedCharacter ?: "All"}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "Failed to update Flutter", e)
        }
    }
    
    private fun updateFlutterUIState() {
        // Notify Flutter to update its UI dropdown state
        try {
            MainActivity.getMethodChannel()?.let { channel ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    channel.invokeMethod("syncCharacterSelectionUI", mapOf(
                        "character" to selectedCharacter
                    ))
                    AppLogger.d("CharacterSelectionOverlay", "✅ Synced UI state with Flutter")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "Failed to sync UI with Flutter", e)
        }
    }
    
    private fun openRegionCustomizer() {
        try {
            AppLogger.d("CharacterSelectionOverlay", "Opening region customizer overlay...")
            
            // Close current overlay first
            removeOverlay()
            
            // Show native region customizer overlay (live overlay on top of game)
            val regionCustomizer = RegionCustomizerOverlay(context)
            regionCustomizer.show()
            
            AppLogger.d("CharacterSelectionOverlay", "✅ Region customizer overlay opened")
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "Failed to open region customizer", e)
            showToast("❌ Failed to open customizer")
        }
    }
    
    private fun addOverlayToWindow() {
        try {
            val layoutParams = createLayoutParams()
            windowManager.addView(overlayView, layoutParams)
            AppLogger.d("CharacterSelectionOverlay", "Overlay added to window manager")
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "Failed to add overlay to window", e)
            overlayView = null
            throw e
        }
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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }
    
    fun removeOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
                AppLogger.d("CharacterSelectionOverlay", "✅ Overlay removed")
            }
        } catch (e: Exception) {
            AppLogger.e("CharacterSelectionOverlay", "❌ Error removing overlay", e)
        }
    }
    
    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
