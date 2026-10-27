package com.fabiancg.bottomappbar.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.PhotoStorage
import com.fabiancg.bottomappbar.data.Prioridad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

fun formatDate(millis: Long?): String =
    if (millis == null) "—"
    else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)


@Composable
fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = background, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EstadoChip(estado: EstadoTicket) {
    val (bg, fg) = when (estado) {
        EstadoTicket.PENDIENTE -> Color(0xFFFFF3CD) to Color(0xFF7A5200)
        EstadoTicket.REVISADO -> Color(0xFFDCEBFA) to Color(0xFF0B3D75)
        EstadoTicket.ASIGNADO -> Color(0xFFE3E0F8) to Color(0xFF3B2F8F)
        EstadoTicket.EN_PROCESO -> Color(0xFFFFE0C2) to Color(0xFF8A3B00)
        EstadoTicket.SOLUCIONADO -> Color(0xFFD4EDDA) to Color(0xFF14532D)
        EstadoTicket.CERRADO -> Color(0xFFE2E3E5) to Color(0xFF3C4043)
        EstadoTicket.CANCELADO -> Color(0xFFF8D7DA) to Color(0xFF7A1C25)
    }
    Pill(estado.label, bg, fg)
}

@Composable
fun PrioridadChip(prioridad: Prioridad?) {
    val (bg, fg) = when (prioridad) {
        Prioridad.URGENTE -> Color(0xFFF8D7DA) to Color(0xFF7A1C25)
        Prioridad.ALTA -> Color(0xFFFFE0C2) to Color(0xFF8A3B00)
        Prioridad.MEDIA -> Color(0xFFFFF3CD) to Color(0xFF7A5200)
        Prioridad.BAJA -> Color(0xFFD4EDDA) to Color(0xFF14532D)
        null -> Color(0xFFE2E3E5) to Color(0xFF3C4043)
    }
    Pill(if (prioridad == null) "Sin prioridad" else "Prioridad ${prioridad.label}", bg, fg)
}


@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            supportingText = if (isError && errorText != null) {
                { Text(errorText) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente que recibe el toque y abre el menú
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun <T> FilterDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelected: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "Todos",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { onSelected(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}



@Composable
fun NoteDialog(
    title: String,
    label: String,
    confirmText: String,
    required: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                label = { Text(label) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = !required || text.isNotBlank(), onClick = { onConfirm(text.trim()) }) {
                Text(confirmText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}



@Composable
fun PhotoThumb(
    name: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 512,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    // Se decodifica en un hilo de fondo para no congelar la pantalla
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxSide) {
        value = withContext(Dispatchers.IO) {
            PhotoStorage.loadBitmap(context, name, maxSide)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = "Fotografía adjunta",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Filled.Image, contentDescription = null)
        }
    }
}


@Composable
fun PhotoRow(photos: List<String>, onRemove: ((String) -> Unit)? = null) {
    var viewing by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photos.forEach { name ->
            Box {
                PhotoThumb(name = name, modifier = Modifier.size(96.dp), onClick = { viewing = name })
                if (onRemove != null) {
                    IconButton(onClick = { onRemove(name) }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Quitar fotografía",
                            tint = Color.White,
                            modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
    viewing?.let { name ->
        Dialog(onDismissRequest = { viewing = null }) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoThumb(
                        name = name,
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        maxSide = 1600,
                        contentScale = ContentScale.Fit
                    )
                    TextButton(onClick = { viewing = null }, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
                }
            }
        }
    }
}


@Composable
fun PhotoPickerButtons(onPhotoReady: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val context = LocalContext.current
    // Si Android destruye la pantalla mientras la cámara está abierta, no perdemos el nombre del archivo
    var pendingName by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val name = pendingName
        pendingName = null
        if (name != null) {
            if (success) onPhotoReady(name) else PhotoStorage.delete(context, name)
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val name = PhotoStorage.copyFromUri(context, uri)
            if (name != null) onPhotoReady(name)
            else Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
        }
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            enabled = enabled,
            onClick = {
                try {
                    val (name, uri) = PhotoStorage.newCameraTarget(context)
                    pendingName = name
                    takePicture.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Este dispositivo no tiene app de cámara.", Toast.LENGTH_LONG).show()
                }
            }
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Tomar foto")
        }
        OutlinedButton(
            enabled = enabled,
            onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Galería")
        }
    }
}
