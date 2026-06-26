package com.finanzas.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finanzas.app.ui.screens.*
import com.finanzas.app.ui.viewmodels.AuthViewModel
import com.finanzas.app.ui.viewmodels.DashboardViewModel
import com.finanzas.app.ui.viewmodels.GastoViewModel
import io.github.jan.supabase.auth.status.SessionStatus

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Inicio",    Icons.Default.Home)
    object Agregar   : Screen("agregar",   "Agregar",   Icons.Default.Add)
    object Yape      : Screen("yape",      "Yape",      Icons.Default.PhoneIphone)
    object Historial : Screen("historial", "Historial", Icons.Default.List)
}

@Composable
fun FinanzasNavGraph() {
    val authVm: AuthViewModel = viewModel()
    val sessionStatus by authVm.sessionStatus.collectAsState()

    when (sessionStatus) {
        is SessionStatus.Authenticated -> MainApp(authVm = authVm)
        else                           -> LoginScreen(authVm = authVm)
    }
}

@Composable
private fun MainApp(authVm: AuthViewModel) {
    val navController = rememberNavController()
    val dashboardVm: DashboardViewModel = viewModel()
    val gastoVm: GastoViewModel         = viewModel()

    val items = listOf(Screen.Dashboard, Screen.Agregar, Screen.Yape, Screen.Historial)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(screen.icon, screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(vm = dashboardVm, onLogout = { authVm.logout() })
            }
            composable(Screen.Agregar.route) {
                AgregarGastoScreen(
                    vm      = gastoVm,
                    onSaved = {
                        dashboardVm.cargarDatos()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Yape.route) {
                YapeScreen(
                    vm      = gastoVm,
                    onSaved = {
                        dashboardVm.cargarDatos()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Historial.route) {
                HistorialScreen(vm = gastoVm)
            }
        }
    }
}
