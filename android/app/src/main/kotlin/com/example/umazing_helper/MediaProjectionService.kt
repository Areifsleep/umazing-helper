// MediaProjectionService.kt (Fixed with proper timing)
package com.example.umazing_helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MediaProjectionService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "media_projection_channel"
        private const val CHANNEL_NAME = "Screen Capture Service"
        
        // Fix: Start service first, then set MediaProjection later
        fun startService(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun setMediaProjection(mediaProjection: MediaProjection) {
            MediaProjectionManager.getInstance().setMediaProjection(mediaProjection)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        AppLogger.d("MediaProjectionService", "onCreate - starting foreground service")
        
        try {
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            AppLogger.d("MediaProjectionService", "Foreground service started successfully")
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionService", "Error starting foreground service", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.d("MediaProjectionService", "onStartCommand")
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        AppLogger.d("MediaProjectionService", "onDestroy")
        
        try {
            MediaProjectionManager.getInstance().cleanup()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            AppLogger.e("MediaProjectionService", "Error in onDestroy", e)
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
                    description = "Screen capture service"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Ready")
            .setContentText("Service running in background")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()
    }
}