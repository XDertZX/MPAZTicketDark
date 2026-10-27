package com.fabiancg.bottomappbar.data

import com.fabiancg.bottomappbar.data.EstadoTicket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

object TicketRepository {

    private val _tickets = MutableStateFlow(SeedData.tickets())
    val tickets: StateFlow<List<Ticket>> = _tickets.asStateFlow()

    private val lastId = AtomicInteger(_tickets.value.maxOf { it.id })

    private sealed interface Outcome {
        data class Ok(val ticket: Ticket) : Outcome
        data class Fail(val message: String) : Outcome
    }

    private fun now() = System.currentTimeMillis()

    private fun mutate(ticketId: Int, block: (Ticket) -> Outcome): String? {
        if (_tickets.value.none { it.id == ticketId }) return "El ticket no existe."
        var error: String? = null
        _tickets.update { list ->
            error = null
            list.map { t ->
                if (t.id != ticketId) t
                else when (val r = block(t)) {
                    is Outcome.Ok -> r.ticket
                    is Outcome.Fail -> { error = r.message; t }
                }
            }
        }
        return error
    }

    private fun log(user: User, text: String) = HistorialItem(now(), user.id, text)


    fun create(
        user: User, titulo: String, descripcion: String, categoria: Categoria, subcategoria: String,
        ubicacion: String, ubicacionDetalle: String, fotos: List<String>
    ): Ticket? {
        if (!TicketRules.canCreate(user)) return null
        if (titulo.isBlank() || descripcion.isBlank()) return null
        val ticket = Ticket(
            id = lastId.incrementAndGet(),
            titulo = titulo.trim(), descripcion = descripcion.trim(),
            categoria = categoria, subcategoria = subcategoria,
            ubicacion = ubicacion, ubicacionDetalle = ubicacionDetalle.trim(),
            solicitanteId = user.id, fechaCreacion = now(), fotos = fotos,
            historial = listOf(log(user, "Ticket creado"))
        )
        _tickets.update { it + ticket }
        return ticket
    }

    fun review(user: User, ticketId: Int, categoria: Categoria, subcategoria: String, prioridad: Prioridad): String? =
        mutate(ticketId) { t ->
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

    fun assign(user: User, ticketId: Int, responsable: User): String? =
        mutate(ticketId) { t ->
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


    fun changeState(user: User, ticketId: Int, nuevo: EstadoTicket, nota: String): String? =
        mutate(ticketId) { t ->
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


    fun addComment(user: User, ticketId: Int, texto: String, tipo: TipoComentario): String? =
        mutate(ticketId) { t ->
            if (!TicketRules.canComment(user, t)) return@mutate Outcome.Fail("No puedes comentar este ticket.")
            if (texto.isBlank()) return@mutate Outcome.Fail("El comentario está vacío.")
            if (tipo != TipoComentario.GENERAL && !TicketRules.isAssignee(user, t))
                return@mutate Outcome.Fail("Ese tipo de comentario es solo para el responsable.")
            Outcome.Ok(t.copy(comentarios = t.comentarios + Comentario(user.id, texto.trim(), now(), tipo)))
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
        }
}
