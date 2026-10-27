@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.DocumentRepository
import com.fabiancg.bottomappbar.data.Documento
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.AuthRequired
import com.fabiancg.bottomappbar.ui.components.ConfirmDialog
import com.fabiancg.bottomappbar.ui.components.EmptyState
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.formatDate
import com.fabiancg.bottomappbar.ui.components.formatSize
import com.fabiancg.bottomappbar.ui.components.launchExternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private val PdfRed = Color(0xFFB42318)
private val WordBlue = Color(0xFF2B579A)

private fun openWithExternalApp(context: Context, doc: Documento) {
    val file = DocumentRepository.fileFor(context, doc.archivo)
    if (!file.exists()) {
        Toast.makeText(context, "El archivo ya no existe en el dispositivo.", Toast.LENGTH_LONG).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(DocumentRepository.uriFor(context, doc), doc.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    launchExternal(
        context, Intent.createChooser(intent, "Abrir ${doc.tipoLabel} con…"),
        if (doc.esPdf) "No hay una app para abrir PDF." else "Instala Word o Google Docs para abrir archivos Word."
    )
}

@Composable
fun DocumentsScreen(vm: AppViewModel, onOpenViewer: (Int) -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión para administrar tus documentos.", onLogin)
    val context = LocalContext.current
    val documentos by vm.documentos.collectAsState()
    val mios = documentos.filter { it.ownerId == user.id }.sortedByDescending { it.fecha }
    var eliminarId by rememberSaveable { mutableIntStateOf(-1) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val (doc, error) = vm.importDocument(context, uri)
            Toast.makeText(context, error ?: "«${doc?.nombre}» cargado", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageHeader("Mis documentos", "Carga, visualiza y elimina archivos PDF o Word (en un futuro: rúbricas).") }
        item {
            Button(onClick = { picker.launch(Documento.permitidos) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Text("  Cargar documento (PDF o Word)")
            }
        }
        item {
            Text(
                "Máximo 15 MB. Los archivos se guardan en el almacenamiento privado de la app. " +
                        "Los PDF se ven dentro de la app; los Word se abren con una app compatible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (mios.isEmpty()) {
            item { EmptyState(Icons.Filled.FolderOpen, "Sin documentos", "Carga un PDF o Word para verlo aquí.") }
        }
        items(mios, key = { it.id }) { doc ->
            DocumentCard(
                doc = doc,
                onView = { if (doc.esPdf) onOpenViewer(doc.id) else openWithExternalApp(context, doc) },
                onDelete = { eliminarId = doc.id },
                modifier = Modifier.animateItem()
            )
        }
    }

    mios.firstOrNull { it.id == eliminarId }?.let { doc ->
        ConfirmDialog(
            title = "Eliminar documento",
            text = "Se eliminará «${doc.nombre}» de este dispositivo. Esta acción no se puede deshacer.",
            confirmText = "Eliminar",
            destructive = true,
            onDismiss = { eliminarId = -1 },
            onConfirm = {
                eliminarId = -1
                Toast.makeText(context, vm.deleteDocument(context, doc.id) ?: "Documento eliminado", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DocumentCard(doc: Documento, onView: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (doc.esPdf) PdfRed else WordBlue
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (doc.esPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Description, contentDescription = null, tint = color)
                }
                Column(Modifier.weight(1f)) {
                    Text(doc.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${doc.tipoLabel} · ${formatSize(doc.tamano)} · ${formatDate(doc.fecha)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(doc.origen, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(" Eliminar", color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = onView) {
                    Icon(if (doc.esPdf) Icons.Filled.Visibility else Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Text(if (doc.esPdf) " Ver" else " Abrir")
                }
            }
        }
    }
}

private const val MAX_PAGES = 12
private const val PAGE_WIDTH_PX = 900

private sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(val pages: List<ImageBitmap>, val total: Int) : PdfState
    data class Error(val message: String) : PdfState
}

private fun renderPdf(file: File): PdfState {
    if (!file.exists()) return PdfState.Error("El archivo no existe.")
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                val total = renderer.pageCount
                val pages = (0 until minOf(total, MAX_PAGES)).map { i ->
                    val page = renderer.openPage(i)
                    try {
                        val height = (PAGE_WIDTH_PX.toFloat() / page.width * page.height).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(PAGE_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp.asImageBitmap()
                    } finally {
                        page.close()
                    }
                }
                PdfState.Ready(pages, total)
            } finally {
                renderer.close()
            }
        }
    } catch (e: IOException) {
        PdfState.Error("No se pudo abrir el PDF.")
    } catch (e: SecurityException) {
        PdfState.Error("El PDF está protegido.")
    } catch (e: OutOfMemoryError) {
        PdfState.Error("El PDF es demasiado grande para mostrarlo aquí. Ábrelo con otra app.")
    }
}

@Composable
fun DocViewerScreen(vm: AppViewModel, docId: Int) {
    val context = LocalContext.current
    val documentos by vm.documentos.collectAsState()
    val doc = documentos.firstOrNull { it.id == docId && it.ownerId == vm.currentUser?.id }
    if (doc == null) {
        EmptyState(Icons.Filled.FolderOpen, "Documento no disponible", "Puede que haya sido eliminado.")
        return
    }
    val state by produceState<PdfState>(PdfState.Loading, doc.archivo) {
        value = withContext(Dispatchers.IO) { renderPdf(DocumentRepository.fileFor(context, doc.archivo)) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F7)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PageHeader(doc.nombre, "${doc.tipoLabel} · ${formatSize(doc.tamano)}")
                OutlinedButton(onClick = { openWithExternalApp(context, doc) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Text("  Abrir con otra app")
                }
            }
        }
        when (val s = state) {
            PdfState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is PdfState.Error -> item { Text(s.message, color = MaterialTheme.colorScheme.error) }
            is PdfState.Ready -> {
                itemsIndexed(s.pages) { i, page ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Image(
                            bitmap = page,
                            contentDescription = "Página ${i + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD5DAE3))
                        )
                        Text("Página ${i + 1} de ${s.total}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (s.total > MAX_PAGES) {
                    item { Text("Se muestran las primeras $MAX_PAGES páginas. Abre el archivo con otra app para verlo completo.") }
                }
            }
        }
    }
}
