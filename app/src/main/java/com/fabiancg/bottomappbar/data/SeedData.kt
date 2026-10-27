package com.fabiancg.bottomappbar.data

import com.fabiancg.bottomappbar.data.Categoria.INFRAESTRUCTURA
import com.fabiancg.bottomappbar.data.Categoria.MOBILIARIO
import com.fabiancg.bottomappbar.data.Categoria.OTRA
import com.fabiancg.bottomappbar.data.Categoria.TECNOLOGIA
import com.fabiancg.bottomappbar.data.EstadoTicket.ASIGNADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CANCELADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CERRADO
import com.fabiancg.bottomappbar.data.EstadoTicket.EN_PROCESO
import com.fabiancg.bottomappbar.data.EstadoTicket.PENDIENTE
import com.fabiancg.bottomappbar.data.EstadoTicket.REVISADO
import com.fabiancg.bottomappbar.data.EstadoTicket.SOLUCIONADO
import com.fabiancg.bottomappbar.data.Prioridad.ALTA
import com.fabiancg.bottomappbar.data.Prioridad.BAJA
import com.fabiancg.bottomappbar.data.Prioridad.MEDIA
import com.fabiancg.bottomappbar.data.Role.ADMIN
import com.fabiancg.bottomappbar.data.Role.RESPONSABLE
import com.fabiancg.bottomappbar.data.Role.SOLICITANTE
import com.fabiancg.bottomappbar.data.TipoComentario.ACCION_REALIZADA
import com.fabiancg.bottomappbar.data.TipoComentario.REQUIERE_MATERIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

object StoreEvents {
    var listener: (() -> Unit)? = null
    fun changed() {
        listener?.invoke()
    }
}

object Users {
    const val DEMO_PASSWORD = "1234"
    const val ENCARGADO_TI_ID = 3

    private fun demo(id: Int, nombre: String, cargo: String, email: String, role: Role, tel: String) =
        User(id, nombre, cargo, email, Security.hash(email, DEMO_PASSWORD), role, tel)

    val demo: List<User> = listOf(
        demo(1, "Ana Torres", "Docente de 3° Básico", "ana@demo.cl", SOLICITANTE, "+56 9 5550 0101"),
        demo(2, "Luis Pérez", "Inspector", "luis@demo.cl", SOLICITANTE, "+56 9 5550 0102"),
        demo(3, "Tomás Vega", "Encargado de Tecnología (TI)", "tomas@demo.cl", ADMIN, "+56 9 5550 0103"),
        demo(4, "Jorge Núñez", "Auxiliar de mantención", "jorge@demo.cl", RESPONSABLE, "+56 9 5550 0104"),
        demo(5, "Marta Lagos", "Auxiliar de mantención", "marta@demo.cl", RESPONSABLE, "+56 9 5550 0105"),
        demo(6, "Carla Rojas", "Jefa de UTP · Administradora de la plataforma", "carla@demo.cl", ADMIN, "+56 9 5550 0106"),
        demo(7, "Paula Soto", "Educadora de Párvulos", "paula@demo.cl", SOLICITANTE, "+56 9 5550 0107"),
        demo(8, "Diego Fuentes", "Profesor de Educación Física", "diego@demo.cl", SOLICITANTE, "+56 9 5550 0108"),
        demo(9, "Camila Reyes", "Encargada de Biblioteca (CRA)", "camila@demo.cl", SOLICITANTE, "+56 9 5550 0109")
    )

    val demoLogin: List<User> get() = listOf(1, 2, 3, 6, 4, 5).mapNotNull { id -> demo.firstOrNull { it.id == id } }

    private val _registered = MutableStateFlow<List<User>>(emptyList())
    val registered: StateFlow<List<User>> = _registered.asStateFlow()

    val all: List<User> get() = demo + _registered.value

    fun byId(id: Int?): User? = all.firstOrNull { it.id == id }
    fun nameOf(id: Int?): String = byId(id)?.nombre ?: "—"

    val admins: List<User> get() = all.filter { it.role == ADMIN }

    val asignables: List<User>
        get() = all.filter { it.role == RESPONSABLE || it.id == ENCARGADO_TI_ID }

