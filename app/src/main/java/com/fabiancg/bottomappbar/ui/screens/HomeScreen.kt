@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.Stats
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.Routes
import com.fabiancg.bottomappbar.ui.components.BarRow
import com.fabiancg.bottomappbar.ui.components.EmergencyBanner
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.StatTile
import com.fabiancg.bottomappbar.ui.components.formatDate
import com.fabiancg.bottomappbar.ui.components.prioridadBarColor
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazNavy
import com.fabiancg.bottomappbar.ui.theme.SuccessFg
import java.time.YearMonth

@Composable
fun HomeScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit, onNavigate: (String) -> Unit) {
    when (vm.currentUser?.role) {
        Role.ADMIN -> AdminHome(vm, onNavigate)
        Role.RESPONSABLE -> AssignedTicketsScreen(vm, onOpenTicket)
        else -> FuncionarioHome(vm, onOpenTicket, onNavigate)
    }
}

@Composable
private fun FuncionarioHome(vm: AppViewModel, onOpenTicket: (Int) -> Unit, onNavigate: (String) -> Unit) {
    val user = vm.currentUser
    val tickets by vm.tickets.collectAsState()
    val mios = vm.misReportes(tickets)
    val form: ReportFormViewModel = viewModel(key = "home_report_form")

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EmergencyBanner()

        AnimatedVisibility(visible, enter = fadeIn() + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroCard(user)

                if (user != null) {
                    val activos = mios.count { it.estado.enCurso }
                    val revisados = mios.size - activos
                    val info = mios.count { it.solicitudPendiente != null }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Pendientes", "$activos", Modifier.weight(1f), icon = Icons.Filled.HourglassTop,
                            onClick = { onNavigate(Routes.REPORTS) })
                        StatTile("Revisados", "$revisados", Modifier.weight(1f), accent = SuccessFg,
                            icon = Icons.Filled.CheckCircle, onClick = { onNavigate(Routes.REPORTS) })
                        StatTile("Piden info", "$info", Modifier.weight(1f), accent = MpazGold,
                            icon = Icons.AutoMirrored.Filled.HelpOutline, onClick = { onNavigate(Routes.REPORTS) })
                    }
                }

                SectionCard("¿Qué puedes hacer con MPAZ Ticket?") {
                    Feature(Icons.Filled.AddCircle, "Reportar problemas",
                        "Infraestructura, mobiliario o tecnología: categoría, ubicación, descripción y fotos opcionales.")
                    Feature(Icons.Filled.TrackChanges, "Seguir el estado",
                        "Cada reporte recibe un número único (ej: MP-0012) y ves si está pendiente, asignado, en proceso o solucionado.")
                    Feature(Icons.Filled.SupportAgent, "Contactar a TI",
                        "Escribe, llama o envía un correo al administrador; queda registrado y se le notifica.")
                    Feature(Icons.Filled.History, "Historial y trazabilidad",
                        "Fechas de creación, asignación, solución y cierre, con comentarios de cada etapa.")
                }

                SectionCard("¿Cómo funciona?") {
                    listOf(
                        "Reportas el problema desde esta misma página.",
                        "El encargado de TI lo revisa y define la prioridad.",
                        "Se asigna a un responsable (TI o auxiliar).",
                        "Se soluciona, se confirma y se cierra."
                    ).forEachIndexed { i, paso -> Step(i + 1, paso) }
                }
            }
        }

        SectionCard("Crear un reporte", icon = Icons.Filled.AddCircle) {
            if (user == null) {
                Text(
                    "Puedes llenar el reporte ahora. Al enviarlo te pediremos registrarte (como funcionario) " +
                            "o iniciar sesión, y luego se enviará automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ReportForm(vm = vm, form = form, onOpenTicket = onOpenTicket)
        }

        Text(
            "Prototipo académico · Todos los datos son ficticios.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun HeroCard(user: User?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MpazNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (user == null) "Bienvenido/a a MPAZ Ticket" else "¡Hola, ${user.nombre.substringBefore(" ")}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MpazNavy
            )
            Text(
                "Escuela Escritora Marcela Paz · Solicitudes de mantención",
                color = MpazGold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Centraliza los reportes de problemas de la escuela para que nada se olvide, " +
                        "se priorice lo urgente y puedas revisar el avance sin preguntar de forma verbal.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(36.dp)
                .background(MpazNavy.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = MpazNavy, modifier = Modifier.size(20.dp)) }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .background(MpazGold, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("$n", color = Color.White, fontWeight = FontWeight.Bold) }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AdminHome(vm: AppViewModel, onNavigate: (String) -> Unit) {
    val user = vm.currentUser ?: return
    val tickets by vm.tickets.collectAsState()
    val conversaciones by vm.conversaciones.collectAsState()
    val notificaciones by vm.notificaciones.collectAsState()
    val mes = YearMonth.now()
    val resumen = Stats.mes(tickets, mes)
    val activos = tickets.count { it.estado.enCurso }
    val sinLeer = vm.unreadConversations(conversaciones)
    val ultimas = notificaciones.filter { it.paraUserId == user.id }.sortedByDescending { it.fecha }.take(3)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(
            "Hola, ${user.nombre.substringBefore(" ")}",
            "Resumen de ${resumen.titulo} · datos ficticios de demostración"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Reportes del mes", "${resumen.total}", Modifier.weight(1f), icon = Icons.Filled.Inbox)
            StatTile("Pendientes", "${resumen.pendientes}", Modifier.weight(1f), accent = MpazGold, icon = Icons.Filled.HourglassTop)
            StatTile("Resueltos", "${resumen.resueltos}", Modifier.weight(1f), accent = SuccessFg, icon = Icons.Filled.CheckCircle)
        }

        SectionCard("Prioridades más frecuentes del mes", icon = Icons.Filled.Assessment) {
            val max = resumen.porPrioridad.maxOfOrNull { it.second } ?: 0
            val top = resumen.prioridadMasFrecuente
            resumen.porPrioridad.filter { it.first != null }.forEach { (p, n) ->
                BarRow("Prioridad ${p!!.label}", n, max, prioridadBarColor(p), highlight = p == top)
            }
            val sinPrioridad = resumen.porPrioridad.firstOrNull { it.first == null }?.second ?: 0
            Text(
                "Sin prioridad asignada aún: $sinPrioridad · Cancelados: ${resumen.cancelados}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Accesos rápidos") {
            QuickLink(Icons.AutoMirrored.Filled.ViewList, "Ver reportes", "$activos reportes activos") { onNavigate(Routes.REPORTS) }
            QuickLink(Icons.Filled.AdminPanelSettings, "Panel de administrador", "Resumen mensual, semestral y PDF") { onNavigate(Routes.ADMIN_PANEL) }
            QuickLink(Icons.Filled.Email, "Mensajes", if (sinLeer > 0) "$sinLeer conversación(es) sin leer" else "Sin mensajes nuevos") { onNavigate(Routes.MESSAGES) }
        }

        SectionCard("Últimas notificaciones") {
            if (ultimas.isEmpty()) Text("No tienes notificaciones.", style = MaterialTheme.typography.bodySmall)
            ultimas.forEach { n ->
                Column {
                    Text(n.titulo, fontWeight = if (n.leida) FontWeight.Normal else FontWeight.Bold)
                    Text("${n.texto} · ${formatDate(n.fecha)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) { Text("Ver todas") }
        }
    }
}

@Composable
private fun QuickLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MpazNavy)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MpazGold)
        }
    }
}
