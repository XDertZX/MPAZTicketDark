package com.fabiancg.bottomappbar.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

object DocumentRepository {

    private const val MAX_BYTES = 15L * 1024 * 1024 // 15 MB

    private val _documentos = MutableStateFlow<List<Documento>>(emptyList())
    val documentos: StateFlow<List<Documento>> = _documentos.asStateFlow()

    private fun dir(context: Context): File = File(context.filesDir, "docs").also { it.mkdirs() }

    fun fileFor(context: Context, archivo: String): File = File(dir(context), File(archivo).name)

    fun uriFor(context: Context, doc: Documento): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileFor(context, doc.archivo))

    private fun nextId(): Int = (_documentos.value.maxOfOrNull { it.id } ?: 0) + 1

    private fun mimeFromName(name: String): String? = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> Documento.MIME_PDF
        "doc" -> Documento.MIME_DOC
        "docx" -> Documento.MIME_DOCX
        else -> null
    }

    fun importFromUri(context: Context, owner: User, uri: Uri): Pair<Documento?, String?> {
        val resolver = context.contentResolver
        var nombre = "documento"
        var tamano = -1L
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val iName = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val iSize = c.getColumnIndex(OpenableColumns.SIZE)
                    if (iName >= 0 && !c.isNull(iName)) nombre = c.getString(iName)
                    if (iSize >= 0 && !c.isNull(iSize)) tamano = c.getLong(iSize)
                }
            }
        } catch (e: SecurityException) {
            return null to "No se pudo leer el archivo seleccionado."
        }

        val mime = resolver.getType(uri)?.takeIf { it in Documento.permitidos } ?: mimeFromName(nombre)
            ?: return null to "Solo se permiten archivos PDF o Word (.pdf, .doc, .docx)."
        if (tamano > MAX_BYTES) return null to "El archivo supera el máximo de 15 MB."

        val extension = when (mime) {
            Documento.MIME_PDF -> "pdf"
            Documento.MIME_DOC -> "doc"
            else -> "docx"
        }
        val archivo = "DOC_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
        val destino = fileFor(context, archivo)
        return try {
            val input = resolver.openInputStream(uri) ?: return null to "No se pudo abrir el archivo."
            input.use { src -> destino.outputStream().use { out -> src.copyTo(out) } }
            if (destino.length() > MAX_BYTES) {
                destino.delete()
                return null to "El archivo supera el máximo de 15 MB."
            }
            val doc = Documento(
                id = nextId(), ownerId = owner.id, nombre = nombre.take(80), archivo = archivo,
                mime = mime, tamano = destino.length(), fecha = System.currentTimeMillis()
            )
            _documentos.value = _documentos.value + doc
            StoreEvents.changed()
            doc to null
        } catch (e: IOException) {
            destino.delete()
            null to "Error al copiar el archivo."
        } catch (e: SecurityException) {
            destino.delete()
            null to "No hay permiso para leer el archivo."
        }
    }

    fun registerGenerated(context: Context, owner: User, file: File, nombre: String): Documento {
        val doc = Documento(
            id = nextId(), ownerId = owner.id, nombre = nombre, archivo = file.name,
            mime = Documento.MIME_PDF, tamano = file.length(), fecha = System.currentTimeMillis(),
            origen = "Generado por MPAZ Ticket"
        )
        _documentos.value = _documentos.value + doc
        StoreEvents.changed()
        return doc
    }


    fun newGeneratedFile(context: Context, prefix: String): File =
        fileFor(context, "${prefix}_${System.currentTimeMillis()}.pdf")

    fun delete(context: Context, user: User, docId: Int): String? {
        val doc = _documentos.value.firstOrNull { it.id == docId } ?: return "El documento no existe."
        if (doc.ownerId != user.id) return "Solo puedes eliminar tus documentos."
        fileFor(context, doc.archivo).delete()
        _documentos.value = _documentos.value.filter { it.id != docId }
        StoreEvents.changed()
        return null
    }

    fun restore(list: List<Documento>) {
        _documentos.value = list
    }

    fun reset(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
        _documentos.value = emptyList()
    }
}
