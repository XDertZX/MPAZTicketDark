package com.fabiancg.bottomappbar.data

import java.security.MessageDigest


enum class Role(val label: String) {
    SOLICITANTE("Funcionario"),
    ADMIN("Administrador (Encargado TI)"),
    RESPONSABLE("Responsable de la solución")
}

data class User(
    val id: Int,
    val nombre: String,
    val cargo: String,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val telefono: String = "",
    val registrado: Boolean = false
) {
    val iniciales: String
        get() = nombre.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
}

enum class Categoria(val label: String) {
    INFRAESTRUCTURA("Infraestructura"),
    TECNOLOGIA("Tecnología"),
    MOBILIARIO("Mobiliario"),
    OTRA("Otra")
}


enum class Prioridad(val label: String, val plazoHoras: Int) {
    BAJA("Baja", 8),
    MEDIA("Media", 6),
    ALTA("Alta", 2)
}

enum class EstadoTicket(val label: String) {
    PENDIENTE("Pendiente"),
    REVISADO("Revisado"),
    ASIGNADO("Asignado"),
    EN_PROCESO("En proceso"),
    SOLUCIONADO("Solucionado"),
    CERRADO("Cerrado"),
    CANCELADO("Cancelado");

    val esFinal: Boolean get() = this == CERRADO || this == CANCELADO

    val enCurso: Boolean get() = this == PENDIENTE || this == REVISADO || this == ASIGNADO || this == EN_PROCESO


    val resuelto: Boolean get() = this == SOLUCIONADO || this == CERRADO
}

enum class TipoComentario(val label: String) {
    GENERAL("Comentario"),
    ACCION_REALIZADA("Acción realizada"),
    REQUIERE_MATERIAL("Necesito materiales"),
    REQUIERE_INFO("Necesito más información"),
    DETALLE_FUNCIONARIO("Detalle agregado por el funcionario"),
    RESPUESTA_INFO("Respuesta a solicitud de información");

    companion object {
        val seleccionablesResponsable = listOf(GENERAL, ACCION_REALIZADA, REQUIERE_MATERIAL, REQUIERE_INFO)
    }
}

data class Comentario(
    val autorId: Int,
    val texto: String,
    val fecha: Long,
    val tipo: TipoComentario = TipoComentario.GENERAL
)

data class HistorialItem(
    val fecha: Long,
    val usuarioId: Int,
    val descripcion: String
)

data class SolicitudInfo(
    val id: Int,
    val adminId: Int,
    val mensaje: String,
    val fecha: Long,
    val plazoHoras: Int,
    val respuesta: String? = null,
    val fechaRespuesta: Long? = null
) {
    val vence: Long get() = fecha + plazoHoras * 3_600_000L
    val respondida: Boolean get() = respuesta != null
    fun vencida(now: Long): Boolean = !respondida && now > vence
}

data class Ticket(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val subcategoria: String,
    val ubicacion: String,
    val ubicacionDetalle: String,
    val solicitanteId: Int,
    val estado: EstadoTicket = EstadoTicket.PENDIENTE,
    val prioridad: Prioridad? = null,
    val gravedad: Prioridad = Prioridad.MEDIA,
    val asignadoId: Int? = null,
    val adminId: Int? = null,
    val fechaCreacion: Long,
    val fechaAsignacion: Long? = null,
    val fechaSolucion: Long? = null,
    val fechaCierre: Long? = null,
    val fotos: List<String> = emptyList(),
    val evidencias: List<String> = emptyList(),
    val comentarios: List<Comentario> = emptyList(),
    val historial: List<HistorialItem> = emptyList(),
    val solicitudesInfo: List<SolicitudInfo> = emptyList()
) {

    val codigo: String get() = "MP-" + id.toString().padStart(4, '0')

    val requiereMateriales: Boolean
        get() = comentarios.any { it.tipo == TipoComentario.REQUIERE_MATERIAL }

    val prioridadEfectiva: Prioridad get() = prioridad ?: gravedad

    val solicitudPendiente: SolicitudInfo? get() = solicitudesInfo.lastOrNull { !it.respondida }
}


data class ReportDraft(
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val subcategoria: String,
    val ubicacion: String,
    val detalle: String,
    val gravedad: Prioridad,
    val fotos: List<String>
)


data class Mensaje(val autorId: Int, val texto: String, val fecha: Long)

data class Conversacion(
    val id: Int,
    val funcionarioId: Int,
    val adminId: Int,
    val asunto: String,
    val abierta: Boolean = true,
    val mensajes: List<Mensaje> = emptyList(),
    val fechaCreacion: Long,
    val fechaCierre: Long? = null,
    val noLeidaAdmin: Boolean = false,
    val noLeidaFuncionario: Boolean = false
) {
    val ultimaFecha: Long get() = mensajes.maxOfOrNull { it.fecha } ?: fechaCreacion
}

enum class TipoContacto(val label: String) {
    MENSAJE("Mensaje"),
    LLAMADA("Llamada"),
    CORREO("Correo")
}

data class RegistroContacto(
    val id: Int,
    val funcionarioId: Int,
    val adminId: Int,
    val tipo: TipoContacto,
    val fecha: Long
)

enum class TipoNotificacion { TICKET, INFO, MENSAJE, CONTACTO, SISTEMA }

data class Notificacion(
    val id: Int,
    val paraUserId: Int,
    val titulo: String,
    val texto: String,
    val fecha: Long,
    val tipo: TipoNotificacion,
    val ticketId: Int? = null,
    val conversacionId: Int? = null,
    val deUserId: Int? = null,
    val leida: Boolean = false
)


