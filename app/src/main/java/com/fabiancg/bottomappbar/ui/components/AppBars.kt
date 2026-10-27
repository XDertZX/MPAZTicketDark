@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.ui.Routes
import com.fabiancg.bottomappbar.ui.theme.BarGray
import com.fabiancg.bottomappbar.ui.theme.BarGrayDark
import com.fabiancg.bottomappbar.ui.theme.BarIndicator
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazGoldOnDark
import com.fabiancg.bottomappbar.ui.theme.MpazNavy

// Fondo oscuro de las barras (casi negro con tinte verde)
private val BarDark = Color(0xFF0B0F0E)

@Composable
fun MpazTopBar(
    showBack: Boolean,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    showBell: Boolean,
    unread: Int,
    onBell: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MPAZ", fontWeight = FontWeight.ExtraBold, color = MpazNavy)
                    Text(" Ticket", fontWeight = FontWeight.ExtraBold, color = MpazGold)
                    Text(" Dark", fontWeight = FontWeight.ExtraBold, color = MpazNavy)
                }
            },
            navigationIcon = {
                Row {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            },
            actions = {
                if (showBell) {
                    IconButton(onClick = onBell) {
                        BadgedBox(badge = {
                            if (unread > 0) {
                                Badge(containerColor = MpazGold, contentColor = Color.White) {
                                    Text(if (unread > 9) "9+" else "$unread")
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones ($unread sin leer)")
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = BarDark,
                titleContentColor = MpazNavy,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MpazNavy) // línea verde bajo la barra
        )
    }
}


enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    INICIO(Routes.HOME, "Inicio", Icons.Filled.Home),
    REPORTES(Routes.REPORTS, "Reportes", Icons.Filled.Description),
    CUENTA(Routes.ACCOUNT, "Cuenta", Icons.Filled.AccountCircle)
}

@Composable
fun MpazBottomBar(selected: BottomTab?, onSelect: (BottomTab) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = BarDark, tonalElevation = 0.dp) {
            BottomTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BarGrayDark,   // verde
                        unselectedIconColor = BarGray,
                        selectedTextColor = MpazNavy,      // verde
                        unselectedTextColor = BarGray,
                        indicatorColor = BarIndicator
                    )
                )
            }
        }
    }
}


private data class DrawerEntry(val label: String, val route: String, val icon: ImageVector, val badge: Int = 0)

@Composable
fun AppDrawer(
    user: User?,
    currentRoute: String?,
    unreadMessages: Int,
    onNavigate: (String) -> Unit
) {
    val entries: List<DrawerEntry> = when (user?.role) {
        null -> listOf(
            DrawerEntry("Inicio", Routes.HOME, Icons.Filled.Home),
            DrawerEntry("Crear Reporte", Routes.CREATE, Icons.Filled.AddCircle)
        )
        Role.SOLICITANTE -> listOf(
            DrawerEntry("Mis reportes", Routes.REPORTS, Icons.AutoMirrored.Filled.Assignment),
            DrawerEntry("Crear Reporte", Routes.CREATE, Icons.Filled.AddCircle),
            DrawerEntry("Contactar a un Administrador", Routes.CONTACT, Icons.Filled.SupportAgent, unreadMessages)
        )
        Role.ADMIN -> listOf(
            DrawerEntry("Ver reportes", Routes.REPORTS, Icons.AutoMirrored.Filled.ViewList),
            DrawerEntry("Panel de administrador", Routes.ADMIN_PANEL, Icons.Filled.AdminPanelSettings),
            DrawerEntry("Mensajes", Routes.MESSAGES, Icons.Filled.Email, unreadMessages)
        )
        Role.RESPONSABLE -> listOf(
            DrawerEntry("Tickets asignados", Routes.REPORTS, Icons.AutoMirrored.Filled.Assignment)
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .background(BarDark)
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar rojo con texto blanco
            Avatar(initials = user?.iniciales ?: "MP", background = MpazGold)
            Text(
                user?.nombre ?: "Invitado",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                user?.let { "${it.role.label} · ${it.cargo}" } ?: "Inicia sesión para ver tus reportes",
                color = MpazGoldOnDark, // verde suave
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MpazNavy)
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp)
        ) {
            entries.forEach { e ->
                DrawerItem(e.label, e.icon, selected = currentRoute == e.route, badge = e.badge) { onNavigate(e.route) }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.navigationBarsPadding().padding(vertical = 8.dp)) {
            if (user != null) {
                DrawerItem("Cerrar sesión", Icons.AutoMirrored.Filled.Logout, selected = currentRoute == Routes.LOGOUT, danger = true) {
                    onNavigate(Routes.LOGOUT)
                }
            } else {
                DrawerItem("Iniciar sesión / Registrarse", Icons.AutoMirrored.Filled.Login, selected = currentRoute == Routes.ACCOUNT) {
                    onNavigate(Routes.ACCOUNT)
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: Int = 0,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MpazNavy // rojo o verde
    NavigationDrawerItem(
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        badge = if (badge > 0) {
            { Badge(containerColor = MpazGold, contentColor = Color.White) { Text("$badge") } }
        } else null,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BarIndicator,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = tint,
            unselectedIconColor = tint,
            selectedTextColor = tint,
            unselectedTextColor = if (danger) tint else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}