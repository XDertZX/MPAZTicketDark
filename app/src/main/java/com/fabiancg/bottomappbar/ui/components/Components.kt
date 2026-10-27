@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.PhotoStorage
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.SolicitudInfo
import com.fabiancg.bottomappbar.data.Validators
import com.fabiancg.bottomappbar.ui.theme.DangerBg
import com.fabiancg.bottomappbar.ui.theme.DangerFg
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazGoldLight
import com.fabiancg.bottomappbar.ui.theme.MpazNavy
import com.fabiancg.bottomappbar.ui.theme.SuccessFg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

fun formatDate(millis: Long?): String =
    if (millis == null) "—"
    else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)

fun formatDuration(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "$h h $m min"
        h > 0 -> "$h h"
        else -> "$m min"
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

@Composable
fun rememberNow(periodMs: Long = 30_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun launchExternal(context: Context, intent: Intent, errorMessage: String = "No hay una aplicación para realizar esta acción.") {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }
}


@Composable
fun PageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MpazNavy)
        Box(
            Modifier
                .width(44.dp)
                .height(3.dp)
                .background(MpazGold, RoundedCornerShape(2.dp))
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmergencyBanner(modifier: Modifier = Modifier) {
    Surface(
        color = DangerBg,
        contentColor = DangerFg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Importante", fontWeight = FontWeight.Bold)
                Text(
                    "Esta aplicación no reemplaza los procedimientos de emergencia de la escuela. " +
                            "Ante un riesgo inmediato, avisa por los canales definidos por el establecimiento.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon != null) Icon(icon, contentDescription = null, tint = MpazNavy)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MpazNavy)
                }
            }
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
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
fun Avatar(initials: String, modifier: Modifier = Modifier, background: Color = MpazNavy, foreground: Color = Color.Black) {    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = foreground, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MpazNavy,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = accent)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BarRow(label: String, count: Int, max: Int, color: Color, highlight: Boolean = false) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val target = if (max <= 0) 0f else count.toFloat() / max
    val progress by animateFloatAsState(if (started) target else 0f, tween(800), label = "bar")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                label + if (highlight) "  ★ más frecuente" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
            )
            Text("$count", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            drawStopIndicator = {}
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AuthRequired(message: String, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MpazNavy, modifier = Modifier.size(52.dp))
        Text("Necesitas iniciar sesión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión o registrarse") }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.textButtonColors()
            ) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


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
        EstadoTicket.PENDIENTE -> Color(0xFF3A3012) to Color(0xFFFACC15)
        EstadoTicket.REVISADO -> Color(0xFF12283B) to Color(0xFF7DD3FC)
        EstadoTicket.ASIGNADO -> Color(0xFF241F3F) to Color(0xFFC4B5FD)
        EstadoTicket.EN_PROCESO -> Color(0xFF3B2412) to Color(0xFFFDBA74)
        EstadoTicket.SOLUCIONADO -> Color(0xFF12301F) to Color(0xFF4ADE80)
        EstadoTicket.CERRADO -> Color(0xFF26292B) to Color(0xFF9CA3AF)
        EstadoTicket.CANCELADO -> Color(0xFF3B1418) to Color(0xFFF87171)
    }
    Pill(estado.label, bg, fg)
}

fun prioridadColors(prioridad: Prioridad?): Pair<Color, Color> = when (prioridad) {
    Prioridad.ALTA -> Color(0xFF3B1418) to Color(0xFFF87171)   // rojo
    Prioridad.MEDIA -> Color(0xFF3A3012) to Color(0xFFFACC15)  // amarillo
    Prioridad.BAJA -> Color(0xFF12301F) to Color(0xFF4ADE80)   // verde claro
    null -> Color(0xFF26292B) to Color(0xFF9CA3AF)
}

fun prioridadBarColor(prioridad: Prioridad?): Color = when (prioridad) {
    Prioridad.ALTA -> Color(0xFFEF4444)
    Prioridad.MEDIA -> Color(0xFFFACC15)
    Prioridad.BAJA -> Color(0xFF4ADE80)
    null -> Color(0xFF6B7280)
}