data class Documento(
    val id: Int,
    val ownerId: Int,
    val nombre: String,
    val archivo: String,
    val mime: String,
    val tamano: Long,
    val fecha: Long,
    val origen: String = "Cargado por el usuario"
) {
    val esPdf: Boolean get() = mime == MIME_PDF
    val tipoLabel: String get() = if (esPdf) "PDF" else "Word"

    companion object {
        const val MIME_PDF = "application/pdf"
        const val MIME_DOC = "application/msword"
        const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        val permitidos = arrayOf(MIME_PDF, MIME_DOC, MIME_DOCX)
    }
}

object Catalogo {
    val subcategorias: Map<Categoria, List<String>> = mapOf(
        Categoria.INFRAESTRUCTURA to listOf(
            "Electricidad e iluminación", "Gasfitería y baños", "Puertas y ventanas",
            "Muros y pintura", "Techumbre y filtraciones", "Otra infraestructura"
        ),
        Categoria.TECNOLOGIA to listOf(
            "Computador o notebook", "Proyector", "Impresora", "Red e internet",
            "Parlantes y audio", "Robótica", "Otro equipo tecnológico"
        ),
        Categoria.MOBILIARIO to listOf(
            "Mesas y sillas", "Estantes y muebles", "Pizarra", "Otro mobiliario"
        ),
        Categoria.OTRA to listOf("Otra")
    )

    val ubicaciones = listOf(
        "Sala de clases", "Oficinas", "Biblioteca", "Laboratorio", "Sala sensorial",
        "CIEMP", "Patio", "Baños", "Huerto", "Pasillos", "Otro espacio"
    )
}

enum class RangoFecha(val label: String, val days: Int?) {
    TODOS("Todas las fechas", null),
    HOY("Últimas 24 horas", 1),
    SEMANA("Últimos 7 días", 7),
    MES("Últimos 30 días", 30)
}

data class TicketFilters(
    val estado: EstadoTicket? = null,
    val prioridad: Prioridad? = null,
    val categoria: Categoria? = null,
    val responsableId: Int? = null,
    val ubicacion: String = "",
    val rango: RangoFecha = RangoFecha.TODOS
) {
    val activeCount: Int
        get() = listOfNotNull(estado, prioridad, categoria, responsableId).size +
                (if (ubicacion.isNotBlank()) 1 else 0) +
                (if (rango != RangoFecha.TODOS) 1 else 0)
}


object Security {
    fun hash(email: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("mpaz:${email.trim().lowercase()}:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val nombreRegex = Regex("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]+$")

    fun email(value: String): String? = when {
        value.isBlank() -> "Ingresa tu correo"
        !emailRegex.matches(value.trim()) -> "Correo no válido (ej: nombre@demo.cl)"
        else -> null
    }

    fun nombre(value: String): String? = when {
        value.trim().length < 3 -> "Mínimo 3 caracteres"
        !nombreRegex.matches(value.trim()) -> "Solo letras y espacios"
        else -> null
    }

    fun cargo(value: String): String? = if (value.trim().length < 3) "Indica tu cargo (mínimo 3 caracteres)" else null

    fun telefono(value: String): String? {
        val digits = value.filter { it.isDigit() }.removePrefix("56")
        return if (digits.length == 9 && digits.startsWith("9")) null else "Debe tener 9 dígitos y empezar con 9"
    }

    fun nuevaPassword(value: String): String? = when {
        value.length < 6 -> "Mínimo 6 caracteres"
        value.none { it.isDigit() } || value.none { it.isLetter() } -> "Debe tener letras y números"
        else -> null
    }

    const val MIN_TITULO = 5
    const val MIN_DESCRIPCION = 10
    const val MIN_DETALLE = 10
    const val MIN_ASUNTO = 4
    const val MIN_MENSAJE = 10
    const val MIN_PASSWORD_LOGIN = 4

    fun minimo(value: String, min: Int): String? =
        if (value.trim().length < min) "Mínimo $min caracteres" else null

    fun obligatorio(value: String, mensaje: String = "Este campo es obligatorio"): String? =
        if (value.isBlank()) mensaje else null

    fun <T> seleccion(value: T?, mensaje: String): String? = if (value == null) mensaje else null

    fun passwordLogin(value: String): String? = when {
        value.isBlank() -> "Ingresa tu contraseña"
        value.length < MIN_PASSWORD_LOGIN -> "Mínimo $MIN_PASSWORD_LOGIN caracteres"
        else -> null
    }

    fun confirmacion(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Repite la contraseña"
        confirm != password -> "Las contraseñas no coinciden"
        else -> null
    }

    fun emailRegistro(value: String, yaExiste: Boolean): String? =
        email(value) ?: if (yaExiste) "Ya existe una cuenta con ese correo" else null

    fun titulo(value: String): String? = minimo(value, MIN_TITULO)

    fun descripcion(value: String): String? = minimo(value, MIN_DESCRIPCION)

    fun detalleOpcional(value: String): String? = if (value.isBlank()) null else minimo(value, MIN_DETALLE)

    fun asunto(value: String): String? = minimo(value, MIN_ASUNTO)

    fun mensaje(value: String): String? = minimo(value, MIN_MENSAJE)

    fun respuestaInfo(value: String): String? = minimo(value, MIN_MENSAJE)

    fun edicionReporte(detalle: String, cambiaGravedad: Boolean, fotosNuevas: Int): String? = when {
        detalle.isBlank() && !cambiaGravedad && fotosNuevas == 0 -> "No hay cambios para guardar."
        else -> detalleOpcional(detalle)
    }

    fun formatTelefono(value: String): String {
        val d = value.filter { it.isDigit() }.removePrefix("56")
        return if (d.length == 9) "+56 ${d[0]} ${d.substring(1, 5)} ${d.substring(5)}" else value.trim()
    }
}
