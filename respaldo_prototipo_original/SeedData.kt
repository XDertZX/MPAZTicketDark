package com.fabiancg.bottomappbar.data

import com.fabiancg.bottomappbar.data.Categoria.*
import com.fabiancg.bottomappbar.data.EstadoTicket.*
import com.fabiancg.bottomappbar.data.Prioridad.*
import com.fabiancg.bottomappbar.data.Role.*
import com.fabiancg.bottomappbar.data.TipoComentario.*


object Users {
    val all = listOf(
        User(1, "Ana Torres", "Docente de 3° Básico", "ana@demo.cl", "1234", SOLICITANTE),
        User(2, "Luis Pérez", "Inspector", "luis@demo.cl", "1234", SOLICITANTE),
        User(3, "Tomás Vega", "Encargado de Tecnología (TI)", "tomas@demo.cl", "1234", ADMIN),
        User(4, "Jorge Núñez", "Auxiliar de mantención", "jorge@demo.cl", "1234", RESPONSABLE),
        User(5, "Marta Lagos", "Auxiliar de mantención", "marta@demo.cl", "1234", RESPONSABLE)
    )

    fun byId(id: Int?): User? = all.firstOrNull { it.id == id }
    fun nameOf(id: Int?): String = byId(id)?.nombre ?: "—"

    val asignables: List<User> get() = all.filter { it.role == ADMIN || it.role == RESPONSABLE }

    fun authenticate(email: String, password: String): User? =
        all.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }
}

object SeedData {
    private const val DAY = 86_400_000L
    private fun ago(days: Double): Long = System.currentTimeMillis() - (days * DAY).toLong()
    private fun ev(days: Double, user: Int, text: String) = HistorialItem(ago(days), user, text)

