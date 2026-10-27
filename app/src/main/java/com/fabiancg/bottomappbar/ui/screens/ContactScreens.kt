@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.Conversacion
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.TipoContacto
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.data.Validators
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.AuthRequired
import com.fabiancg.bottomappbar.ui.components.Avatar
import com.fabiancg.bottomappbar.ui.components.ConfirmDialog
import com.fabiancg.bottomappbar.ui.components.EmptyState
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.Pill
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.ValidatedTextField
import com.fabiancg.bottomappbar.ui.components.formatDate
import com.fabiancg.bottomappbar.ui.components.launchExternal
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazNavy
import com.fabiancg.bottomappbar.ui.theme.SuccessBg
import com.fabiancg.bottomappbar.ui.theme.SuccessFg


private fun dialIntent(phone: String) = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' }))

private fun emailIntent(email: String, subject: String) = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    putExtra(Intent.EXTRA_SUBJECT, subject)
}

@Composable
private fun EstadoConversacionChip(c: Conversacion) {
    if (c.abierta) Pill("Abierta", SuccessBg, SuccessFg) else Pill("Cerrada", Color(0xFF26292B), Color(0xFF9CA3AF))
}


@Composable
fun ContactAdminScreen(vm: AppViewModel, onOpenConversation: (Int) -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión para contactar a un administrador.", onLogin)
    val context = LocalContext.current
    val conversaciones by vm.conversaciones.collectAsState()
    val mias = conversaciones.filter { it.funcionarioId == user.id }.sortedByDescending { it.ultimaFecha }
    var escribirA by rememberSaveable { mutableIntStateOf(-1) }

    if (user.role == Role.ADMIN) {
        EmptyState(Icons.Filled.Forum, "Eres administrador", "Revisa y responde los mensajes desde «Mensajes» en el menú.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                "Contactar a un Administrador",
                "Comunícate directo con TI por correo, teléfono o mensaje. Cada contacto queda registrado " +
                        "y el administrador recibe una notificación en su panel."
            )
        }
        items(Users.admins, key = { it.id }) { admin ->
            AdminContactCard(
                admin = admin,
                onEmail = {
                    vm.registerContact(admin, TipoContacto.CORREO)
                    launchExternal(context, emailIntent(admin.email, "MPAZ Ticket · Consulta de ${user.nombre}"),
                        "No hay una app de correo instalada.")
                },
                onCall = {
                    vm.registerContact(admin, TipoContacto.LLAMADA)
                    launchExternal(context, dialIntent(admin.telefono), "No hay una app de teléfono disponible.")
                },
                onMessage = { escribirA = admin.id }
            )
        }
        item {
            Text("Mis conversaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MpazNavy)
        }
        if (mias.isEmpty()) {
            item { Text("Aún no has enviado mensajes.", style = MaterialTheme.typography.bodySmall) }
        }
        items(mias, key = { "c${it.id}" }) { c ->
            ConversationRow(
                c = c,
                title = c.asunto,
                subtitle = "Con ${Users.nameOf(c.adminId)} · ${formatDate(c.ultimaFecha)}",
                unread = c.noLeidaFuncionario,
                onClick = { onOpenConversation(c.id) }
            )
        }
    }

    Users.byId(escribirA)?.let { admin ->
        NewMessageDialog(
            admin = admin,
            onDismiss = { escribirA = -1 },
            onSend = { asunto, texto ->
                val (error, id) = vm.newConversation(admin, asunto, texto)
                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                } else {
                    escribirA = -1
                    Toast.makeText(context, "Mensaje enviado a ${admin.nombre}", Toast.LENGTH_SHORT).show()
                    id?.let(onOpenConversation)
                }
            }
        )
    }
}

@Composable
private fun AdminContactCard(admin: User, onEmail: () -> Unit, onCall: () -> Unit, onMessage: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(admin.iniciales)
                Column {
                    Text(admin.nombre, fontWeight = FontWeight.Bold)
                    Text(admin.cargo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Email, contentDescription = null, tint = MpazNavy, modifier = Modifier.size(18.dp))
                Text(admin.email, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = MpazNavy, modifier = Modifier.size(18.dp))
                Text(admin.telefono, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEmail, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Correo")
                }
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Llamar")
                }
                Button(onClick = onMessage, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Mensaje")
                }
            }
        }
    }
}

