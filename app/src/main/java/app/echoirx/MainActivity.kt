package app.echoirx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import app.echoirx.data.permission.PermissionHandler
import app.echoirx.data.permission.PermissionManager
import app.echoirx.presentation.components.permission.PermissionBottomSheet
import app.echoirx.presentation.screens.MainScreen
import app.echoirx.presentation.theme.EchoirTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager

    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionHandler = PermissionHandler(this, permissionManager)
        permissionHandler.initialize()

        setContent {
            val navController = rememberNavController()
            val showPermissionBottomSheet by permissionHandler.showPermissionBottomSheet.collectAsState()
            val currentPermissionType by permissionHandler.currentPermissionType.collectAsState()

            EchoirTheme {
                MainScreen(navController = navController)

                if (showPermissionBottomSheet && currentPermissionType != null) {
                    PermissionBottomSheet(
                        permissionType = currentPermissionType!!,
                        onRequestPermission = {
                            permissionHandler.handlePermissionRequest(currentPermissionType!!)
                        },
                        onOpenSettings = {
                            permissionHandler.handleOpenSettings(currentPermissionType!!)
                        },
                        onDismiss = {
                            permissionHandler.skipCurrentPermission()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionHandler.startPermissionFlow()
    }

    override fun onResume() {
        super.onResume()
        permissionHandler.onResume()
    }
}