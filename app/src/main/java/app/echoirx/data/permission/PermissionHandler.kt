package app.echoirx.data.permission

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PermissionHandler(
    private val activity: ComponentActivity,
    private val permissionManager: PermissionManager
) {
    private val _currentPermissionIndex = MutableStateFlow(0)
    val currentPermissionIndex: StateFlow<Int> = _currentPermissionIndex.asStateFlow()

    private val _showPermissionBottomSheet = MutableStateFlow(false)
    val showPermissionBottomSheet: StateFlow<Boolean> = _showPermissionBottomSheet.asStateFlow()

    private val _currentPermissionType = MutableStateFlow<PermissionType?>(null)
    val currentPermissionType: StateFlow<PermissionType?> = _currentPermissionType.asStateFlow()

    private var requiredPermissions = emptyList<PermissionType>()

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var singlePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var settingsLauncher: ActivityResultLauncher<android.content.Intent>

    fun initialize() {
        permissionsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            activity.lifecycleScope.launch {
                checkAndShowNextPermission()
            }
        }

        singlePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            activity.lifecycleScope.launch {
                checkAndShowNextPermission()
            }
        }

        settingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            activity.lifecycleScope.launch {
                checkAndShowNextPermission()
            }
        }
    }

    fun startPermissionFlow() {
        checkAndShowNextPermission()
    }

    fun checkAndShowNextPermission() {
        requiredPermissions = permissionManager.getRequiredPermissionTypes()

        if (requiredPermissions.isEmpty()) {
            hidePermissionDialog()
            return
        }

        val currentIndex = _currentPermissionIndex.value

        if (currentIndex >= requiredPermissions.size) {
            hidePermissionDialog()
            return
        }

        _currentPermissionType.value = requiredPermissions[currentIndex]
        _showPermissionBottomSheet.value = true
    }

    fun skipCurrentPermission() {
        val currentPermission = _currentPermissionType.value
        if (currentPermission != null) {
            activity.lifecycleScope.launch {
                permissionManager.markPermissionSkipped(currentPermission)
                proceedToNextPermission()
            }
        } else {
            proceedToNextPermission()
        }
    }

    private fun proceedToNextPermission() {
        val currentIndex = _currentPermissionIndex.value
        val newIndex = currentIndex + 1

        if (newIndex >= requiredPermissions.size) {
            hidePermissionDialog()
        } else {
            _currentPermissionIndex.value = newIndex
            checkAndShowNextPermission()
        }
    }

    private fun hidePermissionDialog() {
        _currentPermissionIndex.value = 0
        _showPermissionBottomSheet.value = false
        _currentPermissionType.value = null
        requiredPermissions = emptyList()
    }

    fun handlePermissionRequest(permissionType: PermissionType) {
        when (permissionType) {
            PermissionType.STORAGE_LEGACY -> {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                } else {
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                permissionsLauncher.launch(permissions)
            }

            PermissionType.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    proceedToNextPermission()
                }
            }

            else -> {
                handleOpenSettings(permissionType)
            }
        }
    }

    fun handleOpenSettings(permissionType: PermissionType) {
        val intent = when (permissionType) {
            PermissionType.MANAGE_EXTERNAL_STORAGE -> {
                permissionManager.getAllFilesAccessIntent()
            }

            PermissionType.NOTIFICATION_LISTENER -> {
                permissionManager.getNotificationListenerSettingsIntent()
            }

            else -> null
        }

        intent?.let {
            settingsLauncher.launch(it)
        } ?: run {
            proceedToNextPermission()
        }
    }

    fun onResume() {
        if (!_showPermissionBottomSheet.value) {
            checkAndShowNextPermission()
        }
    }
}