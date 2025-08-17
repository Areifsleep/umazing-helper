package com.example.umazing_helper

import android.media.projection.MediaProjection
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

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

    private var mediaProjection: MediaProjection? = null
    private var referenceCount = 0
    private val lock = ReentrantReadWriteLock()

    fun setMediaProjection(projection: MediaProjection) {
        lock.write {
            // Stop existing projection if any
            mediaProjection?.stop()
            mediaProjection = projection
            referenceCount = 0
            AppLogger.d("MediaProjectionManager", "MediaProjection set")
        }
    }

    fun acquireProjection(): MediaProjection? {
        return lock.write {
            mediaProjection?.let {
                referenceCount++
                AppLogger.d("MediaProjectionManager", "Projection acquired, ref count: $referenceCount")
                it
            }
        }
    }

    fun releaseProjection() {
        lock.write {
            if (referenceCount > 0) {
                referenceCount--
                AppLogger.d("MediaProjectionManager", "Projection released, ref count: $referenceCount")
                
                if (referenceCount == 0) {
                    mediaProjection?.stop()
                    mediaProjection = null
                    AppLogger.d("MediaProjectionManager", "MediaProjection stopped and cleared")
                }
            }
        }
    }

    fun getProjection(): MediaProjection? {
        return lock.read { mediaProjection }
    }

    fun hasActiveProjection(): Boolean {
        return lock.read { mediaProjection != null }
    }

    fun cleanup() {
        lock.write {
            mediaProjection?.stop()
            mediaProjection = null
            referenceCount = 0
            AppLogger.d("MediaProjectionManager", "Cleanup completed")
        }
    }
}