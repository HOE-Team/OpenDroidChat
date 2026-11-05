/*
OpenDroidChat
Copyright (C) 2025-2026 HOE Team. All rights reserved.
The source code is open-sourced under the MIT License.
*/
package com.hoeteam.opendroidchat

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.platform.LocalConfiguration
import com.hoeteam.opendroidchat.data.SettingsRepository
import com.hoeteam.opendroidchat.data.UpdateManager
import com.hoeteam.opendroidchat.data.dataStore
import com.hoeteam.opendroidchat.network.LlmApiService
import com.hoeteam.opendroidchat.network.UpdateCheckResult
import com.hoeteam.opendroidchat.network.VersionType
import com.hoeteam.opendroidchat.ui.components.ConversationDrawerContent
import com.hoeteam.opendroidchat.ui.screens.ChatScreen
import com.hoeteam.opendroidchat.ui.screens.ModelEditScreen
import com.hoeteam.opendroidchat.ui.screens.ModelSettingsScreen
import com.hoeteam.opendroidchat.ui.screens.SettingsScreen
import com.hoeteam.opendroidchat.ui.screens.McpScreen
import com.hoeteam.opendroidchat.ui.screens.McpServerEditScreen
import com.hoeteam.opendroidchat.ui.screens.AboutScreen
import com.hoeteam.opendroidchat.ui.screens.LegalScreen
import com.hoeteam.opendroidchat.ui.screens.LicenseScreen
import com.hoeteam.opendroidchat.ui.theme.OpenDroidChatTheme
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory
import com.hoeteam.opendroidchat.viewmodel.ThemeViewModel
import com.hoeteam.opendroidchat.viewmodel.ThemeViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在 setContent 之前同步应用已保存的语言设置
        applySavedLanguageSync()

        // 预热 DataStore：在 Activity 创建时触发 DataStore 文件加载，
        // 确保第一个 Flow 订阅时数据已在内存中，避免冷启动 IO 延迟
        prewarmDataStore()

        setContent {
            // 初始化主题 ViewModel
            val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(this@MainActivity))
            val isDarkTheme by themeViewModel.darkThemeEnabled.collectAsState()

            // 更新检查状态
            var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }
            val updateManager = remember { UpdateManager(this@MainActivity) }

            // 根据存储的主题设置来应用主题
            OpenDroidChatTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation(themeViewModel)

                    // 自动更新检查对话框
                    if (showUpdateDialog && updateCheckResult?.hasUpdate == true) {
                        val result = updateCheckResult!!
                        val versionTag = result.latestVersion ?: ""

                        AlertDialog(
                            onDismissRequest = { showUpdateDialog = false },
                            shape = RoundedCornerShape(24.dp),
                            title = {
                                Text(
                                    text = stringResource(R.string.update_dialog_title),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            },
                            text = {
                                Column {
                                    if (versionTag.isNotEmpty()) {
                                        Text(
                                            text = stringResource(R.string.update_dialog_new_version, versionTag),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.update_dialog_download_question),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = {
                                        updateManager.openDownloadPage(versionTag)
                                        showUpdateDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.update_dialog_download), fontWeight = FontWeight.Bold)
                                }
                            },
                            confirmButton = {
                                OutlinedButton(
                                    onClick = { showUpdateDialog = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.update_dialog_cancel))
                                }
                            },
                        )
                    }
                }
            }

            // 启动时异步检查更新（使用 LaunchedEffect 在组合内安全启动协程）
            LaunchedEffect(Unit) {
                try {
                    // 先读取自动更新检查开关（使用 first() 获取一次值）
                    val settingsRepo = SettingsRepository(this@MainActivity)
                    val autoCheckEnabled = withContext(Dispatchers.IO) {
                        settingsRepo.autoUpdateCheckFlow.first()
                    }
                    if (!autoCheckEnabled) return@LaunchedEffect

                    val currentType = updateManager.getCurrentVersionType()
                    if (currentType == VersionType.NIGHTLY) return@LaunchedEffect

                    val result = withContext(Dispatchers.IO) {
                        updateManager.checkForUpdates(currentType)
                    }
                    if (result.hasUpdate && result.error == null) {
                        updateCheckResult = result
                        showUpdateDialog = true
                    }
                } catch (_: Exception) {
                    // 更新检查失败不影响正常使用
                }
            }
        }
    }

    /**
     * 在 setContent 之前同步应用已保存的语言设置。
     * 使用 runBlocking 确保语言在 UI 组合之前生效。
     */
    private fun applySavedLanguageSync() {
        try {
            val languageCode = runBlocking(Dispatchers.IO) {
                val settingsRepo = SettingsRepository(this@MainActivity)
                settingsRepo.appLanguageFlow.first()
            }
            applyLanguage(languageCode)
        } catch (_: Exception) {
            // 语言应用失败不影响主流程
        }
    }

    /**
     * 从 DataStore 读取已保存的语言设置并应用到 AppCompatDelegate。
     */
    private fun applySavedLanguage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val settingsRepo = SettingsRepository(this@MainActivity)
                val languageCode = settingsRepo.appLanguageFlow.first()
                applyLanguage(languageCode)
            } catch (_: Exception) {
                // 语言应用失败不影响主流程
            }
        }
    }

    /**
     * 根据语言代码设置 AppCompatDelegate 的区域设置。
     */
    private fun applyLanguage(languageCode: String) {
        val localeList = when (languageCode) {
            "zh-CN" -> LocaleListCompat.forLanguageTags("zh-CN")
            "zh-TW" -> LocaleListCompat.forLanguageTags("zh-TW")
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList() // 跟随系统
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * 预加载 DataStore 数据。
     * 通过提前触发 dataStore.data 的首次读取，让 DataStore 在后台线程
     * 完成文件加载和解析，减少后续 UI 首次订阅时的等待时间。
     */
    private fun prewarmDataStore() {
        // 使用 lifecycleScope 在后台协程触发 DataStore 读取，提前将数据缓存到内存
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 只读取一次 DataStore 数据，触发 DataStore 完成反序列化
                // 使用 kotlinx.coroutines.flow.first() 来只取第一个发射的值
                applicationContext.dataStore.data.collect { 
                    // 读取到一次数据后就停止，预热完成
                    return@collect
                }
            } catch (_: Exception) {
                // 预读取失败不影响主流程，UI 层会正常读取
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
    const val LEGAL_SCREEN = "legal"
    const val MCP_SCREEN = "mcp_servers"
    const val MCP_EDIT = "edit_mcp/{serverId}"
    const val ARG_MCP_SERVER_ID = "serverId"
}

@Composable
fun MainNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    // 历史对话抽屉：与聊天页共享同一个 ViewModel，可直接切换会话
    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val isGenerating by viewModel.isLoading.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val screens = listOf(
        Destinations.CHAT_SCREEN to stringResource(R.string.nav_chat),
        Destinations.MODEL_SETTINGS to stringResource(R.string.nav_models),
        Destinations.MCP_SCREEN to stringResource(R.string.nav_mcp),
        Destinations.SETTINGS_SCREEN to stringResource(R.string.nav_settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 每次展开抽屉时刷新历史列表，保证与磁盘上的数据一致
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) viewModel.refreshConversations()
    }

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

    @Composable
    fun NavigationComponent() {
        // 检查当前路由是否是底部导航栏的路由之一 (只在根屏幕显示导航)
        val rootRoutes = screens.map { it.first }.toSet()
        val shouldShowNavigation = currentDestination?.route in rootRoutes
        
        if (shouldShowNavigation) {
            if (isLandscape) {
                // 横屏时显示 NavigationRail
                NavigationRail {
                    screens.forEach { (route, label) ->
                        NavigationRailItem(
                            icon = {
                                when (route) {
                                    Destinations.CHAT_SCREEN -> Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = label)
                                    Destinations.MODEL_SETTINGS -> Icon(Icons.Filled.AutoAwesome, contentDescription = label)
                                    Destinations.MCP_SCREEN -> Icon(Icons.Filled.Storage, contentDescription = label)
                                    Destinations.SETTINGS_SCREEN -> Icon(Icons.Filled.Settings, contentDescription = label)
                                    else -> Icon(Icons.Filled.MoreVert, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = { navigateToRootScreen(route) }
                        )
                    }
                }
            } else {
                // 竖屏时显示 NavigationBar
                NavigationBar {
                    screens.forEach { (route, label) ->
                        NavigationBarItem(
                            icon = {
                                when (route) {
                                    Destinations.CHAT_SCREEN -> Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = label)
                                    Destinations.MODEL_SETTINGS -> Icon(Icons.Filled.AutoAwesome, contentDescription = label)
                                    Destinations.MCP_SCREEN -> Icon(Icons.Filled.Storage, contentDescription = label)
                                    Destinations.SETTINGS_SCREEN -> Icon(Icons.Filled.Settings, contentDescription = label)
                                    else -> Icon(Icons.Filled.MoreVert, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = { navigateToRootScreen(route) }
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
            // 1. 聊天界面
            composable(Destinations.CHAT_SCREEN) {
                ChatScreen(
                    viewModel = viewModel,
                    // 从聊天页顶栏展开历史对话抽屉
                    onOpenHistoryDrawer = { scope.launch { drawerState.open() } },
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

            // 5. 关于程序界面
            composable(Destinations.ABOUT_SCREEN) {
                AboutScreen(
                    onBack = { navController.popBackStack() }, // 返回到 SettingsScreen
                    onNavigateToLicense = { navController.navigate(Destinations.LICENSE_SCREEN) }, // 导航到开源许可界面
                    onNavigateToLegal = { navController.navigate(Destinations.LEGAL_SCREEN) } // 导航到法律声明页面
                )
            }

            // 6. 开源许可界面
            composable(Destinations.LICENSE_SCREEN) {
                LicenseScreen(
                    onBack = { navController.popBackStack() } // 返回到 AboutScreen
                )
            }

            // 7. 法律性和免责声明页面
            composable(Destinations.LEGAL_SCREEN) {
                LegalScreen(
                    onBack = { navController.popBackStack() } // 返回到 AboutScreen
                )
            }

            // 8. MCP 服务器管理页面
            composable(Destinations.MCP_SCREEN) {
                McpScreen(
                    viewModel = viewModel,
                    onNavigateToEditServer = { serverId ->
                        navController.navigate("edit_mcp/$serverId")
                    }
                )
            }

            // 9. MCP 服务器编辑/新增页面
            composable(
                Destinations.MCP_EDIT,
                arguments = listOf(navArgument(Destinations.ARG_MCP_SERVER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val serverId = backStackEntry.arguments?.getString(Destinations.ARG_MCP_SERVER_ID)

                val mcpServerList by viewModel.mcpServers.collectAsState()

                val serverToEdit = remember(serverId) {
                    mcpServerList.find { it.id == serverId }
                }

                McpServerEditScreen(
                    viewModel = viewModel,
                    serverToEdit = serverToEdit,
                    onSave = { navController.popBackStack() }
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // 只在聊天页允许从屏幕边缘滑出抽屉，避免干扰其它页面的手势
        gesturesEnabled = currentDestination?.route == Destinations.CHAT_SCREEN,
        drawerContent = {
            ModalDrawerSheet {
                ConversationDrawerContent(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    isGenerating = isGenerating,
                    onNewConversation = {
                        if (!isGenerating) {
                            viewModel.clearChat()
                            scope.launch { drawerState.close() }
                        }
                    },
                    onOpenConversation = { summary ->
                        if (!isGenerating) {
                            viewModel.openConversation(summary.id)
                            scope.launch { drawerState.close() }
                        }
                    },
                    onDeleteConversation = { summary -> viewModel.deleteConversation(summary.id) },
                    onClearAllConversations = { viewModel.deleteAllConversations() }
                )
            }
        }
    ) {
        if (isLandscape) {
            // 横屏布局：左侧 NavigationRail，右侧内容
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationComponent()
                Content()
            }
        } else {
            // 竖屏布局：底部 NavigationBar，上方内容
            Scaffold(
                bottomBar = { NavigationComponent() }
            ) { innerPadding ->
                Content(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
