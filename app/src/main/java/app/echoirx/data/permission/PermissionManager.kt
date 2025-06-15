package app.echoirx.data.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionDataStore: PermissionDataStore
) {
    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }

            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> true
        }
    }

    fun hasNotificationListenerPermission(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledListeners.contains(context.packageName)
    }

    fun getAllFilesAccessIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
            }
        } else {
            null
        }
    }

    fun getNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun getRequiredPermissionTypes(): List<PermissionType> = runBlocking {
        val types = mutableListOf<PermissionType>()

        if (!hasStoragePermission()) {
            val storageType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionType.MANAGE_EXTERNAL_STORAGE
            } else {
                PermissionType.STORAGE_LEGACY
            }

            if (!permissionDataStore.isPermissionSkipped(storageType)) {
                types.add(storageType)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission() &&
            !permissionDataStore.isPermissionSkipped(PermissionType.POST_NOTIFICATIONS)
        ) {
            types.add(PermissionType.POST_NOTIFICATIONS)
        }

        if (!hasNotificationListenerPermission() &&
            !permissionDataStore.isPermissionSkipped(PermissionType.NOTIFICATION_LISTENER)
        ) {
            types.add(PermissionType.NOTIFICATION_LISTENER)
        }

        return@runBlocking types
    }

    suspend fun markPermissionSkipped(permissionType: PermissionType) {
        permissionDataStore.markPermissionSkipped(permissionType)
    }
}