package com.example.survey

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.survey.ui.HomeScreen             // FIXED: Added missing import
import com.example.survey.ui.SearchScreen          // FIXED: Added missing import
import com.example.survey.ui.SessionDetailScreen   // FIXED: Added missing import
import com.example.survey.ui.SessionSetupScreen    // FIXED: Added missing import
import com.example.survey.ui.CameraScreen          // FIXED: Added missing import
import com.example.survey.ui.MediaPreviewScreen    // FIXED: Added missing import
import com.example.survey.utils.PermissionHelper
import com.example.survey.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission helper
        permissionHelper = PermissionHelper(this)

        // Request permissions on app start
        if (!permissionHelper.hasAllPermissions()) {
            permissionHelper.requestMissingPermissions()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: CameraViewModel = viewModel()

                    // Initialize Room Database Repository
                    LaunchedEffect(Unit) {
                        viewModel.initializeRepository(this@MainActivity)
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            // FIXED: Direct access to mutableStateListOf
                            val sessions = viewModel.allSessions

                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToSearch = {
                                    navController.navigate("search")
                                },
                                onNavigateToNewSession = {
                                    navController.navigate("session_setup")
                                },
                                onSessionClick = { sessionName ->
                                    navController.navigate("session_detail/$sessionName")
                                }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onSessionClick = { sessionName ->
                                    navController.navigate("session_detail/$sessionName")
                                }
                            )
                        }

                        composable(
                            route = "session_detail/{sessionName}",
                            arguments = listOf(navArgument("sessionName") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val sessionName = backStackEntry.arguments?.getString("sessionName") ?: ""
                            SessionDetailScreen(
                                sessionName = sessionName,
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("session_setup") {
                            SessionSetupScreen(
                                viewModel = viewModel,
                                onNavigateToCamera = {
                                    // Check permissions before opening camera
                                    if (permissionHelper.hasAllPermissions()) {
                                        navController.navigate("camera")
                                    } else {
                                        permissionHelper.requestMissingPermissions()
                                    }
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("camera") {
                            CameraScreen(
                                viewModel = viewModel,
                                onNavigateToPreview = {
                                    navController.navigate("preview")
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("preview") {
                            MediaPreviewScreen(
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onSave = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isEmpty()) {
                // All permissions granted
            } else {
                // Some permissions denied
            }
        }
    }
}
