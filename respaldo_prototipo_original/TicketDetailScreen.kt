package com.fabiancg.bottomappbar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.Catalogo
import com.fabiancg.bottomappbar.data.Comentario
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.Ticket
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.TipoComentario
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.DropdownField
import com.fabiancg.bottomappbar.ui.components.EstadoChip
import com.fabiancg.bottomappbar.ui.components.NoteDialog
import com.fabiancg.bottomappbar.ui.components.PhotoPickerButtons
import com.fabiancg.bottomappbar.ui.components.PhotoRow
import com.fabiancg.bottomappbar.ui.components.Pill
import com.fabiancg.bottomappbar.ui.components.PrioridadChip
import com.fabiancg.bottomappbar.ui.components.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(vm: AppViewModel, ticketId: Int, onBack: () -> Unit) {
    val user = vm.currentUser ?: return
    val tickets by vm.tickets.collectAsState()
    val ticket = tickets.firstOrNull { it.id == ticketId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ticket?.codigo ?: "Ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->

        if (ticket == null || !TicketRules.canView(user, ticket)) {
            Box(Modifier.padding(padding).fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No tienes acceso a este ticket o ya no existe.")
            }
        } else {
            TicketDetailContent(vm, user, ticket, Modifier.padding(padding))
        }
    }
}

private data class NoteConfig(val title: String, val label: String, val confirm: String, val required: Boolean)

private fun noteConfig(from: EstadoTicket, to: EstadoTicket): NoteConfig = when (to) {
    EstadoTicket.EN_PROCESO ->
        if (from == EstadoTicket.SOLUCIONADO)
            NoteConfig("Reabrir ticket", "Motivo por el que la solución no se acepta", "Reabrir", true)
        else NoteConfig("Iniciar trabajo", "Nota inicial (opcional)", "Iniciar", false)
    EstadoTicket.SOLUCIONADO ->
        NoteConfig("Marcar como solucionado", "Describe las acciones realizadas", "Marcar solucionado", true)
    EstadoTicket.CERRADO ->
        NoteConfig("Confirmar solución y cerrar", "Comentario de cierre (opcional)", "Cerrar ticket", false)
    EstadoTicket.CANCELADO ->
        NoteConfig("Cancelar ticket", "Motivo de la cancelación", "Cancelar ticket", true)
    else -> NoteConfig("Cambiar estado", "Nota (opcional)", "Confirmar", false)
}

private fun actionLabel(from: EstadoTicket, to: EstadoTicket): String = when (to) {
    EstadoTicket.EN_PROCESO -> if (from == EstadoTicket.SOLUCIONADO) "Reabrir (rechazar solución)" else "Iniciar trabajo"
    EstadoTicket.SOLUCIONADO -> "Marcar como solucionado"
    EstadoTicket.CERRADO -> "Confirmar solución y cerrar"
    EstadoTicket.CANCELADO -> "Cancelar ticket"
    else -> to.label
}

