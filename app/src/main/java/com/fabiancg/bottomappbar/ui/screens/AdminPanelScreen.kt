@file:OptIn(ExperimentalMaterial3Api::class)

package com.fabiancg.bottomappbar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiancg.bottomappbar.data.DocumentRepository
import com.fabiancg.bottomappbar.data.Documento
import com.fabiancg.bottomappbar.data.PdfReports
import com.fabiancg.bottomappbar.data.ResumenPeriodo
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.Stats
import com.fabiancg.bottomappbar.ui.AppViewModel
import com.fabiancg.bottomappbar.ui.components.AuthRequired
import com.fabiancg.bottomappbar.ui.components.BarRow
import com.fabiancg.bottomappbar.ui.components.EmptyState
import com.fabiancg.bottomappbar.ui.components.PageHeader
import com.fabiancg.bottomappbar.ui.components.SectionCard
import com.fabiancg.bottomappbar.ui.components.StatTile
import com.fabiancg.bottomappbar.ui.components.launchExternal
import com.fabiancg.bottomappbar.ui.components.prioridadBarColor
import com.fabiancg.bottomappbar.ui.theme.MpazGold
import com.fabiancg.bottomappbar.ui.theme.MpazNavy
import com.fabiancg.bottomappbar.ui.theme.SuccessFg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

private data class PdfResult(val doc: Documento?, val downloadUri: Uri?, val error: String?)

@Composable
fun AdminPanelScreen(vm: AppViewModel, onOpenDocument: (Int) -> Unit, onLogin: () -> Unit) {
    val user = vm.currentUser ?: return AuthRequired("Inicia sesión como administrador.", onLogin)
    if (user.role != Role.ADMIN) {
        EmptyState(Icons.Filled.Lock, "Solo para administradores", "Esta vista está disponible para el encargado de TI.")
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tickets by vm.tickets.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) } // 0 mensual, 1 semestral
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val hoy = YearMonth.now()
    var semestre by rememberSaveable { mutableIntStateOf(if (hoy.monthValue <= 6) 1 else 2) }
    var generando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<PdfResult?>(null) }

    val ym = hoy.minusMonths(monthOffset.toLong())
    val mensual = Stats.mes(tickets, ym)
    val resumenSemestre = Stats.semestre(tickets, hoy.year, semestre)
    val bimestres = Stats.bimestres(tickets, hoy.year, semestre)

    fun generarPdf(esMensual: Boolean) {
        generando = true
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                val prefijo = if (esMensual) "Resumen_mensual" else "Reporte_semestral"
                val file = DocumentRepository.newGeneratedFile(context, prefijo)
                val ok = if (esMensual) PdfReports.mensual(file, mensual, user)
                else PdfReports.semestral(file, resumenSemestre, bimestres, user)
                if (ok == null) {
                    PdfResult(null, null, "No se pudo generar el PDF.")
                } else {
                    val titulo = if (esMensual) "Resumen mensual ${mensual.titulo}" else "Reporte ${resumenSemestre.titulo}"
                    val nombre = "MPAZ_${titulo.replace(' ', '_')}.pdf"
                    val uri = PdfReports.exportToDownloads(context, ok, nombre)
                    PdfResult(DocumentRepository.registerGenerated(context, user, ok, "$titulo.pdf"), uri, null)
                }
            }
            generando = false
            resultado = res
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader("Panel de administrador", "Estadísticas de reportes (datos ficticios) y descarga de informes en PDF.")

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = tab == 0, onClick = { tab = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2),
                label = { Text("Mensual") })
            SegmentedButton(selected = tab == 1, onClick = { tab = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2),
                label = { Text("Semestral") })
        }

        AnimatedContent(
            targetState = tab,
            transitionSpec = { (fadeIn() + slideInHorizontally { if (targetState > initialState) it / 6 else -it / 6 }) togetherWith fadeOut() },
            label = "panel_tab"
        ) { t ->
            if (t == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 11) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
                        }
                        Text(mensual.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MpazNavy, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = { monthOffset-- }, enabled = monthOffset > 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
                        }
                    }
                    ResumenContenido(mensual)
                    Button(onClick = { generarPdf(true) }, enabled = !generando, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Text("  Descargar PDF mensual")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = semestre == 1, onClick = { semestre = 1 },
                            label = { Text("1er semestre (Ene–Jun)") }, leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) })
                        FilterChip(selected = semestre == 2, onClick = { semestre = 2 },
                            label = { Text("2do semestre (Jul–Dic)") }, leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) })
                    }
                    Text(resumenSemestre.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MpazNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Total", "${resumenSemestre.total}", Modifier.weight(1f), icon = Icons.Filled.Inbox)
                        StatTile("Pendientes", "${resumenSemestre.pendientes}", Modifier.weight(1f), accent = MpazGold, icon = Icons.Filled.HourglassTop)
                        StatTile("Revisados", "${resumenSemestre.revisados}", Modifier.weight(1f), accent = SuccessFg, icon = Icons.Filled.CheckCircle)
                    }
                    SectionCard("Comparación bimestral", icon = Icons.Filled.Assessment) {
                        val max = bimestres.maxOfOrNull { it.total } ?: 0
                        bimestres.forEach { b -> BarRow(b.titulo, b.total, max, MpazNavy) }
                    }
                    bimestres.forEach { b ->
                        SectionCard(b.titulo, icon = Icons.Filled.DateRange) {
                            Text("Total: ${b.total} · Pendientes: ${b.pendientes} · Revisados: ${b.revisados}")
                            Text(
                                "Prioridades → " + b.porPrioridad.joinToString(" · ") { (p, n) -> "${p?.label ?: "Sin prioridad"}: $n" },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Categoría más repetida: " + (b.porCategoria.firstOrNull()?.takeIf { it.second > 0 }
                                    ?.let { "${it.first.label} (${it.second})" } ?: "—"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    SectionCard("Categorías del semestre", icon = Icons.Filled.Category) {
                        CategoriaRanking(resumenSemestre)
                    }
                    Button(onClick = { generarPdf(false) }, enabled = !generando, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Text("  Descargar PDF semestral")
                    }
                }
            }
        }

        if (generando) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Generando PDF…")
            }
        }
    }

    resultado?.let { r ->
        AlertDialog(
            onDismissRequest = { resultado = null },
            icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
            title = { Text(if (r.error == null) "PDF generado" else "Error") },
            text = {
                Text(
                    r.error ?: ("El informe se guardó en «Mis documentos»" +
                            (if (r.downloadUri != null) " y en la carpeta Descargas/MPAZ Ticket del teléfono." else ".") )
                )
            },
            confirmButton = {
                if (r.doc != null) {
                    TextButton(onClick = {
                        resultado = null
                        onOpenDocument(r.doc.id)
                    }) { Text("Ver en la app") }
                }
            },
            dismissButton = {
                Row {
                    if (r.downloadUri != null) {
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(r.downloadUri, Documento.MIME_PDF)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            launchExternal(context, intent, "No hay una app para abrir PDF. Usa «Ver en la app».")
                        }) { Text("Abrir con…") }
                    }
                    TextButton(onClick = { resultado = null }) { Text("Cerrar") }
                }
            }
        )
    }
}

