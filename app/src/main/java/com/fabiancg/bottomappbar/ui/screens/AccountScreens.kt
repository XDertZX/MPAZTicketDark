@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.Notificacion
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.TipoNotificacion
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.Routes
import com.fabiancg.bottomappbar.ui.components.AuthRequired
import com.fabiancg.bottomappbar.ui.components.Avatar
import com.fabiancg.bottomappbar.ui.components.ConfirmDialog
import com.fabiancg.bottomappbar.ui.components.EmptyState
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.formatDate
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazGoldLight
import com.fabiancg.bottomappbar.ui.theme.MpazGoldOnDark
import com.fabiancg.bottomappbar.ui.theme.MpazNavy


@Composable
fun AccountScreen(vm: AppViewModel, onNavigate: (String) -> Unit) {
    val user = vm.currentUser
    val context = LocalContext.current
    var confirmarReset by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (user == null) {
            PageHeader("Mi cuenta", "Inicia sesión o regístrate como funcionario para reportar y seguir tus solicitudes.")
            SectionCard {
                AuthPanel(vm = vm, onAuthenticated = {
                    Toast.makeText(context, "¡Bienvenido/a, ${vm.currentUser?.nombre ?: ""}!", Toast.LENGTH_SHORT).show()
                })
            }
        } else {
            PageHeader("Mi cuenta")
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MpazNavy),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Avatar(user.iniciales, background = MpazGold, foreground = Color.White)
                        Column {
                            Text(user.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(user.role.label, color = MpazGoldOnDark, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Text(user.cargo, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Email, null, Modifier.size(16.dp))
                        Text("  ${user.email}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (user.telefono.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(16.dp))
                            Text("  ${user.telefono}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (user.registrado) {
                        Text("Cuenta creada desde «Registrarse» (guardada en el dispositivo).", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            SectionCard("Opciones") {
                AccountLink(Icons.Filled.Folder, "Mis documentos", "Cargar, ver y eliminar PDF o Word") { onNavigate(Routes.DOCUMENTS) }
                AccountLink(Icons.Filled.Notifications, "Notificaciones", "Avisos de reportes y mensajes") { onNavigate(Routes.NOTIFICATIONS) }
                when (user.role) {
                    Role.SOLICITANTE -> {
                        AccountLink(Icons.AutoMirrored.Filled.Assignment, "Mis reportes", "Pendientes y revisados") { onNavigate(Routes.REPORTS) }
                        AccountLink(Icons.Filled.SupportAgent, "Contactar a un Administrador", "Correo, teléfono o mensaje") { onNavigate(Routes.CONTACT) }
                    }
                    Role.ADMIN -> {
                        AccountLink(Icons.Filled.AdminPanelSettings, "Panel de administrador", "Resúmenes y PDF") { onNavigate(Routes.ADMIN_PANEL) }
                        AccountLink(Icons.Filled.Email, "Mensajes", "Conversaciones con funcionarios") { onNavigate(Routes.MESSAGES) }
                    }
                    Role.RESPONSABLE -> {
                        AccountLink(Icons.AutoMirrored.Filled.Assignment, "Tickets asignados", "Tu bandeja de trabajo") { onNavigate(Routes.REPORTS) }
                    }
                }
                AccountLink(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", "Salir de esta cuenta", danger = true) { onNavigate(Routes.LOGOUT) }
            }
        }

        SectionCard("Datos de demostración") {
            Text(
                "La app guarda todo en el almacenamiento local del teléfono. Si quieres volver a los datos " +
                        "ficticios originales (borra cuentas registradas, reportes nuevos, mensajes y documentos), usa este botón.",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { confirmarReset = true }) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Text("  Restablecer datos de demostración")
            }
        }
    }

    if (confirmarReset) {
        ConfirmDialog(
            title = "Restablecer datos",
            text = "Se borrarán los cambios hechos en este dispositivo y se cerrará la sesión.",
            confirmText = "Restablecer",
            destructive = true,
            onDismiss = { confirmarReset = false },
            onConfirm = {
                confirmarReset = false
                vm.resetDemoData()
                Toast.makeText(context, "Datos de demostración restablecidos", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AccountLink(icon: ImageVector, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MpazNavy
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = if (danger) tint else Color.Unspecified)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MpazGold)
        }
    }
}

@Composable
fun NotificationsScreen(
    vm: AppViewModel,
    onOpenTicket: (Int) -> Unit,
    onOpenConversation: (Int) -> Unit,
    onOpenMessages: () -> Unit,
    onLogin: () -> Unit
) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión para ver tus notificaciones.", onLogin)
    val todas by vm.notificaciones.collectAsState()
    val conversaciones by vm.conversaciones.collectAsState()
    val mias = todas.filter { it.paraUserId == user.id }.sortedByDescending { it.fecha }

    fun abrir(n: Notificacion) {
        vm.markNotificationRead(n.id)
        when {
            n.ticketId != null -> onOpenTicket(n.ticketId)
            n.conversacionId != null && conversaciones.any { it.id == n.conversacionId } -> onOpenConversation(n.conversacionId)
            n.tipo == TipoNotificacion.CONTACTO && user.role == Role.ADMIN -> onOpenMessages()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PageHeader("Notificaciones", "${mias.count { !it.leida }} sin leer", Modifier.weight(1f))
                TextButton(onClick = { vm.markAllNotificationsRead() }, enabled = mias.any { !it.leida }) {
                    Icon(Icons.Filled.DoneAll, contentDescription = null)
                    Text(" Leer todas")
                }
            }
        }
        if (mias.isEmpty()) {
            item { EmptyState(Icons.Filled.NotificationsNone, "Sin notificaciones", "Aquí verás avisos de reportes, mensajes y solicitudes.") }
        }
        items(mias, key = { it.id }) { n ->
            Card(
                onClick = { abrir(n) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(if (n.leida) Color.Transparent else MpazGold, RoundedCornerShape(2.dp))
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(n.titulo, fontWeight = if (n.leida) FontWeight.Medium else FontWeight.Bold)
                        Text(n.texto, style = MaterialTheme.typography.bodySmall)
                        Text(formatDate(n.fecha), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
