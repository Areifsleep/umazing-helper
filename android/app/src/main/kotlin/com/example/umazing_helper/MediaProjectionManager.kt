package com.example.umazing_helper

import android.media.projection.MediaProjection
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// MediaProjectionManager.kt (Fixed to handle refresh)
// MediaProjectionManager.kt (Simplified to use persistent service)
class MediaProjectionManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: MediaProjectionManager? = null
        
        fun getInstance(): MediaProjectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaProjectionManager().also { INSTANCE = it }
            }
        }
    }

    // Store permission data for service restarts
    private var mediaProjectionManager: android.media.projection.MediaProjectionManager? = null
    private var resultCode: Int = 0
    private var resultData: android.content.Intent? = null
    
    fun setPermissionData(manager: android.media.projection.MediaProjectionManager, resultCode: Int, data: android.content.Intent) {
        this.mediaProjectionManager = manager
        this.resultCode = resultCode
        this.resultData = data
        AppLogger.d("MediaProjectionManager", "Permission data stored for service persistence")
    }


    fun setMediaProjection(mediaProjection: MediaProjection) {
        // Store in the persistent service
        MediaProjectionService.setMediaProjection(mediaProjection)
        AppLogger.d("MediaProjectionManager", "MediaProjection forwarded to service")
    }
    
    
    fun getProjection(): MediaProjection? {
        val projection = MediaProjectionService.getMediaProjection()
        
        if (projection != null) {
            AppLogger.d("MediaProjectionManager", "Using persistent MediaProjection from service")
            return projection
        } else {
            AppLogger.e("MediaProjectionManager", "No MediaProjection available in service")
            return null
        }
    }

    fun isProjectionValid(): Boolean {
        val projection = MediaProjectionService.getMediaProjection()
        
        if (projection == null) {
            AppLogger.d("MediaProjectionManager", "❌ Validation failed: MediaProjection is null")
            return false
        }
        
        try {
            // Try to create a temporary VirtualDisplay to test validity
            // If MediaProjection is invalid, this will throw an exception
            val testDisplay = projection.createVirtualDisplay(
                "ValidationTest",
                1, 1, 1, // Minimal size for testing
                0, // No flags
                null, // No surface
                null, null
            )
            
            // If we got here, projection is valid
            testDisplay?.release() // Clean up test display
            AppLogger.d("MediaProjectionManager", "✅ MediaProjection validation successful")
            return true
            
        } catch (e: SecurityException) {
            AppLogger.e("MediaProjectionManager", "❌ MediaProjection invalid: SecurityException - ${e.message}")
            return false
        } catch (e: IllegalStateException) {
            AppLogger.e("MediaProjectionManager", "❌ MediaProjection invalid: IllegalStateException - ${e.message}")
            return false
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionManager", "❌ MediaProjection validation failed: ${e.message}")
            return false
        }
    }
    
    fun hasActiveProjection(): Boolean {
        return MediaProjectionService.isServiceRunning()
    }

    fun isTokenRevoked(): Boolean {
        return !MediaProjectionService.isTokenValid()
    }
    
    fun cleanup() {
        mediaProjectionManager = null
        resultCode = 0
        resultData = null
        AppLogger.d("MediaProjectionManager", "Manager cleaned up")
    }
}