@Composable
private fun ResumenContenido(r: ResumenPeriodo) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Reportes", "${r.total}", Modifier.weight(1f), icon = Icons.Filled.Inbox)
            StatTile("Pendientes", "${r.pendientes}", Modifier.weight(1f), accent = MpazGold, icon = Icons.Filled.HourglassTop)
            StatTile("Revisados", "${r.revisados}", Modifier.weight(1f), accent = SuccessFg, icon = Icons.Filled.CheckCircle)
        }
        SectionCard("Resumen mensual: pendientes y revisados", icon = Icons.Filled.Assessment) {
            val max = maxOf(r.pendientes, r.revisados)
            BarRow("Pendientes (en curso)", r.pendientes, max, MpazGold)
            BarRow("Revisados (solucionados, cerrados o cancelados)", r.revisados, max, MpazNavy)
            Text("Resueltos: ${r.resueltos} · Cancelados: ${r.cancelados}", style = MaterialTheme.typography.bodySmall)
        }
        SectionCard("Total de reportes por prioridad", icon = Icons.Filled.Assessment) {
            val max = r.porPrioridad.maxOfOrNull { it.second } ?: 0
            val top = r.prioridadMasFrecuente
            r.porPrioridad.forEach { (p, n) ->
                BarRow(p?.let { "Prioridad ${it.label}" } ?: "Sin prioridad", n, max, prioridadBarColor(p), highlight = p != null && p == top)
            }
        }
        SectionCard("Categorías que más se repiten", icon = Icons.Filled.Category) {
            CategoriaRanking(r)
        }
    }
}

@Composable
private fun CategoriaRanking(r: ResumenPeriodo) {
    val max = r.porCategoria.maxOfOrNull { it.second } ?: 0
    r.porCategoria.forEachIndexed { i, (c, n) ->
        BarRow("${i + 1}. ${c.label}", n, max, MpazNavy, highlight = i == 0 && n > 0)
    }
    r.subcategoriaMasFrecuente?.let {
        Text("Subcategoría más repetida: ${it.first} (${it.second})", style = MaterialTheme.typography.bodySmall)
    }
}