    fun authenticate(email: String, password: String): User? {
        val hash = Security.hash(email, password)
        return all.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.passwordHash == hash }
    }

    fun emailExists(email: String): Boolean = all.any { it.email.equals(email.trim(), ignoreCase = true) }

    fun register(nombre: String, cargo: String, email: String, telefono: String, password: String): User {
        val id = maxOf(100, all.maxOf { it.id }) + 1
        val mail = email.trim().lowercase()
        val user = User(
            id = id,
            nombre = nombre.trim().split(" ").filter { it.isNotBlank() }
                .joinToString(" ") { p -> p.lowercase().replaceFirstChar { it.uppercaseChar() } },
            cargo = cargo.trim(),
            email = mail,
            passwordHash = Security.hash(mail, password),
            role = SOLICITANTE,
            telefono = Validators.formatTelefono(telefono),
            registrado = true
        )
        _registered.value = _registered.value + user
        StoreEvents.changed()
        return user
    }

    fun restore(list: List<User>) {
        _registered.value = list
    }

    fun reset() {
        _registered.value = emptyList()
    }
}

object SeedData {
    private const val DAY = 86_400_000L
    private const val HOUR = 3_600_000L
    private fun ago(days: Double): Long = System.currentTimeMillis() - (days * DAY).toLong()
    private fun ev(days: Double, user: Int, text: String) = HistorialItem(ago(days), user, text)

