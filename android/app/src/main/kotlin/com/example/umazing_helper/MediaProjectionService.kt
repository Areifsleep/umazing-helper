// MediaProjectionService.kt (Complete fixed version)
package com.example.umazing_helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class MediaProjectionService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "media_projection_channel"
        private const val CHANNEL_NAME = "Screen Capture Service"
        
        // Store MediaProjection in the service itself (persistent)
        private var serviceMediaProjection: MediaProjection? = null
        private var serviceInstance: MediaProjectionService? = null
        private var projectionCallback: MediaProjection.Callback? = null
        private var isTokenValid = false
        
        fun startService(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            AppLogger.d("MediaProjectionService", "Service start requested")
        }
        
        fun setMediaProjection(mediaProjection: MediaProjection) {
            // Remove old callback if exists
            projectionCallback?.let { callback ->
                serviceMediaProjection?.unregisterCallback(callback)
            }
            
            serviceMediaProjection = mediaProjection
            isTokenValid = true
            
            // Create and store the callback
            val newCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    AppLogger.w("MediaProjectionService", "🚨 MediaProjection token REVOKED by user or system!")
                    
                    // Mark token as invalid
                    isTokenValid = false
                    
                    // Clean up the revoked projection
                    serviceMediaProjection?.unregisterCallback(this)
                    serviceMediaProjection = null
                    projectionCallback = null
                    
                    // Update notification to show inactive state
                    serviceInstance?.updateNotificationToInactive()
                    
                    // Broadcast revocation to other components
                    serviceInstance?.broadcastTokenRevocation()
                    
                    AppLogger.d("MediaProjectionService", "🧹 Cleaned up revoked MediaProjection")
                }
            }
            
            // Store the callback reference and register it
            projectionCallback = newCallback
            serviceMediaProjection?.registerCallback(newCallback, Handler(Looper.getMainLooper()))
            
            // Update notification to active state
            serviceInstance?.updateNotificationToActive()
            
            AppLogger.d("MediaProjectionService", "✅ MediaProjection stored with callback registered")
        }
        
        fun getMediaProjection(): MediaProjection? {
            val isAvailable = serviceMediaProjection != null && isTokenValid
            AppLogger.d("MediaProjectionService", "Providing persistent MediaProjection: $isAvailable")
            return if (isTokenValid) serviceMediaProjection else null
        }
        
        fun isServiceRunning(): Boolean {
            val isRunning = serviceInstance != null && serviceMediaProjection != null && isTokenValid
            AppLogger.d("MediaProjectionService", "Service running status: $isRunning")
            return isRunning
        }
        
        fun isTokenValid(): Boolean {
            return isTokenValid && serviceMediaProjection != null
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            context.stopService(intent)
            AppLogger.d("MediaProjectionService", "Service stop requested")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        AppLogger.d("MediaProjectionService", "Persistent service created and running in foreground")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d("MediaProjectionService", "Service command received")
        
        // Ensure we stay in foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("MediaProjectionService", "Service being destroyed")
        
        // Clean up callback and MediaProjection
        projectionCallback?.let { callback ->
            serviceMediaProjection?.unregisterCallback(callback)
        }
        serviceMediaProjection?.stop()
        serviceMediaProjection = null
        projectionCallback = null
        serviceInstance = null
        isTokenValid = false
        
        // Stop foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        AppLogger.d("MediaProjectionService", "Service destroyed and MediaProjection cleaned up")
    }
    
    // Update notification when MediaProjection is active
    private fun updateNotificationToActive() {
        try {
            val activeNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture Active")
                .setContentText("Ready to capture from overlay")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false)
                .build()
                
            startForeground(NOTIFICATION_ID, activeNotification)
            AppLogger.d("MediaProjectionService", "📱 Updated notification to active state")
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionService", "Failed to update notification to active", e)
        }
    }
    
    // Update notification when token is revoked
    fun updateNotificationToInactive() {
        try {
            val inactiveNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture Inactive")
                .setContentText("Permission revoked - restart app to capture again")
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false)
                .setAutoCancel(true)
                .build()
                
            startForeground(NOTIFICATION_ID, inactiveNotification)
            AppLogger.d("MediaProjectionService", "📱 Updated notification to inactive state")
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionService", "Failed to update notification to inactive", e)
        }
    }
    
    // Broadcast token revocation to other components
    fun broadcastTokenRevocation() {
        try {
            val revocationIntent = Intent("com.example.umazing_helper.TOKEN_REVOKED")
            sendBroadcast(revocationIntent)
            AppLogger.d("MediaProjectionService", "📡 Token revocation broadcast sent")
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionService", "Failed to broadcast token revocation", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps screen capture active in background"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                
                notificationManager.createNotificationChannel(channel)
                AppLogger.d("MediaProjectionService", "Notification channel created")
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Ready")
            .setContentText("Waiting for MediaProjection...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()
    }
}