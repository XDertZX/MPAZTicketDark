package com.fabiancg.bottomappbar.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fabiancg.bottomappbar.ui.components.AppDrawer
import com.fabiancg.bottomappbar.ui.components.BottomTab
import com.fabiancg.bottomappbar.ui.components.MpazBottomBar
import com.fabiancg.bottomappbar.ui.components.MpazTopBar
import com.fabiancg.bottomappbar.ui.screens.AccountScreen
import com.fabiancg.bottomappbar.ui.screens.AdminMessagesScreen
import com.fabiancg.bottomappbar.ui.screens.AdminPanelScreen
import com.fabiancg.bottomappbar.ui.screens.ContactAdminScreen
import com.fabiancg.bottomappbar.ui.screens.ConversationScreen
import com.fabiancg.bottomappbar.ui.screens.CreateTicketScreen
import com.fabiancg.bottomappbar.ui.screens.DocViewerScreen
import com.fabiancg.bottomappbar.ui.screens.DocumentsScreen
import com.fabiancg.bottomappbar.ui.screens.EditReportScreen
import com.fabiancg.bottomappbar.ui.screens.HomeScreen
import com.fabiancg.bottomappbar.ui.screens.LogoutScreen
import com.fabiancg.bottomappbar.ui.screens.NotificationsScreen
import com.fabiancg.bottomappbar.ui.screens.ReportsScreen
import com.fabiancg.bottomappbar.ui.screens.TicketDetailScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val REPORTS = "reports"
    const val ACCOUNT = "account"
    const val CREATE = "create"
    const val CONTACT = "contact"
    const val ADMIN_PANEL = "admin_panel"
    const val MESSAGES = "messages"
    const val NOTIFICATIONS = "notifications"
    const val DOCUMENTS = "documents"
    const val LOGOUT = "logout"
    const val DETAIL = "detail/{ticketId}"
    const val EDIT = "edit/{ticketId}"
    const val CONVERSATION = "conversation/{convId}"
    const val DOC_VIEWER = "docviewer/{docId}"

    fun detail(id: Int) = "detail/$id"
    fun edit(id: Int) = "edit/$id"
    fun conversation(id: Int) = "conversation/$id"
    fun docViewer(id: Int) = "docviewer/$id"

    val topLevel = setOf(HOME, REPORTS, ACCOUNT, CREATE, CONTACT, ADMIN_PANEL, MESSAGES, LOGOUT)
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
        launchSingleTop = true
    }
}

private fun tabFor(route: String?): BottomTab? = when (route) {
    Routes.HOME, Routes.CREATE -> BottomTab.INICIO
    Routes.REPORTS, Routes.DETAIL, Routes.EDIT, Routes.ADMIN_PANEL -> BottomTab.REPORTES
    Routes.ACCOUNT, Routes.DOCUMENTS, Routes.DOC_VIEWER, Routes.NOTIFICATIONS, Routes.LOGOUT -> BottomTab.CUENTA
    else -> null
}

@Composable
fun AppNavigation(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user = vm.currentUser

    val notificaciones by vm.notificaciones.collectAsState()
    val conversaciones by vm.conversaciones.collectAsState()
    val unread = vm.unreadNotifications(notificaciones)
    val unreadMsgs = vm.unreadConversations(conversaciones)

    fun go(target: String) {
        scope.launch { drawerState.close() }
        nav.navigateTop(target)
    }

    val openTicket: (Int) -> Unit = { id -> nav.navigate(Routes.detail(id)) }
    val openConversation: (Int) -> Unit = { id -> nav.navigate(Routes.conversation(id)) }
    val goAccount: () -> Unit = { nav.navigateTop(Routes.ACCOUNT) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(user = user, currentRoute = route, unreadMessages = unreadMsgs, onNavigate = { go(it) })
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                MpazTopBar(
                    showBack = route != null && route !in Routes.topLevel && nav.previousBackStackEntry != null,
                    onBack = { nav.popBackStack() },
                    onMenu = { scope.launch { drawerState.open() } },
                    showBell = user != null,
                    unread = unread,
                    onBell = { nav.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true } }
                )
            },
            bottomBar = {
                MpazBottomBar(selected = tabFor(route), onSelect = { nav.navigateTop(it.route) })
            }
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(inner),
                // ANIMACIONES de transición entre pantallas
                enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 10 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 10 } }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(vm = vm, onOpenTicket = openTicket, onNavigate = { nav.navigateTop(it) })
                }
                composable(Routes.REPORTS) {
                    ReportsScreen(
                        vm = vm,
                        onOpenTicket = openTicket,
                        onEdit = { id -> nav.navigate(Routes.edit(id)) },
                        onLogin = goAccount
                    )
                }
                composable(Routes.ACCOUNT) {
                    AccountScreen(vm = vm, onNavigate = { nav.navigate(it) { launchSingleTop = true } })
                }
                composable(Routes.CREATE) {
                    CreateTicketScreen(vm = vm, onOpenTicket = openTicket, onFinished = { nav.navigateTop(Routes.REPORTS) })
                }
                composable(Routes.CONTACT) {
                    ContactAdminScreen(vm = vm, onOpenConversation = openConversation, onLogin = goAccount)
                }
                composable(Routes.ADMIN_PANEL) {
                    AdminPanelScreen(vm = vm, onOpenDocument = { id -> nav.navigate(Routes.docViewer(id)) }, onLogin = goAccount)
                }
                composable(Routes.MESSAGES) {
                    AdminMessagesScreen(vm = vm, onOpenConversation = openConversation, onLogin = goAccount)
                }
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(
                        vm = vm,
                        onOpenTicket = openTicket,
                        onOpenConversation = openConversation,
                        onOpenMessages = { nav.navigateTop(Routes.MESSAGES) },
                        onLogin = goAccount
                    )
                }
                composable(Routes.DOCUMENTS) {
                    DocumentsScreen(vm = vm, onOpenViewer = { id -> nav.navigate(Routes.docViewer(id)) }, onLogin = goAccount)
                }
                composable(Routes.LOGOUT) {
                    LogoutScreen(
                        vm = vm,
                        onCancel = { if (!nav.popBackStack()) nav.navigateTop(Routes.HOME) },
                        onGoHome = { nav.navigateTop(Routes.HOME) },
                        onLogin = goAccount
                    )
                }
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("ticketId") { type = NavType.IntType })
                ) { entry ->
                    val id = entry.arguments?.getInt("ticketId") ?: -1
                    TicketDetailScreen(vm = vm, ticketId = id, onEdit = { nav.navigate(Routes.edit(it)) }, onLogin = goAccount)
                }
                composable(
                    route = Routes.EDIT,
                    arguments = listOf(navArgument("ticketId") { type = NavType.IntType })
                ) { entry ->
                    val id = entry.arguments?.getInt("ticketId") ?: -1
                    EditReportScreen(vm = vm, ticketId = id, onDone = { nav.popBackStack() }, onLogin = goAccount)
                }
                composable(
                    route = Routes.CONVERSATION,
                    arguments = listOf(navArgument("convId") { type = NavType.IntType })
                ) { entry ->
                    val id = entry.arguments?.getInt("convId") ?: -1
                    ConversationScreen(vm = vm, conversacionId = id, onNewRequest = { nav.navigateTop(Routes.CONTACT) }, onLogin = goAccount)
                }
                composable(
                    route = Routes.DOC_VIEWER,
                    arguments = listOf(navArgument("docId") { type = NavType.IntType })
                ) { entry ->
                    val id = entry.arguments?.getInt("docId") ?: -1
                    DocViewerScreen(vm = vm, docId = id)
                }
            }
        }
    }
}