    private fun principales(): List<Ticket> = listOf(
        Ticket(
            id = 1, titulo = "Proyector de la sala 5° A no enciende",
            descripcion = "Al presionar el botón de encendido el proyector no muestra luz ni imagen.\n" +
                    "Se usa todos los días en las clases de la mañana.\n" +
                    "Ya revisamos que el enchufe tiene corriente.\nEl control remoto tampoco responde.",
            categoria = TECNOLOGIA, subcategoria = "Proyector",
            ubicacion = "Sala de clases", ubicacionDetalle = "5° A",
            solicitanteId = 1, estado = EN_PROCESO, prioridad = ALTA, gravedad = ALTA, asignadoId = 3, adminId = 3,
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
            descripcion = "Gotea agua desde la llave del segundo lavamanos y el piso queda mojado.\n" +
                    "Los niños se pueden resbalar al entrar.\nPusimos un letrero de precaución mientras tanto.",
            categoria = INFRAESTRUCTURA, subcategoria = "Gasfitería y baños",
            ubicacion = "Baños", ubicacionDetalle = "Primer ciclo, segundo lavamanos",
            solicitanteId = 2, estado = ASIGNADO, prioridad = ALTA, gravedad = ALTA, asignadoId = 4, adminId = 3,
            fechaCreacion = ago(1.0), fechaAsignacion = ago(0.8),
            historial = listOf(
                ev(1.0, 2, "Ticket creado"),
                ev(0.9, 3, "Revisado: Infraestructura / Gasfitería y baños, prioridad Alta"),
                ev(0.8, 3, "Asignado a Jorge Núñez")
            )
        ),
        Ticket(
            id = 3, titulo = "Silla rota en la biblioteca",
            descripcion = "Una silla de la mesa 2 tiene una pata suelta y es peligroso sentarse.\n" +
                    "La dejamos apartada en un rincón.",
            categoria = MOBILIARIO, subcategoria = "Mesas y sillas",
            ubicacion = "Biblioteca", ubicacionDetalle = "Mesa 2",
            solicitanteId = 2, estado = PENDIENTE, gravedad = MEDIA,
            fechaCreacion = ago(0.2),
            historial = listOf(ev(0.2, 2, "Ticket creado"))
        ),
        Ticket(
            id = 4, titulo = "Sin internet en la sala de profesores",
            descripcion = "Los computadores de la sala de profesores no se conectan a la red desde la mañana.\n" +
                    "El ícono de red muestra un signo de exclamación.",
            categoria = TECNOLOGIA, subcategoria = "Red e internet",
            ubicacion = "Oficinas", ubicacionDetalle = "Sala de profesores",
            solicitanteId = 1, estado = REVISADO, prioridad = MEDIA, gravedad = MEDIA, adminId = 3,
            fechaCreacion = ago(0.6),
            historial = listOf(
                ev(0.6, 1, "Ticket creado"),
                ev(0.5, 3, "Revisado: Tecnología / Red e internet, prioridad Media"),
                ev(0.05, 3, "Se solicitó más información al funcionario (plazo 6 h)")
            ),
            solicitudesInfo = listOf(
                SolicitudInfo(
                    id = 1, adminId = 3,
                    mensaje = "¿Cuántos computadores están sin red y si el problema es solo por cable o también por Wi-Fi?",
                    fecha = System.currentTimeMillis() - (1.2 * HOUR).toLong(), plazoHoras = MEDIA.plazoHoras
                )
            )
        ),
        Ticket(
            id = 5, titulo = "Ampolleta quemada en el pasillo",
            descripcion = "La luz del pasillo del segundo piso no funciona y queda oscuro en la tarde.",
            categoria = INFRAESTRUCTURA, subcategoria = "Electricidad e iluminación",
            ubicacion = "Pasillos", ubicacionDetalle = "Segundo piso, frente a sala 6° B",
            solicitanteId = 2, estado = SOLUCIONADO, prioridad = BAJA, gravedad = BAJA, asignadoId = 5, adminId = 3,
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
            solicitanteId = 1, estado = CERRADO, prioridad = BAJA, gravedad = BAJA, asignadoId = 4, adminId = 3,
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
            solicitanteId = 2, estado = CANCELADO, gravedad = BAJA, adminId = 3,
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
            descripcion = "La puerta queda abierta aunque se empuje; parece que las bisagras están dobladas.\n" +
                    "Hace ruido al moverla.\nEn el laboratorio hay notebooks guardados, por eso es importante.",
            categoria = INFRAESTRUCTURA, subcategoria = "Puertas y ventanas",
            ubicacion = "Laboratorio", ubicacionDetalle = "Entrada principal",
            solicitanteId = 1, estado = ASIGNADO, prioridad = MEDIA, gravedad = MEDIA, asignadoId = 5, adminId = 3,
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

    private data class Plantilla(
        val categoria: Categoria, val sub: String, val ubicacion: String, val titulo: String, val descripcion: String
    )

    private val plantillas = listOf(
        Plantilla(TECNOLOGIA, "Computador o notebook", "Laboratorio", "Notebook no carga la batería",
            "El notebook N° 12 del carro de computación no carga aunque esté conectado.\nLa luz del cargador se apaga.\nSe probó con otro enchufe."),
        Plantilla(TECNOLOGIA, "Red e internet", "Sala de clases", "Wi-Fi intermitente en la sala",
            "La señal Wi-Fi se corta varias veces durante la clase.\nAfecta las actividades con tablets.\nPasa sobre todo en la mañana."),
        Plantilla(TECNOLOGIA, "Parlantes y audio", "Patio", "Parlante del patio con ruido",
            "El parlante que se usa en los actos emite un zumbido fuerte.\nNo se entiende lo que se dice por el micrófono."),
        Plantilla(TECNOLOGIA, "Impresora", "Oficinas", "Impresora imprime con manchas",
            "Las hojas salen con manchas negras en el borde.\nSe cambió el tóner hace una semana.\nUrge para imprimir guías."),
        Plantilla(TECNOLOGIA, "Robótica", "Laboratorio", "Kit de robótica con motor dañado",
            "Uno de los kits de robótica tiene un motor que no gira.\nLos estudiantes no pueden terminar la actividad."),
        Plantilla(TECNOLOGIA, "Proyector", "Sala de clases", "Proyector con imagen borrosa",
            "La imagen del proyector se ve borrosa aunque se ajuste el foco.\nTambién se ve un tono amarillo."),
        Plantilla(INFRAESTRUCTURA, "Electricidad e iluminación", "Sala de clases", "Tubo fluorescente parpadea",
            "Uno de los tubos del techo parpadea todo el día.\nMolesta a los estudiantes y produce dolor de cabeza."),
        Plantilla(INFRAESTRUCTURA, "Gasfitería y baños", "Baños", "Estanque del WC no deja de correr",
            "El estanque del baño de niñas pierde agua constantemente.\nSe escucha correr el agua toda la jornada."),
        Plantilla(INFRAESTRUCTURA, "Puertas y ventanas", "Sala de clases", "Ventana no cierra",
            "La ventana del costado no cierra completamente.\nEntra frío y agua cuando llueve."),
        Plantilla(INFRAESTRUCTURA, "Muros y pintura", "Pasillos", "Pintura descascarada en muro",
            "El muro del pasillo principal tiene pintura suelta.\nSe desprenden trozos al tocarlo."),
        Plantilla(INFRAESTRUCTURA, "Techumbre y filtraciones", "Biblioteca", "Gotera en el techo",
            "Cuando llueve cae agua cerca de los estantes.\nPusimos un balde para proteger los libros."),
        Plantilla(MOBILIARIO, "Mesas y sillas", "Sala de clases", "Mesa coja",
            "Una mesa de estudiantes está coja y se mueve al escribir.\nLe falta un regatón."),
        Plantilla(MOBILIARIO, "Estantes y muebles", "Biblioteca", "Repisa del estante suelta",
            "Una repisa del estante de cuentos se soltó.\nLos libros quedaron en el suelo."),
        Plantilla(MOBILIARIO, "Pizarra", "Sala de clases", "Pizarra con manchas permanentes",
            "La pizarra blanca tiene manchas que no salen con el borrador.\nCuesta leer lo que se escribe."),
        Plantilla(MOBILIARIO, "Otro mobiliario", "Sala sensorial", "Cojín de la sala sensorial rasgado",
            "Uno de los cojines grandes tiene el forro rasgado.\nSe está saliendo el relleno."),
        Plantilla(OTRA, "Otra", "Huerto", "Manguera del huerto rota",
            "La manguera que se usa para regar el huerto tiene una fisura.\nSe pierde mucha agua al regar.")
    )

    private val detalles = listOf("1° A", "2° B", "3° A", "4° B", "6° A", "8° B", "Kínder", "Primer piso", "Segundo piso", "Sector norte")


    private fun historico(): List<Ticket> {
        val rnd = Random(2026L)
        val solicitantes = listOf(2, 7, 8, 9)
        val responsables = listOf(4, 5, 3)
        val result = mutableListOf<Ticket>()
        var id = 9

        fun prioridadAleatoria(): Prioridad = when (rnd.nextInt(10)) {
            in 0..3 -> BAJA
            in 4..7 -> MEDIA
            else -> ALTA
        }

        fun crear(daysAgo: Double, reciente: Boolean) {
            val p = plantillas[rnd.nextInt(plantillas.size)]
            val solicitante = solicitantes[rnd.nextInt(solicitantes.size)]
            val responsable = responsables[rnd.nextInt(responsables.size)]
            val gravedad = prioridadAleatoria()
            val prioridad = if (rnd.nextInt(4) == 0) prioridadAleatoria() else gravedad
            val estado: EstadoTicket = if (reciente) {
                listOf(PENDIENTE, REVISADO, ASIGNADO, EN_PROCESO, EN_PROCESO, SOLUCIONADO, CERRADO, CERRADO)[rnd.nextInt(8)]
            } else {
                when (rnd.nextInt(10)) {
                    in 0..6 -> CERRADO
                    7 -> SOLUCIONADO
                    8 -> CANCELADO
                    else -> CERRADO
                }
            }
            val creado = ago(daysAgo)
            val h = mutableListOf(HistorialItem(creado, solicitante, "Ticket creado"))
            var asignacion: Long? = null
            var solucion: Long? = null
            var cierre: Long? = null
            val revisado = estado != PENDIENTE && estado != CANCELADO
            if (revisado) h += HistorialItem(creado + 3 * HOUR, Users.ENCARGADO_TI_ID,
                "Revisado: ${p.categoria.label} / ${p.sub}, prioridad ${prioridad.label}")
            if (estado in listOf(ASIGNADO, EN_PROCESO, SOLUCIONADO, CERRADO)) {
                asignacion = creado + 6 * HOUR
                h += HistorialItem(asignacion, Users.ENCARGADO_TI_ID, "Asignado a ${Users.nameOf(responsable)}")
            }
            if (estado in listOf(EN_PROCESO, SOLUCIONADO, CERRADO)) {
                h += HistorialItem(creado + 20 * HOUR, responsable, "Estado: Asignado → En proceso")
            }
            if (estado == SOLUCIONADO || estado == CERRADO) {
                solucion = creado + (1 + rnd.nextInt(3)) * DAY
                h += HistorialItem(solucion, responsable, "Estado: En proceso → Solucionado")
            }
            if (estado == CERRADO) {
                cierre = (solucion ?: creado) + 5 * HOUR
                h += HistorialItem(cierre, Users.ENCARGADO_TI_ID, "Estado: Solucionado → Cerrado")
            }
            if (estado == CANCELADO) {
                h += HistorialItem(creado + 2 * HOUR, solicitante, "Estado: Pendiente → Cancelado. Detalle: Reporte duplicado.")
            }
            val comentarios = if (solucion != null)
                listOf(Comentario(responsable, "Se realizó la reparación y se probó el funcionamiento.", solucion, ACCION_REALIZADA))
            else emptyList()

            result += Ticket(
                id = id++, titulo = p.titulo, descripcion = p.descripcion,
                categoria = p.categoria, subcategoria = p.sub,
                ubicacion = p.ubicacion, ubicacionDetalle = detalles[rnd.nextInt(detalles.size)],
                solicitanteId = solicitante, estado = estado,
                prioridad = if (revisado) prioridad else null, gravedad = gravedad,
                asignadoId = if (asignacion != null) responsable else null,
                adminId = if (estado != PENDIENTE) Users.ENCARGADO_TI_ID else null,
                fechaCreacion = creado, fechaAsignacion = asignacion, fechaSolucion = solucion, fechaCierre = cierre,
                comentarios = comentarios, historial = h
            )
        }

        repeat(56) { crear(26.0 + rnd.nextDouble() * 270.0, reciente = false) }
        repeat(14) { crear(0.4 + rnd.nextDouble() * 20.0, reciente = true) }
        return result
    }

    fun tickets(): List<Ticket> = principales() + historico()

    fun conversaciones(): List<Conversacion> = listOf(
        Conversacion(
            id = 1, funcionarioId = 2, adminId = 3, asunto = "Proyector para el acto del viernes",
            fechaCreacion = ago(0.3), noLeidaAdmin = true,
            mensajes = listOf(
                Mensaje(2, "Hola Tomás, ¿podemos contar con el proyector del auditorio para el acto del viernes?", ago(0.3)),
                Mensaje(2, "Necesitamos también un parlante con micrófono.", ago(0.28))
            )
        ),
        Conversacion(
            id = 2, funcionarioId = 7, adminId = 3, asunto = "Wi-Fi en sala de párvulos",
            fechaCreacion = ago(2.0),
            mensajes = listOf(
                Mensaje(7, "Buenos días, la tablet de la sala no se conecta al Wi-Fi.", ago(2.0)),
                Mensaje(3, "Hola Paula, ¿la tablet muestra la red MPAZ-Docentes?", ago(1.9)),
                Mensaje(7, "Sí, aparece pero dice \"sin internet\".", ago(1.8))
            )
        ),
        Conversacion(
            id = 3, funcionarioId = 1, adminId = 6, asunto = "Acceso a la plataforma",
            fechaCreacion = ago(8.0), abierta = false, fechaCierre = ago(7.5),
            mensajes = listOf(
                Mensaje(1, "Hola Carla, ¿cómo veo el estado de mis reportes?", ago(8.0)),
                Mensaje(6, "Hola Ana, en el menú lateral entra a «Mis reportes». Cierro esta consulta.", ago(7.6))
            )
        )
    )

    fun contactos(): List<RegistroContacto> = listOf(
        RegistroContacto(1, 7, 3, TipoContacto.LLAMADA, ago(1.95)),
        RegistroContacto(2, 8, 3, TipoContacto.CORREO, ago(4.0))
    )

    fun notificaciones(): List<Notificacion> = listOf(
        Notificacion(1, 3, "Nuevo mensaje de Luis Pérez", "Proyector para el acto del viernes",
            ago(0.28), TipoNotificacion.MENSAJE, conversacionId = 1, deUserId = 2),
        Notificacion(2, 3, "Nuevo reporte MP-0003", "Silla rota en la biblioteca · Luis Pérez",
            ago(0.2), TipoNotificacion.TICKET, ticketId = 3, deUserId = 2),
        Notificacion(3, 3, "Paula Soto intentó llamarte", "Llamada registrada desde «Contactar a un Administrador»",
            ago(1.95), TipoNotificacion.CONTACTO, deUserId = 7, leida = true),
        Notificacion(4, 1, "TI necesita más información", "MP-0004 · Sin internet en la sala de profesores. Plazo: 6 h",
            System.currentTimeMillis() - (1.2 * HOUR).toLong(), TipoNotificacion.INFO, ticketId = 4, deUserId = 3)
    )
}
