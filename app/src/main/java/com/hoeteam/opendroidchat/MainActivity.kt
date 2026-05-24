/*
OpenDroidChat
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoeteam.opendroidchat.ui.screens.*
import com.hoeteam.opendroidchat.ui.theme.OpenDroidChatTheme
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import com.hoeteam.opendroidchat.viewmodel.ThemeViewModel
import com.hoeteam.opendroidchat.viewmodel.ThemeViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 初始化主题 ViewModel
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(this@MainActivity))
            val isDarkTheme by themeViewModel.darkThemeEnabled.collectAsState()

            // 根据存储的主题设置来应用主题
            OpenDroidChatTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), 
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(themeViewModel)
                }
            }
        }
    }
}

// ------------------- Navigation Destinations -------------------

object Destinations {
    const val CHAT_SCREEN = "chat"
    const val MODEL_SETTINGS = "settings"
    const val MODEL_EDIT = "edit_model/{modelId}"
    const val SETTINGS_SCREEN = "app_settings"
    const val ABOUT_SCREEN = "about_app"
    const val LICENSE_SCREEN = "license_screen"
    const val ARG_MODEL_ID = "modelId"
}

@Composable
fun MainNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    val screens = listOf(
        Destinations.CHAT_SCREEN to "聊天",
        Destinations.MODEL_SETTINGS to "模型",
        Destinations.SETTINGS_SCREEN to "设置"
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 统一的根屏幕导航逻辑
    val navigateToRootScreen: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    @Composable
    fun NavigationComponent() {
        // 检查当前路由是否是底部导航栏的路由之一
        val rootRoutes = screens.map { it.first }.toSet()
        val shouldShowNavigation = currentDestination?.route in rootRoutes
        
        if (shouldShowNavigation) {
            if (isLandscape) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    header = {
                        Text(
                            "Chat", 
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    screens.forEach { (route, label) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationRailItem(
                            icon = {
                                val icon = when (route) {
                                    Destinations.CHAT_SCREEN -> Icons.AutoMirrored.Filled.Chat
                                    Destinations.MODEL_SETTINGS -> Icons.AutoMirrored.Filled.List
                                    Destinations.SETTINGS_SCREEN -> Icons.Filled.Settings
                                    else -> Icons.Filled.MoreVert
                                }
                                Icon(icon, contentDescription = label)
                            },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = { navigateToRootScreen(route) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            } else {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    screens.forEach { (route, label) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            icon = {
                                val icon = when (route) {
                                    Destinations.CHAT_SCREEN -> Icons.AutoMirrored.Filled.Chat
                                    Destinations.MODEL_SETTINGS -> Icons.AutoMirrored.Filled.List
                                    Destinations.SETTINGS_SCREEN -> Icons.Filled.Settings
                                    else -> Icons.Filled.MoreVert
                                }
                                Icon(icon, contentDescription = label)
                            },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            selected = selected,
                            onClick = { navigateToRootScreen(route) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        NavHost(
            navController = navController as NavHostController,
            startDestination = Destinations.CHAT_SCREEN,
            modifier = modifier.fillMaxSize()
        ) {
            composable(Destinations.CHAT_SCREEN) {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navigateToRootScreen(Destinations.SETTINGS_SCREEN) },
                    onNavigateToModelSettings = { navigateToRootScreen(Destinations.MODEL_SETTINGS) }
                )
            }

            composable(Destinations.MODEL_SETTINGS) {
                ModelSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEditModel = { modelId ->
                        navController.navigate("edit_model/$modelId")
                    }
                )
            }

            composable(
                Destinations.MODEL_EDIT,
                arguments = listOf(navArgument(Destinations.ARG_MODEL_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val modelId = backStackEntry.arguments?.getString(Destinations.ARG_MODEL_ID)
                val allModelsList by viewModel.allModels.collectAsState()
                val modelToEdit = remember(modelId, allModelsList) {
                    allModelsList.find { it.id == modelId }
                }

                ModelEditScreen(
                    viewModel = viewModel,
                    modelToEdit = modelToEdit,
                    onSave = { navController.popBackStack() }
                )
            }

            composable(Destinations.SETTINGS_SCREEN) {
                val isDarkTheme by themeViewModel.darkThemeEnabled.collectAsState()
                val allowOtherChannelsUpdate by themeViewModel.allowOtherChannelsUpdate.collectAsState()

                SettingsScreen(
                    currentDarkTheme = isDarkTheme,
                    onThemeToggle = { themeViewModel.setDarkTheme(it) },
                    allowOtherChannelsUpdate = allowOtherChannelsUpdate,
                    onAllowOtherChannelsUpdateChange = { themeViewModel.setAllowOtherChannelsUpdate(it) },
                    onNavigateToAbout = { navController.navigate(Destinations.ABOUT_SCREEN) }
                )
            }

            composable(Destinations.ABOUT_SCREEN) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToLicense = { navController.navigate(Destinations.LICENSE_SCREEN) }
                )
            }

            composable(Destinations.LICENSE_SCREEN) {
                LicenseScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationComponent()
            Content()
        }
    } else {
        Scaffold(
            bottomBar = { NavigationComponent() },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Content(modifier = Modifier.padding(innerPadding))
        }
    }
}