@Composable
private fun TicketDetailContent(vm: AppViewModel, user: User, ticket: Ticket, modifier: Modifier) {
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf("") }
    var commentText by rememberSaveable { mutableStateOf("") }
    var commentType by rememberSaveable { mutableStateOf(TipoComentario.GENERAL) }

    fun feedback(error: String?, okMessage: String) {
        Toast.makeText(context, error ?: okMessage, Toast.LENGTH_SHORT).show()
    }

    val isAssignee = TicketRules.isAssignee(user, ticket)
    val canClassify = TicketRules.canClassify(user, ticket)
    val canAssign = TicketRules.canAssign(user, ticket)
    // «Revisado» se alcanza con el diálogo «Revisar y priorizar», por eso no se lista aquí
    val transitions = TicketRules.allowedTransitions(user, ticket).filter { it != EstadoTicket.REVISADO }
    val solicitante = Users.byId(ticket.solicitanteId)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Encabezado
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ticket.titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoChip(ticket.estado)
                    PrioridadChip(ticket.prioridad)
                }
                if (ticket.requiereMateriales) {
                    Pill("Requiere materiales", Color(0xFFFFE0C2), Color(0xFF8A3B00))
                }
            }
        }

        // ---- Datos
        Section("Datos del ticket") {
            InfoRow("Categoría", "${ticket.categoria.label} · ${ticket.subcategoria}")
            InfoRow(
                "Ubicación",
                ticket.ubicacion + if (ticket.ubicacionDetalle.isNotBlank()) " (${ticket.ubicacionDetalle})" else ""
            )
            InfoRow("Solicitante", "${solicitante?.nombre ?: "—"} · ${solicitante?.cargo ?: ""}")
            InfoRow("Creado", formatDate(ticket.fechaCreacion))
            InfoRow("Administrador", Users.nameOf(ticket.adminId))
            InfoRow("Responsable", Users.nameOf(ticket.asignadoId))
            InfoRow("Asignado", formatDate(ticket.fechaAsignacion))
            InfoRow("Solucionado", formatDate(ticket.fechaSolucion))
            InfoRow("Cerrado", formatDate(ticket.fechaCierre))
        }

        Section("Descripción") { Text(ticket.descripcion) }

        if (ticket.fotos.isNotEmpty()) {
            Section("Fotografías del problema") { PhotoRow(ticket.fotos) }
        }


        if (canClassify || canAssign || transitions.isNotEmpty()) {
            Section("Gestión del ticket") {
                if (canClassify) {
                    Button(onClick = { dialog = "review" }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (ticket.estado == EstadoTicket.PENDIENTE) "Revisar y priorizar" else "Editar clasificación y prioridad")
                    }
                }
                if (canAssign) {
                    Button(onClick = { dialog = "assign" }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (ticket.asignadoId == null) "Asignar responsable" else "Reasignar")
                    }
                }
                transitions.forEach { target ->
                    val destructive = target == EstadoTicket.CANCELADO
                    OutlinedButton(
                        onClick = { dialog = "to:${target.name}" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (destructive)
                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        else ButtonDefaults.outlinedButtonColors()
                    ) { Text(actionLabel(ticket.estado, target)) }
                }
            }
        }


        if (ticket.evidencias.isNotEmpty() || TicketRules.canAddEvidence(user, ticket)) {
            Section("Evidencia de la solución") {
                if (ticket.evidencias.isEmpty()) {
                    Text("Aún no hay evidencia adjunta.", style = MaterialTheme.typography.bodySmall)
                } else {
                    PhotoRow(ticket.evidencias)
                }
                if (TicketRules.canAddEvidence(user, ticket)) {
                    Text(
                        "Adjunta una foto de la reparación (evita rostros y datos personales).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    PhotoPickerButtons(
                        onPhotoReady = { name -> feedback(vm.addEvidence(ticket.id, name), "Evidencia adjuntada") }
                    )
                }
            }
        }


        Section("Comentarios") {
            if (ticket.comentarios.isEmpty()) {
                Text("Aún no hay comentarios.", style = MaterialTheme.typography.bodySmall)
            }
            ticket.comentarios.forEach { CommentItem(it) }

            if (TicketRules.canComment(user, ticket)) {
                HorizontalDivider()
                if (isAssignee) {
                    DropdownField(
                        label = "Tipo de comentario",
                        options = TipoComentario.entries.toList(),
                        selected = commentType,
                        optionLabel = { it.label },
                        onSelected = { commentType = it }
                    )
                }
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it.take(300) },
                    label = { Text("Escribe un comentario") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = commentText.isNotBlank(),
                    onClick = {
                        val error = vm.addComment(ticket.id, commentText, if (isAssignee) commentType else TipoComentario.GENERAL)
                        if (error == null) {
                            commentText = ""
                            commentType = TipoComentario.GENERAL
                        }
                        feedback(error, "Comentario publicado")
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Publicar")
                }
            }
        }


        Section("Historial de cambios") {
            ticket.historial.sortedByDescending { it.fecha }.forEach { item ->
                Column {
                    Text(item.descripcion, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${formatDate(item.fecha)} · ${Users.nameOf(item.usuarioId)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }


    when {
        dialog == "review" -> ReviewDialog(
            ticket = ticket,
            onDismiss = { dialog = "" },
            onConfirm = { categoria, sub, prioridad ->
                feedback(vm.review(ticket.id, categoria, sub, prioridad), "Ticket clasificado")
                dialog = ""
            }
        )

        dialog == "assign" -> AssignDialog(
            ticket = ticket,
            onDismiss = { dialog = "" },
            onConfirm = { responsable ->
                feedback(vm.assign(ticket.id, responsable), "Ticket asignado a ${responsable.nombre}")
                dialog = ""
            }
        )

        dialog.startsWith("to:") -> {
            val target = EstadoTicket.entries.firstOrNull { it.name == dialog.removePrefix("to:") }
            if (target == null) {
                dialog = ""
            } else {
                val config = noteConfig(ticket.estado, target)
                NoteDialog(
                    title = config.title,
                    label = config.label,
                    confirmText = config.confirm,
                    required = config.required,
                    onDismiss = { dialog = "" },
                    onConfirm = { note ->
                        feedback(vm.changeState(ticket.id, target, note), "Estado actualizado: ${target.label}")
                        dialog = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            modifier = Modifier.weight(0.38f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, modifier = Modifier.weight(0.62f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CommentItem(c: Comentario) {
    val autor = Users.byId(c.autorId)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${autor?.nombre ?: "Usuario"} · ${autor?.cargo ?: ""}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(formatDate(c.fecha), style = MaterialTheme.typography.labelSmall)
            if (c.tipo != TipoComentario.GENERAL) {
                Pill(c.tipo.label, Color(0xFFFFE0C2), Color(0xFF8A3B00))
            }
            Text(c.texto)
        }
    }
}

@Composable
private fun ReviewDialog(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onConfirm: (Categoria, String, Prioridad) -> Unit
) {
    var categoria by remember { mutableStateOf(ticket.categoria) }
    var sub by remember { mutableStateOf(ticket.subcategoria) }
    var prioridad by remember { mutableStateOf(ticket.prioridad) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Revisar y priorizar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = "Categoría",
                    options = Categoria.entries.toList(),
                    selected = categoria,
                    optionLabel = { it.label },
                    onSelected = {
                        categoria = it
                        sub = Catalogo.subcategorias[it]?.firstOrNull() ?: ""
                    }
                )
                DropdownField(
                    label = "Subcategoría",
                    options = Catalogo.subcategorias[categoria] ?: emptyList(),
                    selected = sub,
                    optionLabel = { it },
                    onSelected = { sub = it }
                )
                DropdownField(
                    label = "Prioridad *",
                    options = Prioridad.entries.toList(),
                    selected = prioridad,
                    optionLabel = { it.label },
                    onSelected = { prioridad = it }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = prioridad != null && sub.isNotBlank(), onClick = {
                prioridad?.let { onConfirm(categoria, sub, it) }
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}

@Composable
private fun AssignDialog(ticket: Ticket, onDismiss: () -> Unit, onConfirm: (User) -> Unit) {
    var selected by remember { mutableStateOf(Users.byId(ticket.asignadoId)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ticket.asignadoId == null) "Asignar responsable" else "Reasignar ticket") },
        text = {
            DropdownField(
                label = "Responsable",
                options = Users.asignables,
                selected = selected,
                optionLabel = { "${it.nombre} · ${it.cargo}" },
                onSelected = { selected = it }
            )
        },
        confirmButton = {
            TextButton(
                enabled = selected != null && selected?.id != ticket.asignadoId,
                onClick = { selected?.let(onConfirm) }
            ) { Text("Asignar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
