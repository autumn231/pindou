package com.pindou.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pindou.app.ui.screen.EditorScreen
import com.pindou.app.ui.screen.ExtractScreen
import com.pindou.app.ui.screen.ExportScreen
import com.pindou.app.ui.screen.HomeScreen

@Composable
fun PindouApp() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onNext = { nav.navigate("extract") }
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
