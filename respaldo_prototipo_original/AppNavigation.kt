package com.fabiancg.bottomappbar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fabiancg.bottomappbar.ui.screens.CreateTicketScreen
import com.fabiancg.bottomappbar.ui.screens.HomeScreen
import com.fabiancg.bottomappbar.ui.screens.LoginScreen
import com.fabiancg.bottomappbar.ui.screens.TicketDetailScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CREATE = "create"
    const val DETAIL = "detail/{ticketId}"
    fun detail(id: Int) = "detail/$id"
}

@Composable
fun AppNavigation(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val user = vm.currentUser
    val route = backStack?.destination?.route


    LaunchedEffect(user, route) {
        if (user == null && route != null && route != Routes.LOGIN) {
            nav.navigate(Routes.LOGIN) {
                popUpTo(nav.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = { email, password -> vm.login(email, password) },
                onSuccess = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenTicket = { id -> nav.navigate(Routes.detail(id)) },
                onCreate = { nav.navigate(Routes.CREATE) },
                onLogout = { vm.logout() }
            )
        }

        composable(Routes.CREATE) {
            CreateTicketScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onCreated = { nav.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("ticketId") ?: -1
            TicketDetailScreen(vm = vm, ticketId = id, onBack = { nav.popBackStack() })
        }
    }
}
