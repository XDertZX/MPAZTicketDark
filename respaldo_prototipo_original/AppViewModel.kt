package com.fabiancg.bottomappbar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.Ticket
import com.fabiancg.bottomappbar.data.TicketFilters
import com.fabiancg.bottomappbar.data.TicketRepository
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.TipoComentario
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.data.Users

private const val DAY_MS = 86_400_000L

/**
 * ViewModel compartido por toda la app: guarda la SESIÓN (usuario actual), los FILTROS
 * y expone las acciones que la interfaz puede pedir. Sobrevive a rotaciones de pantalla.
 */
class AppViewModel : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var filters by mutableStateOf(TicketFilters())
        private set

    var filtersVisible by mutableStateOf(false)
        private set

    val tickets = TicketRepository.tickets

    // ----- Sesión -----

    fun login(email: String, password: String): Boolean {
        val user = Users.authenticate(email, password)
        currentUser = user
        filters = TicketFilters()
        filtersVisible = false
        return user != null
    }

    fun logout() {
        currentUser = null
        filters = TicketFilters()
        filtersVisible = false
    }

    // ----- Filtros -----

    fun updateFilters(newFilters: TicketFilters) {
        filters = newFilters
    }

    fun toggleFilters() {
        filtersVisible = !filtersVisible
    }

    /** Primero se restringe por PERMISOS del perfil, y recién después se aplican los filtros. */
    fun visibleTickets(all: List<Ticket>): List<Ticket> {
        val user = currentUser ?: return emptyList()
        val f = filters
        val minDate = f.rango.days?.let { System.currentTimeMillis() - it * DAY_MS }
        return all
            .filter { TicketRules.canView(user, it) }
            .filter { f.estado == null || it.estado == f.estado }
            .filter { f.prioridad == null || it.prioridad == f.prioridad }
            .filter { f.categoria == null || it.categoria == f.categoria }
            .filter { f.responsableId == null || it.asignadoId == f.responsableId }
            .filter {
                f.ubicacion.isBlank() ||
                        "${it.ubicacion} ${it.ubicacionDetalle}".contains(f.ubicacion.trim(), ignoreCase = true)
            }
            .filter { minDate == null || it.fechaCreacion >= minDate }
            .sortedByDescending { it.fechaCreacion }
    }

    // ----- Acciones (todas pasan por el repositorio, que vuelve a validar permisos) -----

    fun createTicket(
        titulo: String, descripcion: String, categoria: Categoria, subcategoria: String,
        ubicacion: String, detalle: String, fotos: List<String>
    ): Ticket? {
        val user = currentUser ?: return null
        return TicketRepository.create(user, titulo, descripcion, categoria, subcategoria, ubicacion, detalle, fotos)
    }

    fun review(ticketId: Int, categoria: Categoria, subcategoria: String, prioridad: Prioridad): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.review(user, ticketId, categoria, subcategoria, prioridad)
    }

    fun assign(ticketId: Int, responsable: User): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.assign(user, ticketId, responsable)
    }

    fun changeState(ticketId: Int, nuevo: EstadoTicket, nota: String): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.changeState(user, ticketId, nuevo, nota)
    }

    fun addComment(ticketId: Int, texto: String, tipo: TipoComentario): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.addComment(user, ticketId, texto, tipo)
    }

    fun addEvidence(ticketId: Int, fileName: String): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.addEvidence(user, ticketId, fileName)
    }

    private companion object {
        const val NO_SESSION = "Tu sesión terminó. Vuelve a ingresar."
    }
}
