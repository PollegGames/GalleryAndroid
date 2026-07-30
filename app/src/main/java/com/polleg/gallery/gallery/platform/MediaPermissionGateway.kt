package com.polleg.gallery.gallery.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class MediaPermissionStatus(
    val hasAccess: Boolean,
    val isLimited: Boolean,
)

class MediaPermissionGateway(
    private val context: Context,
) {
    fun currentStatus(): MediaPermissionStatus = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            val images = isGranted(Manifest.permission.READ_MEDIA_IMAGES)
            val videos = isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            val selected = isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            val hasAccess = images || videos || selected
            MediaPermissionStatus(
                hasAccess = hasAccess,
                isLimited = hasAccess && !(images && videos),
            )
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val images = isGranted(Manifest.permission.READ_MEDIA_IMAGES)
            val videos = isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            val hasAccess = images || videos
            MediaPermissionStatus(
                hasAccess = hasAccess,
                isLimited = hasAccess && !(images && videos),
            )
        }

        else -> {
            val granted = isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            MediaPermissionStatus(hasAccess = granted, isLimited = false)
        }
    }

    fun permissionsToRequest(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
