@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fabiancg.bottomappbar.data.Catalogo
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.PhotoStorage
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.ReportDraft
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.Validators
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.DropdownField
import com.fabiancg.bottomappbar.ui.components.EmergencyBanner
import com.fabiancg.bottomappbar.ui.components.FieldMessage
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.PhotoPickerButtons
import com.fabiancg.bottomappbar.ui.components.PhotoRow
import com.fabiancg.bottomappbar.ui.components.PrioridadSelector
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.ValidatedTextField

private const val MAX_PHOTOS = 4

class ReportFormViewModel : ViewModel() {
    var categoria by mutableStateOf<Categoria?>(null)
    var subcategoria by mutableStateOf<String?>(null)
    var ubicacion by mutableStateOf<String?>(null)
    var detalle by mutableStateOf("")
    var titulo by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var gravedad by mutableStateOf<Prioridad?>(null)
    val fotos = mutableStateListOf<String>()

    val errorCategoria get() = Validators.seleccion(categoria, "Selecciona una categoría")
    val errorSubcategoria get() = Validators.seleccion(subcategoria, "Selecciona una subcategoría")
    val errorUbicacion get() = Validators.seleccion(ubicacion, "Selecciona el espacio")
    val errorTitulo get() = Validators.titulo(titulo)
    val errorDescripcion get() = Validators.descripcion(descripcion)
    val errorGravedad get() = Validators.seleccion(gravedad, "Selecciona la gravedad")
    val esValido
        get() = listOf(errorCategoria, errorSubcategoria, errorUbicacion, errorTitulo, errorDescripcion, errorGravedad)
            .all { it == null }

    fun toDraft() = ReportDraft(
        titulo = titulo, descripcion = descripcion, categoria = categoria!!, subcategoria = subcategoria!!,
        ubicacion = ubicacion!!, detalle = detalle, gravedad = gravedad!!, fotos = fotos.toList()
    )

    fun reset() {
        categoria = null; subcategoria = null; ubicacion = null
        detalle = ""; titulo = ""; descripcion = ""; gravedad = null
        fotos.clear()
    }
}