    fun tickets(): List<Ticket> = listOf(
        Ticket(
            id = 1, titulo = "Proyector de la sala 5° A no enciende",
            descripcion = "Al presionar el botón de encendido el proyector no muestra luz ni imagen. Se usa todos los días.",
            categoria = TECNOLOGIA, subcategoria = "Proyector",
            ubicacion = "Sala de clases", ubicacionDetalle = "5° A",
            solicitanteId = 1, estado = EN_PROCESO, prioridad = ALTA, asignadoId = 3, adminId = 3,
            fechaCreacion = ago(3.0), fechaAsignacion = ago(2.5),
            comentarios = listOf(
                Comentario(1, "Lo necesito para la clase de mañana, por favor.", ago(2.9)),
                Comentario(3, "Cambié el cable HDMI y sigue sin encender. Reviso la lámpara.", ago(1.5), ACCION_REALIZADA)
            ),
            historial = listOf(
                ev(3.0, 1, "Ticket creado"),
                ev(2.8, 3, "Revisado: Tecnología / Proyector, prioridad Alta"),
                ev(2.5, 3, "Asignado a Tomás Vega"),
                ev(2.0, 3, "Estado: Asignado → En proceso")
            )
        ),
        Ticket(
            id = 2, titulo = "Filtración de agua en baño del primer ciclo",
            descripcion = "Gotea agua desde la llave del segundo lavamanos y el piso queda mojado.",
            categoria = INFRAESTRUCTURA, subcategoria = "Gasfitería y baños",
            ubicacion = "Baños", ubicacionDetalle = "Primer ciclo, segundo lavamanos",
            solicitanteId = 2, estado = ASIGNADO, prioridad = URGENTE, asignadoId = 4, adminId = 3,
            fechaCreacion = ago(1.0), fechaAsignacion = ago(0.8),
            historial = listOf(
                ev(1.0, 2, "Ticket creado"),
                ev(0.9, 3, "Revisado: Infraestructura / Gasfitería y baños, prioridad Urgente"),
                ev(0.8, 3, "Asignado a Jorge Núñez")
            )
        ),
        Ticket(
            id = 3, titulo = "Silla rota en la biblioteca",
            descripcion = "Una silla de la mesa 2 tiene una pata suelta y es peligroso sentarse.",
            categoria = MOBILIARIO, subcategoria = "Mesas y sillas",
            ubicacion = "Biblioteca", ubicacionDetalle = "Mesa 2",
            solicitanteId = 2, estado = PENDIENTE,
            fechaCreacion = ago(0.2),
            historial = listOf(ev(0.2, 2, "Ticket creado"))
        ),
        Ticket(
            id = 4, titulo = "Sin internet en la sala de profesores",
            descripcion = "Los computadores de la sala de profesores no se conectan a la red desde la mañana.",
            categoria = TECNOLOGIA, subcategoria = "Red e internet",
            ubicacion = "Oficinas", ubicacionDetalle = "Sala de profesores",
            solicitanteId = 1, estado = REVISADO, prioridad = MEDIA, adminId = 3,
            fechaCreacion = ago(0.6),
            historial = listOf(
                ev(0.6, 1, "Ticket creado"),
                ev(0.5, 3, "Revisado: Tecnología / Red e internet, prioridad Media")
            )
        ),
        Ticket(
            id = 5, titulo = "Ampolleta quemada en el pasillo",
            descripcion = "La luz del pasillo del segundo piso no funciona y queda oscuro.",
            categoria = INFRAESTRUCTURA, subcategoria = "Electricidad e iluminación",
            ubicacion = "Pasillos", ubicacionDetalle = "Segundo piso, frente a sala 6° B",
            solicitanteId = 2, estado = SOLUCIONADO, prioridad = BAJA, asignadoId = 5, adminId = 3,
            fechaCreacion = ago(5.0), fechaAsignacion = ago(4.5), fechaSolucion = ago(1.0),
            comentarios = listOf(
                Comentario(5, "Reemplacé la ampolleta por una LED. Funciona correctamente.", ago(1.0), ACCION_REALIZADA)
            ),
            historial = listOf(
                ev(5.0, 2, "Ticket creado"),
                ev(4.8, 3, "Revisado: Infraestructura / Electricidad e iluminación, prioridad Baja"),
                ev(4.5, 3, "Asignado a Marta Lagos"),
                ev(3.0, 5, "Estado: Asignado → En proceso"),
                ev(1.0, 5, "Estado: En proceso → Solucionado")
            )
        ),
        Ticket(
            id = 6, titulo = "Marco de la pizarra suelto",
            descripcion = "El marco inferior de la pizarra se está despegando de la pared.",
            categoria = MOBILIARIO, subcategoria = "Pizarra",
            ubicacion = "Sala de clases", ubicacionDetalle = "7° B",
            solicitanteId = 1, estado = CERRADO, prioridad = BAJA, asignadoId = 4, adminId = 3,
            fechaCreacion = ago(12.0), fechaAsignacion = ago(11.5), fechaSolucion = ago(10.0), fechaCierre = ago(9.5),
            comentarios = listOf(
                Comentario(4, "Fijé el marco con tarugos y tornillos nuevos.", ago(10.0), ACCION_REALIZADA)
            ),
            historial = listOf(
                ev(12.0, 1, "Ticket creado"),
                ev(11.8, 3, "Revisado: Mobiliario / Pizarra, prioridad Baja"),
                ev(11.5, 3, "Asignado a Jorge Núñez"),
                ev(10.5, 4, "Estado: Asignado → En proceso"),
                ev(10.0, 4, "Estado: En proceso → Solucionado"),
                ev(9.5, 3, "Estado: Solucionado → Cerrado")
            )
        ),
        Ticket(
            id = 7, titulo = "Impresora atasca el papel",
            descripcion = "La impresora de secretaría se detiene a la mitad de cada hoja.",
            categoria = TECNOLOGIA, subcategoria = "Impresora",
            ubicacion = "Oficinas", ubicacionDetalle = "Secretaría",
            solicitanteId = 2, estado = CANCELADO, adminId = 3,
            fechaCreacion = ago(7.0),
            comentarios = listOf(
                Comentario(2, "Ya se arregló limpiando los rodillos, cancelo la solicitud.", ago(6.5))
            ),
            historial = listOf(
                ev(7.0, 2, "Ticket creado"),
                ev(6.5, 2, "Estado: Pendiente → Cancelado. Detalle: Ya se arregló limpiando los rodillos.")
            )
        ),
        Ticket(
            id = 8, titulo = "La puerta del laboratorio no cierra bien",
            descripcion = "La puerta queda abierta aunque se empuje; parece que las bisagras están dobladas.",
            categoria = INFRAESTRUCTURA, subcategoria = "Puertas y ventanas",
            ubicacion = "Laboratorio", ubicacionDetalle = "Entrada principal",
            solicitanteId = 1, estado = ASIGNADO, prioridad = MEDIA, asignadoId = 5, adminId = 3,
            fechaCreacion = ago(2.0), fechaAsignacion = ago(1.5),
            comentarios = listOf(
                Comentario(5, "Necesito bisagras nuevas para reparar la puerta.", ago(1.0), REQUIERE_MATERIAL)
            ),
            historial = listOf(
                ev(2.0, 1, "Ticket creado"),
                ev(1.8, 3, "Revisado: Infraestructura / Puertas y ventanas, prioridad Media"),
                ev(1.5, 3, "Asignado a Marta Lagos")
            )
        )
    )
}
