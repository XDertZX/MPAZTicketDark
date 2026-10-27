@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.PhotoStorage
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.RangoFecha
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.Ticket
import com.fabiancg.bottomappbar.data.TicketFilters
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.data.Validators
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.AuthRequired
import com.fabiancg.bottomappbar.ui.components.DropdownField
import com.fabiancg.bottomappbar.ui.components.EmptyState
import com.fabiancg.bottomappbar.ui.components.EstadoChip
import com.fabiancg.bottomappbar.ui.components.FieldMessage
import com.fabiancg.bottomappbar.ui.components.FilterDropdown
import com.fabiancg.bottomappbar.ui.components.GravedadChip
import com.fabiancg.bottomappbar.ui.components.InfoRequestBanner
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.PhotoPickerButtons
import com.fabiancg.bottomappbar.ui.components.PhotoRow
import com.fabiancg.bottomappbar.ui.components.PrioridadChip
import com.fabiancg.bottomappbar.ui.components.PrioridadSelector
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.ValidatedTextField
import com.fabiancg.bottomappbar.ui.components.formatDate
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazNavy


@Composable
fun ReportsScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit, onEdit: (Int) -> Unit, onLogin: () -> Unit) {
    when (vm.currentUser?.role) {
        null -> AuthRequired("Inicia sesión para ver tus reportes y su estado.", onLogin)
        Role.SOLICITANTE -> MisReportesScreen(vm, onOpenTicket, onEdit)
        Role.ADMIN -> AdminReportesScreen(vm, onOpenTicket)
        Role.RESPONSABLE -> AssignedTicketsScreen(vm, onOpenTicket)
    }
}

@Composable
private fun MisReportesScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit, onEdit: (Int) -> Unit) {
    val tickets by vm.tickets.collectAsState()
    val mios = vm.misReportes(tickets)
    val pendientes = mios.filter { it.estado.enCurso }
    val revisados = mios.filter { !it.estado.enCurso }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val lista = if (tab == 0) pendientes else revisados

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Mis reportes", "Revisa el avance de tus solicitudes y edita las que siguen activas.") }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tab == 0, onClick = { tab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Pendientes (${pendientes.size})") }
                )
                SegmentedButton(
                    selected = tab == 1, onClick = { tab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Revisados (${revisados.size})") }
                )
            }
        }
        item {
            AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab_hint") { t ->
                Text(
                    if (t == 0) "En curso: pendientes, revisados por TI, asignados o en proceso. Puedes editarlos."
                    else "Ya atendidos: solucionados, cerrados o cancelados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (lista.isEmpty()) {
            item {
                EmptyState(
                    icon = if (tab == 0) Icons.Filled.Inbox else Icons.Filled.CheckCircle,
                    title = if (tab == 0) "No tienes reportes pendientes" else "Aún no tienes reportes revisados",
                    text = "Puedes crear un reporte desde Inicio o desde el menú."
                )
            }
        }
        items(lista, key = { it.id }) { ticket ->
            FuncionarioReportCard(
                ticket = ticket,
                canEdit = vm.currentUser?.let { TicketRules.canEditAsFuncionario(it, ticket) } == true,
                onOpen = { onOpenTicket(ticket.id) },
                onEdit = { onEdit(ticket.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun FuncionarioReportCard(ticket: Ticket, canEdit: Boolean, onOpen: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TicketHeader(ticket)
            Text(ticket.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${ticket.categoria.label} · ${ticket.subcategoria}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GravedadChip(ticket.gravedad)
                PrioridadChip(ticket.prioridad)
            }
            ticket.solicitudPendiente?.let { InfoRequestBanner(it) }
            Text("Creado: ${formatDate(ticket.fechaCreacion)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen) { Text("Ver detalle") }
                if (canEdit) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text(" Editar")
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketHeader(ticket: Ticket) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ticket.codigo, style = MaterialTheme.typography.labelLarge, color = MpazNavy, fontWeight = FontWeight.Bold)
        EstadoChip(ticket.estado)
    }
}

@Composable
private fun AdminReportesScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit) {
    val all by vm.tickets.collectAsState()
    val filtrados = vm.visibleTickets(all)
    var alcance by rememberSaveable { mutableIntStateOf(0) } // 0 activos, 1 historial
    val activos = filtrados.filter { !it.estado.esFinal }
    val lista = if (alcance == 0) activos else filtrados

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Ver reportes", "Entra a cada reporte para revisarlo, priorizarlo, asignarlo o pedir más información.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    SegmentedButton(
                        selected = alcance == 0, onClick = { alcance = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Activos (${activos.size})") }
                    )
                    SegmentedButton(
                        selected = alcance == 1, onClick = { alcance = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("Historial (${filtrados.size})") }
                    )
                }
                FilledTonalButton(onClick = { vm.toggleFilters() }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    BadgedBox(badge = { if (vm.filters.activeCount > 0) Badge { Text("${vm.filters.activeCount}") } }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Mostrar u ocultar filtros")
                    }
                }
            }
        }
        item {
            AnimatedVisibility(vm.filtersVisible) {
                FiltersPanel(vm.filters, onChange = { vm.updateFilters(it) })
            }
        }
        if (lista.isEmpty()) {
            item {
                EmptyState(Icons.Filled.Inbox, "Sin reportes",
                    if (vm.filters.activeCount > 0) "Ningún reporte coincide con los filtros." else "No hay reportes para mostrar.")
            }
        }
        items(lista, key = { it.id }) { t ->
            AdminReportCard(t, onEnter = { onOpenTicket(t.id) }, modifier = Modifier.animateItem())
        }
    }
}

