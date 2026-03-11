package com.deafcall.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deafcall.ui.screens.ActiveCallScreen
import com.deafcall.ui.screens.DiagnosticsScreen
import com.deafcall.ui.screens.HistoryScreen
import com.deafcall.ui.screens.HomeScreen
import com.deafcall.ui.screens.IncomingCallScreen
import com.deafcall.ui.screens.PermissionsScreen
import com.deafcall.ui.screens.SettingsScreen
import com.deafcall.ui.theme.DeafCallTheme
import com.deafcall.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

object Routes {
    const val HOME        = "home"
    const val INCOMING    = "incoming_call"
    const val ACTIVE      = "active_call"
    const val SETTINGS    = "settings"
    const val HISTORY     = "history"
    const val DIAGNOSTICS = "diagnostics"
    const val PERMISSIONS = "permissions"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val requestDefaultDialer = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Default dialer result: ${result.resultCode}")
        // Проверим стал ли дефолтным после ответа
        checkAndLogDialerStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState()
            val uiState  by viewModel.uiState.collectAsState()

            DeafCallTheme(highContrast = settings.isHighContrastEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.PERMISSIONS
                    ) {
                        composable(Routes.PERMISSIONS) {
                            PermissionsScreen(
                                onRequestDefaultDialer = { requestDefaultDialerRole() },
                                onPermissionsGranted = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSettings    = { navController.navigate(Routes.SETTINGS) },
                                onNavigateToHistory     = { navController.navigate(Routes.HISTORY) },
                                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                                onNavigateToIncoming    = { navController.navigate(Routes.INCOMING) },
                                onNavigateToActive      = { navController.navigate(Routes.ACTIVE) }
                            )
                        }
                        composable(Routes.INCOMING) {
                            IncomingCallScreen(
                                viewModel = viewModel,
                                onCallAccepted = {
                                    navController.navigate(Routes.ACTIVE) {
                                        popUpTo(Routes.INCOMING) { inclusive = true }
                                    }
                                },
                                onCallRejected = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.INCOMING) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.ACTIVE) {
                            ActiveCallScreen(
                                viewModel = viewModel,
                                onCallEnded = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.ACTIVE) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.DIAGNOSTICS) {
                            DiagnosticsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    /**
     * Запрос роли звонилки по умолчанию.
     * Для Android 10+ (API 29+) используем RoleManager.
     * Для старых версий — TelecomManager.ACTION_CHANGE_DEFAULT_DIALER.
     * На Realme/OPPO/Xiaomi оба метода показывают диалог выбора.
     */
    private fun requestDefaultDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            requestDefaultDialer.launch(intent)
                            Log.d(TAG, "Requesting ROLE_DIALER via RoleManager")
                        } else {
                            Log.w(TAG, "ROLE_DIALER not available on this device")
                            // Fallback для Realme где роль может быть недоступна
                            requestDefaultDialerLegacy()
                        }
                    } else {
                        Log.d(TAG, "Already default dialer")
                    }
                }
            } else {
                requestDefaultDialerLegacy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting default dialer: ${e.message}")
            requestDefaultDialerLegacy()
        }
    }

    private fun requestDefaultDialerLegacy() {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(
                TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                packageName
            )
        }
        try {
            requestDefaultDialer.launch(intent)
            Log.d(TAG, "Requesting default dialer via legacy TelecomManager")
        } catch (e: Exception) {
            Log.e(TAG, "Legacy dialer request failed: ${e.message}")
        }
    }

    private fun checkAndLogDialerStatus() {
        val telecomManager = getSystemService(TelecomManager::class.java)
        val isDefault = telecomManager?.defaultDialerPackage == packageName
        Log.d(TAG, "Is default dialer: $isDefault (package: $packageName)")
    }
}
