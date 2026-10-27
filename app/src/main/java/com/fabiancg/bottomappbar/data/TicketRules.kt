package com.fabiancg.bottomappbar.data

import com.fabiancg.bottomappbar.data.EstadoTicket.ASIGNADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CANCELADO
import com.fabiancg.bottomappbar.data.EstadoTicket.CERRADO
import com.fabiancg.bottomappbar.data.EstadoTicket.EN_PROCESO
import com.fabiancg.bottomappbar.data.EstadoTicket.PENDIENTE
import com.fabiancg.bottomappbar.data.EstadoTicket.REVISADO
import com.fabiancg.bottomappbar.data.EstadoTicket.SOLUCIONADO


object TicketRules {

    fun canCreate(user: User): Boolean = user.role == Role.SOLICITANTE || user.role == Role.ADMIN

    fun canView(user: User, t: Ticket): Boolean = when (user.role) {
        Role.ADMIN -> true
        Role.SOLICITANTE -> t.solicitanteId == user.id
        Role.RESPONSABLE -> t.asignadoId == user.id
    }

    fun allowedTransitions(user: User, t: Ticket): List<EstadoTicket> {
        if (!canView(user, t)) return emptyList()
        val result = linkedSetOf<EstadoTicket>()

        if (user.role == Role.ADMIN) {
            result += when (t.estado) {
                PENDIENTE -> listOf(REVISADO, CANCELADO)
                REVISADO, ASIGNADO, EN_PROCESO -> listOf(CANCELADO)
                SOLUCIONADO -> listOf(CERRADO, EN_PROCESO)
                CERRADO, CANCELADO -> emptyList()
            }
        }
        if (t.asignadoId == user.id) {
            result += when (t.estado) {
                ASIGNADO -> listOf(EN_PROCESO)
                EN_PROCESO -> listOf(SOLUCIONADO)
                else -> emptyList()
            }
        }
        if (t.solicitanteId == user.id && t.estado == PENDIENTE) {
            result += CANCELADO
        }
        return result.toList()
    }

    fun canClassify(user: User, t: Ticket): Boolean =
        user.role == Role.ADMIN && t.estado in listOf(PENDIENTE, REVISADO, ASIGNADO, EN_PROCESO)

    fun canAssign(user: User, t: Ticket): Boolean =
        user.role == Role.ADMIN && t.estado in listOf(REVISADO, ASIGNADO, EN_PROCESO)

    fun canComment(user: User, t: Ticket): Boolean = canView(user, t) && !t.estado.esFinal

    fun isAssignee(user: User, t: Ticket): Boolean = t.asignadoId == user.id

    fun canAddEvidence(user: User, t: Ticket): Boolean = isAssignee(user, t) && t.estado == EN_PROCESO

    fun canEditAsFuncionario(user: User, t: Ticket): Boolean =
        user.role == Role.SOLICITANTE && t.solicitanteId == user.id && t.estado.enCurso

    fun canRequestInfo(user: User, t: Ticket): Boolean =
        user.role == Role.ADMIN && t.estado.enCurso && t.solicitudPendiente == null

    fun canAnswerInfo(user: User, t: Ticket): Boolean =
        t.solicitanteId == user.id && t.solicitudPendiente != null && !t.estado.esFinal

    fun canViewConversation(user: User, c: Conversacion): Boolean =
        user.role == Role.ADMIN || c.funcionarioId == user.id
}
