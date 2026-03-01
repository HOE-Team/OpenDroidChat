/*
OpenDroidChat
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import com.hoeteam.opendroidchat.data.SettingsRepository
import com.hoeteam.opendroidchat.network.LlmApiService
import com.hoeteam.opendroidchat.ui.screens.ChatScreen
import com.hoeteam.opendroidchat.ui.screens.ModelEditScreen
import com.hoeteam.opendroidchat.ui.screens.ModelSettingsScreen
import com.hoeteam.opendroidchat.ui.screens.SettingsScreen
import com.hoeteam.opendroidchat.ui.screens.AboutScreen
import com.hoeteam.opendroidchat.ui.screens.LicenseScreen
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

    // 统一的根屏幕导航逻辑
    val navigateToRootScreen: (String) -> Unit = { route ->
        navController.navigate(route) {
            // Pop up to the start destination of the graph to avoid building up a large stack
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when re-selecting the same item
            launchSingleTop = true
            // Restore state when re-selecting a previously selected item
            restoreState = true
        }
    }

    val bottomBar: @Composable (NavController) -> Unit = { navController ->
        // 检查当前路由是否是底部导航栏的路由之一 (只在根屏幕显示 BNB)
        val rootRoutes = screens.map { it.first }.toSet()
        val shouldShowBottomBar = currentDestination?.route in rootRoutes
        if (shouldShowBottomBar) {
            NavigationBar {
                screens.forEach { (route, label) ->
                    NavigationBarItem(
                        icon = {
                            when (route) {
                                Destinations.CHAT_SCREEN -> Icon(Icons.Filled.Chat, contentDescription = label) // 统一 Chat 图标
                                Destinations.MODEL_SETTINGS -> Icon(Icons.Filled.List, contentDescription = label)
                                Destinations.SETTINGS_SCREEN -> Icon(Icons.Filled.Settings, contentDescription = label)
                                else -> Icon(Icons.Filled.MoreVert, contentDescription = label) // Fallback icon
                            }
                        },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = { navigateToRootScreen(route) } // 使用统一导航逻辑
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = { bottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController as NavHostController,
            startDestination = Destinations.CHAT_SCREEN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. 聊天界面
            composable(Destinations.CHAT_SCREEN) {
                ChatScreen(
                    viewModel = viewModel,
                    // 允许从 ChatScreen 内部（如配置为空时）导航到通用设置
                    onNavigateToSettings = { navigateToRootScreen(Destinations.SETTINGS_SCREEN) },
                    // FIX: 新增导航到模型列表的回调 (用于 EmptyConfigScreen 按钮)
                    onNavigateToModelSettings = { navigateToRootScreen(Destinations.MODEL_SETTINGS) }
                )
            }

            // 2. 模型管理列表
            composable(Destinations.MODEL_SETTINGS) {
                ModelSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEditModel = { modelId ->
                        navController.navigate("edit_model/$modelId")
                    }
                )
            }

            // 3. 模型编辑/新增界面
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

                val modelToEdit = remember(modelId) {
                    allModelsList.find { it.id == modelId }
                }

                ModelEditScreen(
                    viewModel = viewModel,
                    modelToEdit = modelToEdit, // 直接传递找到的模型对象
                    onSave = { navController.popBackStack() }
                )
            }

            // 4. 应用程序设置界面
            composable(Destinations.SETTINGS_SCREEN) {
                val isDarkTheme by themeViewModel.darkThemeEnabled.collectAsState()

                SettingsScreen(
                    // 传入主题状态和切换回调
                    currentDarkTheme = isDarkTheme,
                    onThemeToggle = { themeViewModel.setDarkTheme(it) },
                    onNavigateToAbout = { navController.navigate(Destinations.ABOUT_SCREEN) }, // 导航到 About
                    onNavigateToChat = { navigateToRootScreen(Destinations.CHAT_SCREEN) } // 导航到 Chat (通过 BNB 机制)
                )
            }

            // 5. 关于程序界面（修改：添加导航到开源许可的回调）
            composable(Destinations.ABOUT_SCREEN) {
                AboutScreen(
                    onBack = { navController.popBackStack() }, // 返回到 SettingsScreen
                    onNavigateToLicense = { navController.navigate(Destinations.LICENSE_SCREEN) } // 新增：导航到开源许可界面
                )
            }

            // 6. 新增：开源许可界面
            composable(Destinations.LICENSE_SCREEN) {
                LicenseScreen(
                    onBack = { navController.popBackStack() } // 返回到 AboutScreen
                )
            }
        }
    }
}