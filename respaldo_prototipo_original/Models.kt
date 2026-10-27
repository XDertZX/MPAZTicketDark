package com.fabiancg.bottomappbar.data

enum class Role(val label: String) {

    SOLICITANTE("Solicitante"),
    ADMIN("Administrador (Encargado TI)"),
    RESPONSABLE("Responsable de la solución")
}

data class User(
    val id: Int,
    val nombre: String,
    val cargo: String,
    val email: String,
    val password: String,

    val role: Role
)

enum class Categoria(val label: String) {
    INFRAESTRUCTURA("Infraestructura"),
    TECNOLOGIA("Tecnología"),
    MOBILIARIO("Mobiliario"),
    OTRA("Otra")
}

enum class Prioridad(val label: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
    URGENTE("Urgente")
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
}

enum class TipoComentario(val label: String) {
    GENERAL("Comentario"),
    ACCION_REALIZADA("Acción realizada"),
    REQUIERE_MATERIAL("Necesito materiales"),
    REQUIERE_INFO("Necesito más información")
}

data class Comentario(
    val autorId: Int,
    val texto: String,
    val fecha: Long,
    val tipo: TipoComentario = TipoComentario.GENERAL
)

/** Cada cambio importante del ticket queda registrado aquí (trazabilidad). */
data class HistorialItem(
    val fecha: Long,
    val usuarioId: Int,
    val descripcion: String
)

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
    val asignadoId: Int? = null,
    val adminId: Int? = null,
    val fechaCreacion: Long,
    val fechaAsignacion: Long? = null,
    val fechaSolucion: Long? = null,
    val fechaCierre: Long? = null,
    val fotos: List<String> = emptyList(),
    val evidencias: List<String> = emptyList(),
    val comentarios: List<Comentario> = emptyList(),
    val historial: List<HistorialItem> = emptyList()
) {
    /** Código único visible para el usuario, ej: MP-0007 */
    val codigo: String get() = "MP-" + id.toString().padStart(4, '0')

    val requiereMateriales: Boolean
        get() = comentarios.any { it.tipo == TipoComentario.REQUIERE_MATERIAL }
}

/** Catálogo ficticio de categorías, subcategorías y ubicaciones (material de apoyo del caso). */
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

/** Filtros del historial: categoría, prioridad, estado, ubicación, responsable y fecha (sección 3.1). */
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
