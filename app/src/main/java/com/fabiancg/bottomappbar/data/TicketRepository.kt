package com.fabiancg.bottomappbar.data

import com.fabiancg.bottomappbar.data.EstadoTicket.ASIGNADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CANCELADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CERRADO
import com.fabiancg.bottomappbar.data.EstadoTicket.EN_PROCESO
import com.fabiancg.bottomappbar.data.EstadoTicket.PENDIENTE
import com.fabiancg.bottomappbar.data.EstadoTicket.REVISADO
import com.fabiancg.bottomappbar.data.EstadoTicket.SOLUCIONADO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TicketRepository {

    private const val MAX_FOTOS_TICKET = 12

    private val lock = Any()
    private val _tickets = MutableStateFlow(SeedData.tickets())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private sealed interface Outcome {
        data class Ok(val ticket: Ticket) : Outcome
        data class Fail(val message: String) : Outcome
    }

    private data class Result(val error: String?, val ticket: Ticket?)

    private fun now() = System.currentTimeMillis()

    private fun mutate(ticketId: Int, block: (Ticket) -> Outcome): Result {
        val result = synchronized(lock) {
            val list = _tickets.value
            val actual = list.firstOrNull { it.id == ticketId }
            if (actual == null) {
                Result("El ticket no existe.", null)
            } else {
                when (val r = block(actual)) {
                    is Outcome.Ok -> {
                        _tickets.value = list.map { if (it.id == ticketId) r.ticket else it }
                        Result(null, r.ticket)
                    }
                    is Outcome.Fail -> Result(r.message, null)
                }
            }
        }
        if (result.error == null) StoreEvents.changed()
        return result
    }

    private fun log(user: User, text: String) = HistorialItem(now(), user.id, text)

    private fun notifySolicitante(actor: User, t: Ticket, titulo: String, texto: String, tipo: TipoNotificacion = TipoNotificacion.TICKET) {
        if (actor.id != t.solicitanteId) {
            MessageRepository.notify(t.solicitanteId, titulo, texto, tipo, ticketId = t.id, deUserId = actor.id)
        }
    }

    fun create(user: User, draft: ReportDraft): Ticket? {
        if (!TicketRules.canCreate(user)) return null
        if (Validators.titulo(draft.titulo) != null || Validators.descripcion(draft.descripcion) != null) return null
        val ticket = synchronized(lock) {
            val nuevo = Ticket(
                id = (_tickets.value.maxOfOrNull { it.id } ?: 0) + 1,
                titulo = draft.titulo.trim(), descripcion = draft.descripcion.trim(),
                categoria = draft.categoria, subcategoria = draft.subcategoria,
                ubicacion = draft.ubicacion, ubicacionDetalle = draft.detalle.trim(),
                solicitanteId = user.id, fechaCreacion = now(), fotos = draft.fotos,
                gravedad = draft.gravedad,
                historial = listOf(log(user, "Ticket creado (gravedad informada: ${draft.gravedad.label})"))
            )
            _tickets.value = _tickets.value + nuevo
            nuevo
        }
        StoreEvents.changed()
        MessageRepository.notifyAdmins(
            "Nuevo reporte ${ticket.codigo}", "${ticket.titulo} · ${user.nombre}",
            TipoNotificacion.TICKET, ticketId = ticket.id, deUserId = user.id
        )
        return ticket
    }

    fun review(user: User, ticketId: Int, categoria: Categoria, subcategoria: String, prioridad: Prioridad): String? {
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canClassify(user, t)) return@mutate Outcome.Fail("No puedes clasificar este ticket.")
            val primeraRevision = t.estado == PENDIENTE
            val detalle = "${categoria.label} / $subcategoria, prioridad ${prioridad.label}"
            Outcome.Ok(
                t.copy(
                    categoria = categoria, subcategoria = subcategoria, prioridad = prioridad,
                    adminId = user.id,
                    estado = if (primeraRevision) REVISADO else t.estado,
                    historial = t.historial + log(user, (if (primeraRevision) "Revisado: " else "Clasificación actualizada: ") + detalle)
                )
            )
        }
        r.ticket?.let { notifySolicitante(user, it, "Tu reporte ${it.codigo} fue revisado", "Prioridad ${prioridad.label}") }
        return r.error
    }

    fun assign(user: User, ticketId: Int, responsable: User): String? {
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canAssign(user, t)) return@mutate Outcome.Fail("No puedes asignar este ticket.")
            if (responsable.role == Role.SOLICITANTE) return@mutate Outcome.Fail("Esa persona no puede ser responsable.")
            if (t.asignadoId == responsable.id) return@mutate Outcome.Fail("El ticket ya está asignado a esa persona.")
            val texto = if (t.asignadoId == null) "Asignado a ${responsable.nombre}"
            else "Reasignado de ${Users.nameOf(t.asignadoId)} a ${responsable.nombre}"
            Outcome.Ok(
                t.copy(
                    asignadoId = responsable.id, fechaAsignacion = now(), estado = ASIGNADO,
                    historial = t.historial + log(user, texto)
                )
            )
        }
        r.ticket?.let {
            if (responsable.id != user.id) {
                MessageRepository.notify(responsable.id, "Se te asignó ${it.codigo}", it.titulo,
                    TipoNotificacion.TICKET, ticketId = it.id, deUserId = user.id)
            }
            notifySolicitante(user, it, "Tu reporte ${it.codigo} fue asignado", "Responsable: ${responsable.nombre}")
        }
        return r.error
    }

    fun changeState(user: User, ticketId: Int, nuevo: EstadoTicket, nota: String): String? {
        val r = mutate(ticketId) { t ->
            if (nuevo == REVISADO) return@mutate Outcome.Fail("Para revisar usa «Revisar y priorizar».")
            if (nuevo !in TicketRules.allowedTransitions(user, t))
                return@mutate Outcome.Fail("No puedes cambiar este ticket a «${nuevo.label}».")
            val reabriendo = t.estado == SOLUCIONADO && nuevo == EN_PROCESO
            val notaObligatoria = nuevo == SOLUCIONADO || nuevo == CANCELADO || reabriendo
            if (notaObligatoria && nota.isBlank()) return@mutate Outcome.Fail("Debes escribir un detalle.")

            val ahora = now()
            val limpia = nota.trim()
            var n = t.copy(estado = nuevo)
            when {
                nuevo == SOLUCIONADO -> n = n.copy(
                    fechaSolucion = ahora,
                    comentarios = n.comentarios + Comentario(user.id, limpia, ahora, TipoComentario.ACCION_REALIZADA)
                )
                nuevo == CERRADO -> n = n.copy(fechaCierre = ahora)
                reabriendo -> n = n.copy(fechaSolucion = null)
            }
            val texto = "Estado: ${t.estado.label} → ${nuevo.label}" +
                    (if (limpia.isNotEmpty() && nuevo != SOLUCIONADO) ". Detalle: $limpia" else "")
            Outcome.Ok(n.copy(historial = n.historial + log(user, texto)))
        }
        r.ticket?.let {
            notifySolicitante(user, it, "Tu reporte ${it.codigo} cambió de estado", "Nuevo estado: ${nuevo.label}")
            if (nuevo == SOLUCIONADO) {
                MessageRepository.notifyAdmins("${it.codigo} marcado como solucionado",
                    "Revisa y confirma el cierre · ${it.titulo}", TipoNotificacion.TICKET, ticketId = it.id, deUserId = user.id)
            }
        }
        return r.error
    }

    fun addComment(user: User, ticketId: Int, texto: String, tipo: TipoComentario): String? {
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canComment(user, t)) return@mutate Outcome.Fail("No puedes comentar este ticket.")
            Validators.obligatorio(texto, "El comentario está vacío.")?.let { return@mutate Outcome.Fail(it) }
            if (tipo != TipoComentario.GENERAL && !TicketRules.isAssignee(user, t))
                return@mutate Outcome.Fail("Ese tipo de comentario es solo para el responsable.")
            Outcome.Ok(t.copy(comentarios = t.comentarios + Comentario(user.id, texto.trim(), now(), tipo)))
        }
        r.ticket?.let { notifySolicitante(user, it, "Nuevo comentario en ${it.codigo}", texto.trim().take(80)) }
        return r.error
    }

    fun addEvidence(user: User, ticketId: Int, fileName: String): String? =
        mutate(ticketId) { t ->
            if (!TicketRules.canAddEvidence(user, t)) return@mutate Outcome.Fail("No puedes adjuntar evidencia ahora.")
            Outcome.Ok(
                t.copy(
                    evidencias = t.evidencias + fileName,
                    historial = t.historial + log(user, "Evidencia de la solución adjuntada")
                )
            )
        }.error

    fun updateByFuncionario(
        user: User, ticketId: Int, detalle: String, gravedad: Prioridad, nuevasFotos: List<String>
    ): String? {
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canEditAsFuncionario(user, t))
                return@mutate Outcome.Fail("Solo puedes editar tus reportes activos.")
            val limpio = detalle.trim()
            val cambiaGravedad = gravedad != t.gravedad
            Validators.edicionReporte(limpio, cambiaGravedad, nuevasFotos.size)?.let {
                return@mutate Outcome.Fail(if (it.startsWith("Mínimo")) "Detalle: ${it.lowercase()}." else it)
            }
            if (t.fotos.size + nuevasFotos.size > MAX_FOTOS_TICKET)
                return@mutate Outcome.Fail("El reporte puede tener como máximo $MAX_FOTOS_TICKET fotos.")

            val ahora = now()
            var n = t
            val eventos = mutableListOf<HistorialItem>()
            if (limpio.isNotEmpty()) {
                n = n.copy(comentarios = n.comentarios + Comentario(user.id, limpio, ahora, TipoComentario.DETALLE_FUNCIONARIO))
                eventos += HistorialItem(ahora, user.id, "El funcionario agregó detalles del problema")
            }
            if (cambiaGravedad) {
                n = n.copy(gravedad = gravedad)
                eventos += HistorialItem(ahora, user.id, "Gravedad actualizada: ${t.gravedad.label} → ${gravedad.label}")
            }
            if (nuevasFotos.isNotEmpty()) {
                n = n.copy(fotos = n.fotos + nuevasFotos)
                eventos += HistorialItem(ahora, user.id, "Se agregaron ${nuevasFotos.size} foto(s) nueva(s) del problema")
            }
            Outcome.Ok(n.copy(historial = n.historial + eventos))
        }
        r.ticket?.let {
            MessageRepository.notifyAdmins("${user.nombre} actualizó ${it.codigo}",
                "${it.titulo} · gravedad ${it.gravedad.label}", TipoNotificacion.TICKET, ticketId = it.id, deUserId = user.id)
        }
        return r.error
    }

    fun requestInfo(user: User, ticketId: Int, mensaje: String): String? {
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canRequestInfo(user, t))
                return@mutate Outcome.Fail("No es posible solicitar información ahora (¿ya hay una solicitud pendiente?).")
            Validators.mensaje(mensaje)?.let { return@mutate Outcome.Fail("Explica qué información necesitas: ${it.lowercase()}.") }
            val plazo = t.prioridadEfectiva.plazoHoras
            val solicitud = SolicitudInfo(
                id = (t.solicitudesInfo.maxOfOrNull { it.id } ?: 0) + 1,
                adminId = user.id, mensaje = mensaje.trim(), fecha = now(), plazoHoras = plazo
            )
            Outcome.Ok(
                t.copy(
                    solicitudesInfo = t.solicitudesInfo + solicitud,
                    historial = t.historial + log(user, "Se solicitó más información al funcionario (plazo $plazo h)")
                )
            )
        }
        r.ticket?.let {
            MessageRepository.notify(it.solicitanteId, "TI necesita más información",
                "${it.codigo} · ${it.titulo}. Plazo: ${it.prioridadEfectiva.plazoHoras} h",
                TipoNotificacion.INFO, ticketId = it.id, deUserId = user.id)
        }
        return r.error
    }

    fun answerInfo(user: User, ticketId: Int, respuesta: String): String? {
        var adminId: Int? = null
        val r = mutate(ticketId) { t ->
            if (!TicketRules.canAnswerInfo(user, t)) return@mutate Outcome.Fail("No hay una solicitud pendiente para responder.")
            Validators.respuestaInfo(respuesta)?.let { return@mutate Outcome.Fail("Respuesta: ${it.lowercase()}.") }
            val pendiente = t.solicitudPendiente ?: return@mutate Outcome.Fail("No hay una solicitud pendiente.")
            adminId = pendiente.adminId
            val ahora = now()
            val aTiempo = ahora <= pendiente.vence
            Outcome.Ok(
                t.copy(
                    solicitudesInfo = t.solicitudesInfo.map {
                        if (it.id == pendiente.id) it.copy(respuesta = respuesta.trim(), fechaRespuesta = ahora) else it
                    },
                    comentarios = t.comentarios + Comentario(user.id, respuesta.trim(), ahora, TipoComentario.RESPUESTA_INFO),
                    historial = t.historial + log(user, "El funcionario respondió la solicitud de información" +
                            if (aTiempo) " (dentro del plazo)" else " (fuera de plazo)")
                )
            )
        }
        val ticket = r.ticket
        val destino = adminId
        if (ticket != null && destino != null) {
            MessageRepository.notify(destino, "Respuesta de ${user.nombre}", "${ticket.codigo} · ${respuesta.trim().take(80)}",
                TipoNotificacion.INFO, ticketId = ticket.id, deUserId = user.id)
        }
        return r.error
    }


    fun restore(list: List<Ticket>) {
        _tickets.value = list
    }

    fun reset() {
        _tickets.value = SeedData.tickets()
    }
}
