package com.example.umazing_helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    private val mediaProjectionManager: MediaProjectionManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        } else {
            null
        }
    }

    fun isScreenCaptureSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun createScreenCaptureIntent(): Intent {
        if (!isScreenCaptureSupported()) {
            throw UnsupportedOperationException("Screen capture requires API level 21+")
        }
        return mediaProjectionManager!!.createScreenCaptureIntent()
    }

    fun createMediaProjection(resultCode: Int, data: Intent): MediaProjection {
        if (!isScreenCaptureSupported()) {
            throw UnsupportedOperationException("Screen capture requires API level 21+")
        }
        return mediaProjectionManager!!.getMediaProjection(resultCode, data)
    }

    fun hasRequiredPermissions(): Boolean {
        val hasOverlay = hasOverlayPermission()
        val hasNotification = hasNotificationPermission()
        return hasOverlay && hasNotification
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
}