@Composable
fun ReportForm(vm: AppViewModel, form: ReportFormViewModel, onOpenTicket: (Int) -> Unit, onFinished: (() -> Unit)? = null) {
    val context: Context = LocalContext.current
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }
    var showAuth by rememberSaveable { mutableStateOf(false) }
    var codigoCreado by rememberSaveable { mutableStateOf<String?>(null) }
    var idCreado by rememberSaveable { mutableIntStateOf(-1) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun enviar() {
        val user = vm.currentUser
        if (user == null) {
            showAuth = true
            return
        }
        if (!TicketRules.canCreate(user)) {
            error = "Tu perfil (${user.role.label}) no puede crear reportes."
            return
        }
        val ticket = vm.createTicket(form.toDraft())
        if (ticket == null) {
            error = "No se pudo crear el reporte. Revisa los datos."
        } else {
            codigoCreado = ticket.codigo
            idCreado = ticket.id
            error = null
            intentoEnviar = false
            form.reset()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField(
            label = "Categoría *",
            options = Categoria.entries.toList(),
            selected = form.categoria,
            optionLabel = { it.label },
            onSelected = { form.categoria = it; form.subcategoria = null },
            isError = intentoEnviar && form.errorCategoria != null,
            errorText = form.errorCategoria,
            leadingIcon = Icons.Filled.Category,
            showValid = true
        )
        DropdownField(
            label = "Subcategoría *",
            options = form.categoria?.let { Catalogo.subcategorias[it] } ?: emptyList(),
            selected = form.subcategoria,
            optionLabel = { it },
            onSelected = { form.subcategoria = it },
            enabled = form.categoria != null,
            isError = intentoEnviar && form.errorSubcategoria != null,
            errorText = form.errorSubcategoria,
            leadingIcon = Icons.AutoMirrored.Filled.Label,
            showValid = true
        )
        DropdownField(
            label = "Ubicación en la escuela *",
            options = Catalogo.ubicaciones,
            selected = form.ubicacion,
            optionLabel = { it },
            onSelected = { form.ubicacion = it },
            isError = intentoEnviar && form.errorUbicacion != null,
            errorText = form.errorUbicacion,
            leadingIcon = Icons.Filled.Place,
            showValid = true
        )
        ValidatedTextField(
            value = form.detalle,
            onValueChange = { form.detalle = it.take(60) },
            label = "Detalle de la ubicación (opcional)",
            error = null,
            showError = false,
            leadingIcon = Icons.Filled.MeetingRoom,
            placeholder = "Ej: 5° A, segundo piso",
            showValid = false
        )
        ValidatedTextField(
            value = form.titulo,
            onValueChange = { form.titulo = it.take(80) },
            label = "Título *",
            error = form.errorTitulo,
            showError = intentoEnviar || form.titulo.isNotEmpty(),
            leadingIcon = Icons.Filled.Title,
            helper = "${form.titulo.length}/80"
        )
        ValidatedTextField(
            value = form.descripcion,
            onValueChange = { form.descripcion = it.take(500) },
            label = "Descripción del problema *",
            error = form.errorDescripcion,
            showError = intentoEnviar || form.descripcion.isNotEmpty(),
            leadingIcon = Icons.Filled.Description,
            helper = "${form.descripcion.length}/500",
            singleLine = false,
            minLines = 4
        )

        Text("Gravedad del problema *", fontWeight = FontWeight.SemiBold)
        PrioridadSelector(selected = form.gravedad, onSelect = { form.gravedad = it })
        FieldMessage(
            error = if (intentoEnviar) form.errorGravedad else null,
            helper = "El encargado de TI confirmará la prioridad final."
        )

        Text("Fotografías (opcional, máximo $MAX_PHOTOS)", fontWeight = FontWeight.SemiBold)
        Text(
            "Evita fotografiar rostros, documentos o datos personales.",
            style = MaterialTheme.typography.bodySmall
        )
        PhotoPickerButtons(
            onPhotoReady = { if (form.fotos.size < MAX_PHOTOS) form.fotos.add(it) },
            enabled = form.fotos.size < MAX_PHOTOS
        )
        AnimatedVisibility(form.fotos.isNotEmpty()) {
            PhotoRow(
                photos = form.fotos.toList(),
                onRemove = { name ->
                    form.fotos.remove(name)
                    PhotoStorage.delete(context, name)
                }
            )
        }

        AnimatedVisibility(error != null) {
            FieldMessage(error = error, helper = null)
        }
        AnimatedVisibility(intentoEnviar && !form.esValido) {
            FieldMessage(error = "Revisa los campos marcados en rojo.", helper = null)
        }

        Button(
            onClick = {
                intentoEnviar = true
                if (form.esValido) enviar()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Text("  Enviar reporte")
        }
    }

    if (showAuth) {
        AuthDialog(
            vm = vm,
            title = "Para enviar tu reporte",
            message = "Regístrate como funcionario o inicia sesión (funcionario o administrador). " +
                    "Tu reporte se enviará automáticamente.",
            onDismiss = { showAuth = false },
            onAuthenticated = {
                showAuth = false
                enviar()
            }
        )
    }

    codigoCreado?.let { codigo ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Reporte enviado") },
            text = {
                Text(
                    "Tu solicitud quedó registrada con el número $codigo y estado «Pendiente». " +
                            "Podrás ver su avance y los comentarios en «Mis reportes»."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = idCreado
                    codigoCreado = null
                    onOpenTicket(id)
                }) { Text("Ver reporte") }
            },
            dismissButton = {
                TextButton(onClick = {
                    codigoCreado = null
                    onFinished?.invoke()
                }) { Text("Aceptar") }
            }
        )
    }
}

@Composable
fun CreateTicketScreen(vm: AppViewModel, onOpenTicket: (Int) -> Unit, onFinished: () -> Unit) {
    val form: ReportFormViewModel = viewModel(key = "create_report_form")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader("Crear reporte", "Completa los datos del problema. Los campos con * son obligatorios.")
        EmergencyBanner()
        SectionCard("Datos del reporte", icon = Icons.Filled.AddCircle) {
            if (vm.currentUser == null) {
                Text(
                    "Aún no inicias sesión: al enviar se te pedirá registrarte o ingresar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ReportForm(vm = vm, form = form, onOpenTicket = onOpenTicket, onFinished = onFinished)
        }
    }
}
