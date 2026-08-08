package com.pindou.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pindou.app.ui.screen.EditorScreen
import com.pindou.app.ui.screen.ExtractScreen
import com.pindou.app.ui.screen.ExportScreen
import com.pindou.app.ui.screen.HomeScreen
import com.pindou.app.ui.screen.ProjectListScreen

@Composable
fun PindouApp() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitConfirm by remember { mutableStateOf(false) }

    // 监听 ViewModel 的消息, 自动显示 Snackbar
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    // 返回处理: 仅在有未保存修改的非首页拦截, 弹出确认
    // 其他情况由 NavController / 系统处理 (popBackStack 或退出 App)
    BackHandler(enabled = vm.hasUnsavedChanges && nav.currentDestination?.route != "home") {
        showExitConfirm = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onNext = { nav.navigate("extract") },
                    onOpenProjects = { nav.navigate("projects") }
                )
            }
            composable("projects") {
                ProjectListScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onProjectLoaded = {
                        nav.popBackStack("home", inclusive = false)
                    }
                )
            }
            composable("extract") {
                ExtractScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate("editor") },
                    onSkip = {
                        vm.skipExtraction()
                        nav.navigate("editor")
                    }
                )
            }
            composable("editor") {
                EditorScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate("export") }
                )
            }
            composable("export") {
                ExportScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }

    // 退出确认对话框
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("未保存的修改") },
            text = { Text("当前有未保存的修改, 确定要离开吗?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    nav.popBackStack()
                }) { Text("离开") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("继续编辑") }
            }
        )
    }
}
