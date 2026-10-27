package com.fabiancg.bottomappbar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.RangoFecha
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.Ticket
import com.fabiancg.bottomappbar.data.TicketFilters
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.DropdownField
import com.fabiancg.bottomappbar.ui.components.EstadoChip
import com.fabiancg.bottomappbar.ui.components.FilterDropdown
import com.fabiancg.bottomappbar.ui.components.PrioridadChip
import com.fabiancg.bottomappbar.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenTicket: (Int) -> Unit,
    onCreate: () -> Unit,
    onLogout: () -> Unit
) {
    val user = vm.currentUser ?: return
    val allTickets by vm.tickets.collectAsState()
    val visible = vm.visibleTickets(allTickets)

    var showWelcome by rememberSaveable { mutableStateOf(true) }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    val title = when (user.role) {
        Role.SOLICITANTE -> "Mis solicitudes"
        Role.ADMIN -> "Bandeja de tickets"
        Role.RESPONSABLE -> "Tickets asignados a mí"
    }

    val fab: (@Composable () -> Unit)? = if (TicketRules.canCreate(user)) {
        {
            FloatingActionButton(
                onClick = onCreate,
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) { Icon(Icons.Filled.Add, contentDescription = "Crear ticket nuevo") }
        }
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text("${user.nombre} · ${user.role.label}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* ya estamos en la lista de tickets */ }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lista de tickets")
                    }
                    IconButton(onClick = { vm.toggleFilters() }) {
                        BadgedBox(badge = {
                            if (vm.filters.activeCount > 0) Badge { Text("${vm.filters.activeCount}") }
                        }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Mostrar u ocultar filtros")
                        }
                    }
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Ayuda")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                floatingActionButton = fab
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (vm.filtersVisible) {
                item {
                    FiltersPanel(
                        filters = vm.filters,
                        isAdmin = user.role == Role.ADMIN,
                        onChange = { vm.updateFilters(it) }
                    )
                }
            }
            item { SummaryLine(user, visible) }
            if (visible.isEmpty()) {
                item {
                    Text(
                        if (vm.filters.activeCount > 0) "Ningún ticket coincide con los filtros."
                        else "Aún no hay tickets para mostrar.",
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            items(visible, key = { it.id }) { ticket ->
                TicketCard(ticket) { onOpenTicket(ticket.id) }
            }
        }
    }

    if (showWelcome) {
        HelpDialog(user, welcome = true) { showWelcome = false }
    }
    if (showHelp) {
        HelpDialog(user, welcome = false) { showHelp = false }
    }
}

@Composable
private fun SummaryLine(user: User, visible: List<Ticket>) {
    val text = if (user.role == Role.ADMIN) {
        val pendientes = visible.count { it.estado == EstadoTicket.PENDIENTE }
        val porCerrar = visible.count { it.estado == EstadoTicket.SOLUCIONADO }
        "${visible.size} ticket(s) · $pendientes pendiente(s) de revisar · $porCerrar por confirmar y cerrar"
    } else {
        "${visible.size} ticket(s)"
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ticket.codigo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                EstadoChip(ticket.estado)
            }
            Text(
                ticket.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text("${ticket.categoria.label} · ${ticket.subcategoria}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Ubicación: ${ticket.ubicacion}" +
                        (if (ticket.ubicacionDetalle.isNotBlank()) " (${ticket.ubicacionDetalle})" else ""),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrioridadChip(ticket.prioridad)
            }
            Text(
                "Creado: ${formatDate(ticket.fechaCreacion)} · Responsable: ${Users.nameOf(ticket.asignadoId)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FiltersPanel(filters: TicketFilters, isAdmin: Boolean, onChange: (TicketFilters) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FilterDropdown(
                label = "Estado",
                options = EstadoTicket.entries.toList(),
                selected = filters.estado,
                optionLabel = { it.label },
                onSelected = { onChange(filters.copy(estado = it)) }
            )
            FilterDropdown(
                label = "Prioridad",
                options = Prioridad.entries.toList(),
                selected = filters.prioridad,
                optionLabel = { it.label },
                onSelected = { onChange(filters.copy(prioridad = it)) }
            )
            FilterDropdown(
                label = "Categoría",
                options = Categoria.entries.toList(),
                selected = filters.categoria,
                optionLabel = { it.label },
                onSelected = { onChange(filters.copy(categoria = it)) }
            )
            if (isAdmin) {
                FilterDropdown(
                    label = "Responsable",
                    options = Users.asignables,
                    selected = Users.asignables.firstOrNull { it.id == filters.responsableId },
                    optionLabel = { it.nombre },
                    onSelected = { onChange(filters.copy(responsableId = it?.id)) }
                )
            }
            OutlinedTextField(
                value = filters.ubicacion,
                onValueChange = { onChange(filters.copy(ubicacion = it.take(40))) },
                label = { Text("Ubicación contiene…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownField(
                label = "Fecha de creación",
                options = RangoFecha.entries.toList(),
                selected = filters.rango,
                optionLabel = { it.label },
                onSelected = { onChange(filters.copy(rango = it)) }
            )
            TextButton(onClick = { onChange(TicketFilters()) }) { Text("Limpiar filtros") }
        }
    }
}

@Composable
private fun HelpDialog(user: User, welcome: Boolean, onDismiss: () -> Unit) {
    val que = when (user.role) {
        Role.SOLICITANTE ->
            "Puedes crear tickets (botón +), adjuntar fotos opcionales y revisar el estado y los comentarios de tus solicitudes."
        Role.ADMIN ->
            "Puedes ver todos los tickets, revisar y priorizar, asignar o reasignar responsables, comentar, confirmar la solución, cerrar y filtrar el historial."
        Role.RESPONSABLE ->
            "Solo ves los tickets asignados a ti. Puedes iniciar el trabajo, comentar acciones, pedir materiales o información, adjuntar evidencia y marcar como solucionado."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (welcome) "¡Bienvenido/a, ${user.nombre}!" else "Ayuda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ingresaste como ${user.role.label} (${user.cargo}).")
                Text(que)
                Text(
                    "Importante: esta app no reemplaza los procedimientos de emergencia de la escuela. " +
                            "Si hay riesgo inmediato, avisa por los canales definidos por el establecimiento.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido") } }
    )
}