@Composable
fun PrioridadChip(prioridad: Prioridad?) {
    val (bg, fg) = prioridadColors(prioridad)
    Pill(if (prioridad == null) "Sin prioridad TI" else "Prioridad ${prioridad.label}", bg, fg)
}

@Composable
fun GravedadChip(gravedad: Prioridad) {
    val (bg, fg) = prioridadColors(gravedad)
    Pill("Gravedad ${gravedad.label}", bg.copy(alpha = 0.6f), fg)
}

@Composable
fun InfoRequestBanner(solicitud: SolicitudInfo, modifier: Modifier = Modifier) {
    val now = rememberNow()
    val vencida = solicitud.vencida(now)
    Surface(
        color = if (vencida) DangerBg else MpazGoldLight,
        contentColor = if (vencida) DangerFg else Color(0xFFFCA5A5),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("TI solicita más información", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(
                if (vencida) "Plazo vencido (máximo ${solicitud.plazoHoras} h)"
                else "Responde dentro de ${formatDuration(solicitud.vence - now)} (máximo ${solicitud.plazoHoras} h)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PrioridadSelector(selected: Prioridad?, onSelect: (Prioridad) -> Unit, modifier: Modifier = Modifier) {
    val options = Prioridad.entries
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, p ->
            SegmentedButton(
                selected = selected == p,
                onClick = { onSelect(p) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = prioridadColors(p).first,
                    activeContentColor = prioridadColors(p).second,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface
                ),
                label = { Text(p.label) }
            )
        }
    }
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
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    showValid: Boolean = false
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
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null) }
            } else null,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isError) {
                        Icon(Icons.Filled.Error, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error)
                    } else if (showValid && selected != null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Campo válido", tint = SuccessFg)
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            supportingText = if (isError && errorText != null) {
                { Text(errorText) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true })
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
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = true })
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
    onConfirm: (String) -> Unit,
    helper: String? = null,
    minChars: Int = 1
) {
    var text by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    val error = when {
        !required -> null
        minChars > 1 -> Validators.minimo(text, minChars)
        else -> Validators.obligatorio(text)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (helper != null) Text(helper, style = MaterialTheme.typography.bodySmall)
                ValidatedTextField(
                    value = text,
                    onValueChange = { text = it.take(300) },
                    label = label,
                    error = error,
                    showError = intento || text.isNotEmpty(),
                    singleLine = false,
                    minLines = 3,
                    helper = "${text.length}/300",
                    showValid = required
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intento = true
                if (error == null) onConfirm(text.trim())
            }) {
                Text(confirmText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    showError: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    helper: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showValid: Boolean = true,
    placeholder: String? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val mostrarError = showError && error != null
    val mostrarValido = showValid && error == null && value.isNotBlank()
    val tieneIconoFinal = mostrarError || mostrarValido || isPassword
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null) }
        } else null,
        trailingIcon = if (tieneIconoFinal) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mostrarError) {
                        Icon(Icons.Filled.Error, contentDescription = "Campo con error", tint = MaterialTheme.colorScheme.error)
                    } else if (mostrarValido) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Campo válido", tint = SuccessFg)
                    }
                    if (isPassword) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    } else {
                        Spacer(Modifier.width(12.dp))
                    }
                }
            }
        } else null,
        isError = mostrarError,
        supportingText = if (mostrarError) {
            { Text(error.orEmpty()) }
        } else if (helper != null) {
            { Text(helper) }
        } else null,
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
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
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxSide) {
        value = withContext(Dispatchers.IO) {
            PhotoStorage.loadBitmap(context, name, maxSide)?.asImageBitmap()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
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

@Composable
fun FieldMessage(error: String?, helper: String?, modifier: Modifier = Modifier) {
    if (error != null) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    } else if (helper != null) {
        Text(helper, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