@Composable
private fun NewMessageDialog(admin: User, onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var asunto by rememberSaveable { mutableStateOf("") }
    var texto by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    val eAsunto = Validators.asunto(asunto)
    val eTexto = Validators.mensaje(texto)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensaje para ${admin.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ValidatedTextField(
                    value = asunto, onValueChange = { asunto = it.take(60) },
                    label = "Asunto *", error = eAsunto, showError = intento || asunto.isNotEmpty(),
                    leadingIcon = Icons.AutoMirrored.Filled.Subject
                )
                ValidatedTextField(
                    value = texto, onValueChange = { texto = it.take(500) },
                    label = "Mensaje *", error = eTexto, showError = intento || texto.isNotEmpty(),
                    leadingIcon = Icons.AutoMirrored.Filled.Chat, helper = "${texto.length}/500",
                    singleLine = false, minLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intento = true
                if (eAsunto == null && eTexto == null) onSend(asunto, texto)
            }) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ConversationRow(c: Conversacion, title: String, subtitle: String, unread: Boolean, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(if (unread) MpazGold else Color.Transparent, CircleShape)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(c.mensajes.lastOrNull()?.texto ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EstadoConversacionChip(c)
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun ConversationScreen(vm: AppViewModel, conversacionId: Int, onNewRequest: () -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión para ver tus mensajes.", onLogin)
    val context = LocalContext.current
    val conversaciones by vm.conversaciones.collectAsState()
    val c = conversaciones.firstOrNull { it.id == conversacionId }
    var texto by rememberSaveable { mutableStateOf("") }
    var confirmarCierre by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    if (c == null || !TicketRules.canViewConversation(user, c)) {
        EmptyState(Icons.Filled.Forum, "Conversación no disponible", "Puede que haya sido eliminada del historial.")
        return
    }
    val esAdmin = user.role == Role.ADMIN
    val funcionario = Users.byId(c.funcionarioId)

    LaunchedEffect(c.id, c.mensajes.size) {
        vm.markConversationRead(c.id)
        if (c.mensajes.isNotEmpty()) listState.animateScrollToItem(c.mensajes.size - 1)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PageHeader(c.asunto, if (esAdmin) "Funcionario: ${funcionario?.nombre ?: "—"} · ${funcionario?.cargo ?: ""}"
            else "Con ${Users.nameOf(c.adminId)}")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoConversacionChip(c)
                if (esAdmin && funcionario != null) {
                    FilledTonalButton(onClick = { launchExternal(context, dialIntent(funcionario.telefono)) }) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Llamar")
                    }
                    if (c.abierta) {
                        OutlinedButton(onClick = { confirmarCierre = true }) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Cerrar")
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(c.mensajes) { m ->
                val mio = m.autorId == user.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mio) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (mio) MpazNavy else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (mio) Color.Black else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 300.dp).animateContentSize()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(Users.nameOf(m.autorId), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = if (mio) Color(0xFF7F1D1D) else MpazNavy)
                            Text(m.texto)
                            Text(formatDate(m.fecha), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        if (c.abierta) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(500) },
                    placeholder = { Text("Escribe una respuesta…") },
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                IconButton(enabled = texto.isNotBlank(), onClick = {
                    val e = vm.reply(c.id, texto)
                    if (e == null) texto = "" else Toast.makeText(context, e, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (texto.isNotBlank()) MpazNavy else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Conversación cerrada el ${formatDate(c.fechaCierre)}.", fontWeight = FontWeight.SemiBold)
                if (!esAdmin) {
                    Text("Para continuar, envía una nueva solicitud de mensaje.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onNewRequest, modifier = Modifier.fillMaxWidth()) { Text("Nueva solicitud de mensaje") }
                }
            }
        }
    }

    if (confirmarCierre) {
        ConfirmDialog(
            title = "Cerrar mensaje",
            text = "El funcionario no podrá responder aquí y tendrá que enviar una nueva solicitud de mensaje.",
            confirmText = "Cerrar mensaje",
            onDismiss = { confirmarCierre = false },
            onConfirm = {
                confirmarCierre = false
                Toast.makeText(context, vm.closeConversation(c.id) ?: "Conversación cerrada", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AdminMessagesScreen(vm: AppViewModel, onOpenConversation: (Int) -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión como administrador.", onLogin)
    if (user.role != Role.ADMIN) {
        EmptyState(Icons.Filled.Lock, "Solo para administradores", "Esta vista está disponible para el encargado de TI.")
        return
    }
    val context = LocalContext.current
    val conversaciones by vm.conversaciones.collectAsState()
    val contactos by vm.contactos.collectAsState()
    var filtro by rememberSaveable { mutableIntStateOf(0) } // 0 todas, 1 abiertas, 2 cerradas
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }
    var cerrarId by rememberSaveable { mutableIntStateOf(-1) }

    val filtradas = conversaciones.filter {
        when (filtro) {
            1 -> it.abierta
            2 -> !it.abierta
            else -> true
        }
    }
    val cerradas = conversaciones.count { !it.abierta }
    val funcionarios = (filtradas.map { it.funcionarioId } + if (filtro == 0) contactos.map { it.funcionarioId } else emptyList())
        .distinct()
        .sortedByDescending { id ->
            maxOf(
                filtradas.filter { it.funcionarioId == id }.maxOfOrNull { it.ultimaFecha } ?: 0L,
                contactos.filter { it.funcionarioId == id }.maxOfOrNull { it.fecha } ?: 0L
            )
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Mensajes", "Mensajes, llamadas y correos de los funcionarios, agrupados por persona.") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Todos", "Abiertos", "Cerrados").forEachIndexed { i, label ->
                    FilterChip(selected = filtro == i, onClick = { filtro = i }, label = { Text(label) })
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmarBorrado = true },
                enabled = cerradas > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Text("  Borrar historial de mensajes cerrados ($cerradas)")
            }
        }
        if (funcionarios.isEmpty()) {
            item { EmptyState(Icons.Filled.Forum, "Sin mensajes", "No hay conversaciones para este filtro.") }
        }
        items(funcionarios, key = { "f$it" }) { fid ->
            val f = Users.byId(fid)
            val convs = filtradas.filter { it.funcionarioId == fid }.sortedByDescending { it.ultimaFecha }
            val llamadas = contactos.filter { it.funcionarioId == fid && it.tipo == TipoContacto.LLAMADA }
            val correos = contactos.filter { it.funcionarioId == fid && it.tipo == TipoContacto.CORREO }
            SectionCard(modifier = Modifier.animateItem()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(f?.iniciales ?: "?")
                    Column(Modifier.weight(1f)) {
                        Text(f?.nombre ?: "Funcionario", fontWeight = FontWeight.Bold)
                        Text(f?.cargo ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(f?.telefono ?: "", style = MaterialTheme.typography.labelSmall)
                    }
                    if (f != null) {
                        IconButton(onClick = { launchExternal(context, dialIntent(f.telefono)) }) {
                            Icon(Icons.Filled.Call, contentDescription = "Llamar a ${f.nombre}", tint = MpazNavy)
                        }
                    }
                }
                if (llamadas.isNotEmpty() || correos.isNotEmpty()) {
                    val ultimo = (llamadas + correos).maxOf { it.fecha }
                    Text(
                        "Intentos de contacto: ${llamadas.size} llamada(s) · ${correos.size} correo(s) · último ${formatDate(ultimo)}",
                        style = MaterialTheme.typography.bodySmall, color = MpazGold, fontWeight = FontWeight.SemiBold
                    )
                }
                convs.forEach { c ->
                    ConversationRow(
                        c = c,
                        title = c.asunto,
                        subtitle = "Para ${Users.nameOf(c.adminId)} · ${formatDate(c.ultimaFecha)}",
                        unread = c.noLeidaAdmin,
                        onClick = { onOpenConversation(c.id) },
                        trailing = {
                            Row {
                                TextButton(onClick = { onOpenConversation(c.id) }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Responder") }
                                if (c.abierta) {
                                    TextButton(onClick = { cerrarId = c.id }, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Cerrar") }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmarBorrado) {
        ConfirmDialog(
            title = "Borrar historial",
            text = "Se eliminarán $cerradas conversación(es) cerradas. Las conversaciones abiertas no se tocan.",
            confirmText = "Borrar",
            destructive = true,
            onDismiss = { confirmarBorrado = false },
            onConfirm = {
                confirmarBorrado = false
                val n = vm.clearClosedConversations()
                Toast.makeText(context, "Se borraron $n conversación(es) cerradas", Toast.LENGTH_SHORT).show()
            }
        )
    }
    if (cerrarId != -1) {
        ConfirmDialog(
            title = "Cerrar mensaje",
            text = "El funcionario tendrá que enviar una nueva solicitud de mensaje para continuar.",
            confirmText = "Cerrar mensaje",
            onDismiss = { cerrarId = -1 },
            onConfirm = {
                val id = cerrarId
                cerrarId = -1
                Toast.makeText(context, vm.closeConversation(id) ?: "Conversación cerrada", Toast.LENGTH_SHORT).show()
            }
        )
    }
}