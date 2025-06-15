package app.echoirx.data.permission

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.permissionDataStore: DataStore<Preferences> by preferencesDataStore(name = "permissions")

@Singleton
class PermissionDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val STORAGE_LEGACY_SKIPPED = booleanPreferencesKey("skipped_STORAGE_LEGACY")
        val MANAGE_EXTERNAL_STORAGE_SKIPPED =
            booleanPreferencesKey("skipped_MANAGE_EXTERNAL_STORAGE")
        val NOTIFICATION_LISTENER_SKIPPED = booleanPreferencesKey("skipped_NOTIFICATION_LISTENER")
        val POST_NOTIFICATIONS_SKIPPED = booleanPreferencesKey("skipped_POST_NOTIFICATIONS")
    }

    private fun getKeyForPermission(permissionType: PermissionType) = when (permissionType) {
        PermissionType.STORAGE_LEGACY -> PreferencesKeys.STORAGE_LEGACY_SKIPPED
        PermissionType.MANAGE_EXTERNAL_STORAGE -> PreferencesKeys.MANAGE_EXTERNAL_STORAGE_SKIPPED
        PermissionType.NOTIFICATION_LISTENER -> PreferencesKeys.NOTIFICATION_LISTENER_SKIPPED
        PermissionType.POST_NOTIFICATIONS -> PreferencesKeys.POST_NOTIFICATIONS_SKIPPED
    }

    suspend fun isPermissionSkipped(permissionType: PermissionType): Boolean {
        val key = getKeyForPermission(permissionType)
        return context.permissionDataStore.data.first()[key] ?: false
    }

    suspend fun markPermissionSkipped(permissionType: PermissionType) {
        val key = getKeyForPermission(permissionType)
        context.permissionDataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    suspend fun clearPermissionSkipped(permissionType: PermissionType) {
        val key = getKeyForPermission(permissionType)
        context.permissionDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    suspend fun clearAllSkippedPermissions() {
        context.permissionDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}