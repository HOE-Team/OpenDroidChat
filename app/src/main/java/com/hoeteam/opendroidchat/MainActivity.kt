package com.hoeteam.opendroidchat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hoeteam.opendroidchat.data.SettingsRepository
import com.hoeteam.opendroidchat.network.LlmApiService
import com.hoeteam.opendroidchat.ui.ChatScreen
import com.hoeteam.opendroidchat.ui.ModelEditScreen
import com.hoeteam.opendroidchat.ui.ModelSettingsScreen
// 请根据您的主题文件 (ui.theme/Theme.kt) 中定义的函数名修改此处的引用
import com.hoeteam.opendroidchat.ui.theme.OpenDroidChatTheme
import com.hoeteam.opendroidchat.viewmodel.ChatViewModel
import com.hoeteam.opendroidchat.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 请将 OpenDroidChatTheme 替换为您主题文件中的实际函数名
            OpenDroidChatTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
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
    const val ARG_MODEL_ID = "modelId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    NavHost(navController, startDestination = Destinations.CHAT_SCREEN) {

        // 1. 聊天界面
        composable(Destinations.CHAT_SCREEN) {
            ChatScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Destinations.MODEL_SETTINGS) }
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

        // 3. 模型编辑/新增界面 (包含 Unresolved reference 的修复)
        composable(
            Destinations.MODEL_EDIT,
            arguments = listOf(navArgument(Destinations.ARG_MODEL_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getString(Destinations.ARG_MODEL_ID)

            // 修复后的代码：
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
    }
}

