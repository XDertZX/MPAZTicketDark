package com.fabiancg.bottomappbar.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object PdfReports {

    private const val W = 595 // A4 en puntos
    private const val H = 842
    private const val M = 40f

    private val fecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
    private fun fmt(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fecha)

    private fun hex(value: String) = android.graphics.Color.parseColor(value)

    private class Writer(private val doc: PdfDocument, private val titulo: String, private val subtitulo: String) {
        private var pageNum = 0
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f

        private val navy = Paint().apply { color = hex("#062451") }
        private val gold = Paint().apply { color = hex("#B88A00") }
        private val track = Paint().apply { color = hex("#E5E7EB") }
        private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 20f; typeface = Typeface.DEFAULT_BOLD }
        private val whiteSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 11f }
        private val goldText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hex("#E3C25C"); textSize = 13f; typeface = Typeface.DEFAULT_BOLD }
        private val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hex("#062451"); textSize = 14f; typeface = Typeface.DEFAULT_BOLD }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hex("#1F2937"); textSize = 10.5f }
        private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hex("#1F2937"); textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD }
        private val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = hex("#6B7280"); textSize = 9f }

        fun newPage() {
            finishPage()
            pageNum++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create())
            page = p
            canvas = p.canvas
            canvas.drawRect(0f, 0f, W.toFloat(), 86f, navy)
            canvas.drawRect(0f, 86f, W.toFloat(), 90f, gold)
            canvas.drawText("MPAZ Ticket", M, 36f, white)
            canvas.drawText(titulo, M, 58f, goldText)
            canvas.drawText(subtitulo, M, 75f, whiteSmall)
            canvas.drawText("Escuela Escritora Marcela Paz · Datos ficticios de demostración · Página $pageNum", M, H - 24f, muted)
            y = 110f
        }

        private fun finishPage() {
            page?.let { doc.finishPage(it) }
            page = null
        }

        private fun ensure(space: Float) {
            if (page == null || y + space > H - 50) newPage()
        }

        private fun fit(value: String, max: Int) = if (value.length <= max) value else value.take(max - 1) + "…"

        fun heading(value: String) {
            ensure(44f)
            y += 8f
            canvas.drawText(value, M, y + 14f, heading)
            y += 20f
            canvas.drawRect(M, y, M + 40f, y + 2.5f, gold)
            y += 12f
        }

        fun line(value: String, strong: Boolean = false) {
            ensure(16f)
            canvas.drawText(fit(value, 95), M, y + 11f, if (strong) bold else text)
            y += 16f
        }

        fun keyValue(key: String, value: String) {
            ensure(16f)
            canvas.drawText(key, M, y + 11f, text)
            canvas.drawText(value, M + 230f, y + 11f, bold)
            y += 16f
        }

        fun bar(label: String, value: Int, max: Int, colorHex: String) {
            ensure(20f)
            val barX = M + 150f
            val barW = W - 2 * M - 200f
            canvas.drawText(fit(label, 26), M, y + 11f, text)
            canvas.drawRect(barX, y + 3f, barX + barW, y + 13f, track)
            val ratio = if (max <= 0) 0f else value.toFloat() / max
            canvas.drawRect(barX, y + 3f, barX + barW * ratio, y + 13f, Paint().apply { color = hex(colorHex) })
            canvas.drawText(value.toString(), barX + barW + 10f, y + 12f, bold)
            y += 20f
        }

        fun row(cols: List<String>, widths: List<Float>, strong: Boolean = false) {
            ensure(15f)
            var x = M
            cols.forEachIndexed { i, c ->
                val maxChars = (widths[i] / 5.4f).toInt().coerceAtLeast(4)
                canvas.drawText(fit(c, maxChars), x, y + 10f, if (strong) bold else text)
                x += widths[i]
            }
            y += 15f
        }

        fun space(h: Float = 8f) {
            y += h
        }

        fun close() = finishPage()
    }

    private fun prioridadColor(p: Prioridad?) = when (p) {
        Prioridad.ALTA -> "#B42318"
        Prioridad.MEDIA -> "#B88A00"
        Prioridad.BAJA -> "#15803D"
        null -> "#9CA3AF"
    }

    private fun resumenBloque(w: Writer, r: ResumenPeriodo) {
        w.keyValue("Total de reportes", r.total.toString())
        w.keyValue("Pendientes (en curso)", r.pendientes.toString())
        w.keyValue("Revisados (solucionados, cerrados o cancelados)", r.revisados.toString())
        w.keyValue("Resueltos (solucionados + cerrados)", r.resueltos.toString())
        w.keyValue("Cancelados", r.cancelados.toString())
        w.keyValue("Prioridad más frecuente", r.prioridadMasFrecuente?.label ?: "—")
    }

    private fun prioridades(w: Writer, r: ResumenPeriodo) {
        val max = r.porPrioridad.maxOfOrNull { it.second } ?: 0
        r.porPrioridad.forEach { (p, n) -> w.bar(p?.let { "Prioridad ${it.label}" } ?: "Sin prioridad", n, max, prioridadColor(p)) }
    }

    private fun categorias(w: Writer, r: ResumenPeriodo) {
        val max = r.porCategoria.maxOfOrNull { it.second } ?: 0
        r.porCategoria.forEachIndexed { i, (c, n) -> w.bar("${i + 1}. ${c.label}", n, max, "#062451") }
        r.subcategoriaMasFrecuente?.let { w.line("Subcategoría más repetida: ${it.first} (${it.second})") }
    }

    private fun escribir(doc: PdfDocument, out: File): File? = try {
        out.outputStream().use { doc.writeTo(it) }
        out
    } catch (e: IOException) {
        out.delete()
        null
    } finally {
        doc.close()
    }

    fun mensual(out: File, r: ResumenPeriodo, autor: User): File? {
        val doc = PdfDocument()
        val w = Writer(doc, "Resumen mensual · ${r.titulo}", "Generado el ${fmt(System.currentTimeMillis())} por ${autor.nombre}")
        w.newPage()
        w.heading("Resumen general")
        resumenBloque(w, r)
        w.heading("Pendientes y revisados")
        val max = maxOf(r.pendientes, r.revisados)
        w.bar("Pendientes", r.pendientes, max, "#B88A00")
        w.bar("Revisados", r.revisados, max, "#062451")
        w.heading("Total de reportes por prioridad")
        prioridades(w, r)
        w.heading("Categorías que más se repiten")
        categorias(w, r)
        w.heading("Detalle de reportes del mes")
        val widths = listOf(55f, 85f, 170f, 95f, 110f)
        w.row(listOf("Código", "Fecha", "Título", "Prioridad", "Estado"), widths, strong = true)
        r.tickets.take(80).forEach {
            w.row(listOf(it.codigo, fmt(it.fechaCreacion).take(10), it.titulo, it.prioridad?.label ?: "—", it.estado.label), widths)
        }
        if (r.tickets.isEmpty()) w.line("No hay reportes registrados en este mes.")
        w.close()
        return escribir(doc, out)
    }

    fun semestral(out: File, semestre: ResumenPeriodo, bimestres: List<ResumenPeriodo>, autor: User): File? {
        val doc = PdfDocument()
        val w = Writer(doc, "Reporte semestral · ${semestre.titulo}", "Generado el ${fmt(System.currentTimeMillis())} por ${autor.nombre}")
        w.newPage()
        w.heading("Resumen del semestre")
        resumenBloque(w, semestre)
        w.heading("Comparación por bimestre")
        val max = bimestres.maxOfOrNull { it.total } ?: 0
        bimestres.forEach { w.bar(it.titulo.substringBefore(" ·"), it.total, max, "#062451") }
        bimestres.forEach { b ->
            w.heading(b.titulo)
            w.keyValue("Total", b.total.toString())
            w.keyValue("Pendientes / Revisados", "${b.pendientes} / ${b.revisados}")
            w.keyValue("Alta / Media / Baja", b.porPrioridad.filter { it.first != null }.joinToString(" / ") { it.second.toString() })
            w.keyValue("Categoría más repetida", b.porCategoria.firstOrNull()?.takeIf { it.second > 0 }?.let { "${it.first.label} (${it.second})" } ?: "—")
        }
        w.heading("Categorías del semestre")
        categorias(w, semestre)
        w.heading("Prioridades del semestre")
        prioridades(w, semestre)
        w.close()
        return escribir(doc, out)
    }

    fun exportToDownloads(context: Context, file: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, Documento.MIME_PDF)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MPAZ Ticket")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }
}
