package com.fabiancg.bottomappbar.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.DropdownField
import com.fabiancg.bottomappbar.ui.components.PhotoPickerButtons
import com.fabiancg.bottomappbar.ui.components.PhotoRow

private const val MAX_PHOTOS = 4


class CreateTicketFormViewModel : ViewModel() {
    var categoria by mutableStateOf<Categoria?>(null)
    var subcategoria by mutableStateOf<String?>(null)
    var ubicacion by mutableStateOf<String?>(null)
    var detalle by mutableStateOf("")
    var titulo by mutableStateOf("")
    var descripcion by mutableStateOf("")
    val fotos = mutableStateListOf<String>()

    val tituloValido get() = titulo.trim().length >= 5
    val descripcionValida get() = descripcion.trim().length >= 10
    val esValido get() = categoria != null && subcategoria != null && ubicacion != null &&
            tituloValido && descripcionValida
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTicketScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    form: CreateTicketFormViewModel = viewModel()
) {
    val context: Context = LocalContext.current
    var intentoEnviar by rememberSaveable { mutableStateOf(false) }
    var codigoCreado by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    "Si hay un riesgo inmediato, avisa por los canales de emergencia de la escuela: " +
                            "esta app no los reemplaza.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DropdownField(
                label = "Categoría *",
                options = Categoria.entries.toList(),
                selected = form.categoria,
                optionLabel = { it.label },
                onSelected = { form.categoria = it; form.subcategoria = null },
                isError = intentoEnviar && form.categoria == null,
                errorText = "Selecciona una categoría"
            )
            DropdownField(
                label = "Subcategoría *",
                options = form.categoria?.let { Catalogo.subcategorias[it] } ?: emptyList(),
                selected = form.subcategoria,
                optionLabel = { it },
                onSelected = { form.subcategoria = it },
                enabled = form.categoria != null,
                isError = intentoEnviar && form.subcategoria == null,
                errorText = "Selecciona una subcategoría"
            )
            DropdownField(
                label = "Ubicación en la escuela *",
                options = Catalogo.ubicaciones,
                selected = form.ubicacion,
                optionLabel = { it },
                onSelected = { form.ubicacion = it },
                isError = intentoEnviar && form.ubicacion == null,
                errorText = "Selecciona el espacio"
            )
            OutlinedTextField(
                value = form.detalle,
                onValueChange = { form.detalle = it.take(60) },
                label = { Text("Detalle de la ubicación (ej: 5° A, segundo piso)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.titulo,
                onValueChange = { form.titulo = it.take(80) },
                label = { Text("Título *") },
                singleLine = true,
                isError = intentoEnviar && !form.tituloValido,
                supportingText = if (intentoEnviar && !form.tituloValido) {
                    { Text("Mínimo 5 caracteres") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.descripcion,
                onValueChange = { form.descripcion = it.take(500) },
                label = { Text("Descripción del problema *") },
                minLines = 4,
                isError = intentoEnviar && !form.descripcionValida,
                supportingText = {
                    Text(
                        if (intentoEnviar && !form.descripcionValida) "Mínimo 10 caracteres"
                        else "${form.descripcion.length}/500"
                    )
                },
                modifier = Modifier.fillMaxWidth()
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
            if (form.fotos.isNotEmpty()) {
                PhotoRow(
                    photos = form.fotos.toList(),
                    onRemove = { name ->
                        form.fotos.remove(name)
                        PhotoStorage.delete(context, name)
                    }
                )
            }

            Button(
                onClick = {
                    intentoEnviar = true
                    if (form.esValido) {
                        val ticket = vm.createTicket(
                            titulo = form.titulo,
                            descripcion = form.descripcion,
                            categoria = form.categoria!!,
                            subcategoria = form.subcategoria!!,
                            ubicacion = form.ubicacion!!,
                            detalle = form.detalle,
                            fotos = form.fotos.toList()
                        )
                        codigoCreado = ticket?.codigo
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text("  Enviar ticket")
            }
        }
    }

    codigoCreado?.let { codigo ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ticket creado") },
            text = {
                Text(
                    "Tu solicitud quedó registrada con el número $codigo y estado «Pendiente». " +
                            "Podrás ver su avance y los comentarios en «Mis solicitudes»."
                )
            },
            confirmButton = { TextButton(onClick = onCreated) { Text("Aceptar") } }
        )
    }
}