@Composable
private fun AdminReportCard(ticket: Ticket, onEnter: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onEnter,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TicketHeader(ticket)
            Text(ticket.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                ticket.descripcion,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PrioridadChip(ticket.prioridad)
                GravedadChip(ticket.gravedad)
            }
            ticket.solicitudPendiente?.let { InfoRequestBanner(it) }
            Text(
                "${Users.nameOf(ticket.solicitanteId)} · ${ticket.ubicacion}" +
                        (if (ticket.ubicacionDetalle.isNotBlank()) " (${ticket.ubicacionDetalle})" else "") +
                        " · ${formatDate(ticket.fechaCreacion)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onEnter) {
                    Text("Entrar ")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun FiltersPanel(filters: TicketFilters, onChange: (TicketFilters) -> Unit) {
    SectionCard("Filtros del historial", icon = Icons.Filled.FilterList) {
        FilterDropdown("Estado", EstadoTicket.entries.toList(), filters.estado, { it.label }, { onChange(filters.copy(estado = it)) })
        FilterDropdown("Prioridad", Prioridad.entries.toList(), filters.prioridad, { it.label }, { onChange(filters.copy(prioridad = it)) })
        FilterDropdown("Categoría", Categoria.entries.toList(), filters.categoria, { it.label }, { onChange(filters.copy(categoria = it)) })
        FilterDropdown(
            "Responsable", Users.asignables, Users.asignables.firstOrNull { it.id == filters.responsableId },
            { it.nombre }, { onChange(filters.copy(responsableId = it?.id)) }
        )
        OutlinedTextField(
            value = filters.ubicacion,
            onValueChange = { onChange(filters.copy(ubicacion = it.take(40))) },
            label = { Text("Ubicación contiene…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownField("Fecha de creación", RangoFecha.entries.toList(), filters.rango, { it.label }, { onChange(filters.copy(rango = it)) })
        TextButton(onClick = { onChange(TicketFilters()) }) { Text("Limpiar filtros") }
    }
}

@Composable
fun AssignedTicketsScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit) {
    val all by vm.tickets.collectAsState()
    val visibles = vm.visibleTickets(all)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Tickets asignados a mí", "${visibles.size} ticket(s). Inicia el trabajo, comenta y marca como solucionado.") }
        if (visibles.isEmpty()) item { EmptyState(Icons.Filled.Inbox, "Sin tickets asignados", "Cuando TI te asigne un ticket aparecerá aquí.") }
        items(visibles, key = { it.id }) { t ->
            AdminReportCard(t, onEnter = { onOpenTicket(t.id) }, modifier = Modifier.animateItem())
        }
    }
}

class EditReportViewModel : ViewModel() {
    var detalle by mutableStateOf("")
    var gravedad by mutableStateOf<Prioridad?>(null)
    var respuesta by mutableStateOf("")
    var intentoRespuesta by mutableStateOf(false)
    val fotos = mutableStateListOf<String>()
}

private const val MAX_NUEVAS_FOTOS = 4

@Composable
fun EditReportScreen(vm: AppViewModel, ticketId: Int, onDone: () -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión para editar tus reportes.", onLogin)
    val tickets by vm.tickets.collectAsState()
    val ticket = tickets.firstOrNull { it.id == ticketId }
    val context = LocalContext.current
    val form: EditReportViewModel = viewModel(key = "edit_report_$ticketId")
    var intento by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    if (ticket == null || !TicketRules.canEditAsFuncionario(user, ticket)) {
        EmptyState(Icons.Filled.Edit, "No se puede editar", "Solo puedes editar tus reportes que siguen activos.")
        return
    }
    val gravedadActual = form.gravedad ?: ticket.gravedad

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader("Editar ${ticket.codigo}", ticket.titulo)

        SectionCard("Estado actual") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EstadoChip(ticket.estado)
                GravedadChip(ticket.gravedad)
                PrioridadChip(ticket.prioridad)
            }
            Text(ticket.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }

        ticket.solicitudPendiente?.let { sol ->
            SectionCard("Solicitud de información de TI") {
                InfoRequestBanner(sol)
                Text("«${sol.mensaje}»", style = MaterialTheme.typography.bodyMedium)
                Text("— ${Users.nameOf(sol.adminId)}, ${formatDate(sol.fecha)}", style = MaterialTheme.typography.bodySmall)
                ValidatedTextField(
                    value = form.respuesta,
                    onValueChange = { form.respuesta = it.take(400) },
                    label = "Tu respuesta *",
                    error = Validators.respuestaInfo(form.respuesta),
                    showError = form.intentoRespuesta || form.respuesta.isNotEmpty(),
                    leadingIcon = Icons.Filled.QuestionAnswer,
                    helper = "${form.respuesta.length}/400",
                    singleLine = false,
                    minLines = 3
                )
                Button(
                    onClick = {
                        form.intentoRespuesta = true
                        if (Validators.respuestaInfo(form.respuesta) == null) {
                            val e = vm.answerInfo(ticket.id, form.respuesta)
                            Toast.makeText(context, e ?: "Respuesta enviada al administrador", Toast.LENGTH_SHORT).show()
                            if (e == null) {
                                form.respuesta = ""
                                form.intentoRespuesta = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enviar respuesta") }
            }
        }

        SectionCard("Agregar detalles del problema") {
            ValidatedTextField(
                value = form.detalle,
                onValueChange = { form.detalle = it.take(400); error = null },
                label = "¿Qué cambió o qué más debe saber TI?",
                error = Validators.detalleOpcional(form.detalle),
                showError = intento || form.detalle.isNotEmpty(),
                leadingIcon = Icons.Filled.Description,
                helper = "${form.detalle.length}/400 · opcional",
                singleLine = false,
                minLines = 3
            )
        }

        SectionCard("Actualizar gravedad") {
            PrioridadSelector(selected = gravedadActual, onSelect = { form.gravedad = it; error = null })
            if (gravedadActual != ticket.gravedad) {
                Text("Cambiará de ${ticket.gravedad.label} a ${gravedadActual.label}.", style = MaterialTheme.typography.bodySmall, color = MpazGold)
            }
        }

        SectionCard("Fotos nuevas (solo si es necesario)") {
            Text("Sube fotos del estado actual del problema. Evita rostros y datos personales.", style = MaterialTheme.typography.bodySmall)
            PhotoPickerButtons(
                onPhotoReady = { if (form.fotos.size < MAX_NUEVAS_FOTOS) form.fotos.add(it) },
                enabled = form.fotos.size < MAX_NUEVAS_FOTOS
            )
            if (form.fotos.isNotEmpty()) {
                PhotoRow(form.fotos.toList(), onRemove = { name ->
                    form.fotos.remove(name)
                    PhotoStorage.delete(context, name)
                })
            }
            if (ticket.fotos.isNotEmpty()) {
                Text("Fotos ya adjuntas: ${ticket.fotos.size}", style = MaterialTheme.typography.bodySmall)
            }
        }

        AnimatedVisibility(error != null) {
            FieldMessage(error = error, helper = null)
        }

        Button(
            onClick = {
                intento = true
                error = Validators.edicionReporte(form.detalle, gravedadActual != ticket.gravedad, form.fotos.size)
                    ?: vm.updateReport(ticket.id, form.detalle, gravedadActual, form.fotos.toList())
                if (error == null) {
                    Toast.makeText(context, "Reporte actualizado", Toast.LENGTH_SHORT).show()
                    form.detalle = ""
                    form.gravedad = null
                    form.fotos.clear()
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Text("  Guardar cambios")
        }
        Text(
            "El administrador recibirá una notificación con los cambios.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}