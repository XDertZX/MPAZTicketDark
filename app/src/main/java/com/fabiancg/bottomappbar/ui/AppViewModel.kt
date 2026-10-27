package com.fabiancg.bottomappbar.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fabiancg.bottomappbar.data.Categoria
import com.fabiancg.bottomappbar.data.Conversacion
import com.fabiancg.bottomappbar.data.DocumentRepository
import com.fabiancg.bottomappbar.data.Documento
import com.fabiancg.bottomappbar.data.EstadoTicket
import com.fabiancg.bottomappbar.data.LocalStore
import com.fabiancg.bottomappbar.data.MessageRepository
import com.fabiancg.bottomappbar.data.Notificacion
import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.ReportDraft
import com.fabiancg.bottomappbar.data.Role
import com.fabiancg.bottomappbar.data.Ticket
import com.fabiancg.bottomappbar.data.TicketFilters
import com.fabiancg.bottomappbar.data.TicketRepository
import com.fabiancg.bottomappbar.data.TicketRules
import com.fabiancg.bottomappbar.data.TipoComentario
import com.fabiancg.bottomappbar.data.TipoContacto
import com.fabiancg.bottomappbar.data.User
import com.fabiancg.bottomappbar.data.Users
import com.fabiancg.bottomappbar.data.Validators

private const val DAY_MS = 86_400_000L
class AppViewModel : ViewModel() {

    var currentUser by mutableStateOf(LocalStore.sessionUserId()?.let { Users.byId(it) })
        private set

    var filters by mutableStateOf(TicketFilters())
        private set

    var filtersVisible by mutableStateOf(false)
        private set

    val tickets = TicketRepository.tickets
    val conversaciones = MessageRepository.conversaciones
    val notificaciones = MessageRepository.notificaciones
    val contactos = MessageRepository.contactos
    val documentos = DocumentRepository.documentos

    fun login(email: String, password: String): String? {
        Validators.email(email)?.let { return it }
        Validators.passwordLogin(password)?.let { return it }
        val user = Users.authenticate(email, password) ?: return "Correo o contraseña incorrectos."
        startSession(user)
        return null
    }

    fun register(nombre: String, cargo: String, email: String, telefono: String, password: String, confirm: String): String? {
        Validators.nombre(nombre)?.let { return "Nombre: $it" }
        Validators.cargo(cargo)?.let { return it }
        Validators.emailRegistro(email, Users.emailExists(email))?.let { return it }
        Validators.telefono(telefono)?.let { return "Teléfono: $it" }
        Validators.nuevaPassword(password)?.let { return "Contraseña: $it" }
        Validators.confirmacion(password, confirm)?.let { return it }
        startSession(Users.register(nombre, cargo, email, telefono, password))
        return null
    }

    private fun startSession(user: User) {
        currentUser = user
        filters = TicketFilters()
        filtersVisible = false
        LocalStore.saveSession(user.id)
    }

    fun logout() {
        currentUser = null
        filters = TicketFilters()
        filtersVisible = false
        LocalStore.saveSession(null)
    }

    fun resetDemoData() {
        LocalStore.resetAll()
        currentUser = null
        filters = TicketFilters()
    }


    fun updateFilters(newFilters: TicketFilters) {
        filters = newFilters
    }

    fun toggleFilters() {
        filtersVisible = !filtersVisible
    }

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

    fun misReportes(all: List<Ticket>): List<Ticket> {
        val user = currentUser ?: return emptyList()
        return all.filter { it.solicitanteId == user.id }.sortedByDescending { it.fechaCreacion }
    }

    fun unreadNotifications(all: List<Notificacion>): Int {
        val user = currentUser ?: return 0
        return all.count { it.paraUserId == user.id && !it.leida }
    }

    fun unreadConversations(all: List<Conversacion>): Int {
        val user = currentUser ?: return 0
        return when (user.role) {
            Role.ADMIN -> all.count { it.noLeidaAdmin }
            else -> all.count { it.funcionarioId == user.id && it.noLeidaFuncionario }
        }
    }

    fun createTicket(draft: ReportDraft): Ticket? {
        val user = currentUser ?: return null
        return TicketRepository.create(user, draft)
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

    fun updateReport(ticketId: Int, detalle: String, gravedad: Prioridad, fotos: List<String>): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.updateByFuncionario(user, ticketId, detalle, gravedad, fotos)
    }

    fun requestInfo(ticketId: Int, mensaje: String): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.requestInfo(user, ticketId, mensaje)
    }

    fun answerInfo(ticketId: Int, respuesta: String): String? {
        val user = currentUser ?: return NO_SESSION
        return TicketRepository.answerInfo(user, ticketId, respuesta)
    }


    fun registerContact(admin: User, tipo: TipoContacto) {
        val user = currentUser ?: return
        if (user.role != Role.ADMIN) MessageRepository.registrarContacto(user, admin, tipo)
    }

    fun newConversation(admin: User, asunto: String, texto: String): Pair<String?, Int?> {
        val user = currentUser ?: return NO_SESSION to null
        return MessageRepository.nuevaConversacion(user, admin, asunto, texto)
    }

    fun reply(conversacionId: Int, texto: String): String? {
        val user = currentUser ?: return NO_SESSION
        return MessageRepository.responder(user, conversacionId, texto)
    }

    fun closeConversation(conversacionId: Int): String? {
        val user = currentUser ?: return NO_SESSION
        return MessageRepository.cerrar(user, conversacionId)
    }

    fun markConversationRead(conversacionId: Int) {
        currentUser?.let { MessageRepository.marcarLeida(it, conversacionId) }
    }

    fun clearClosedConversations(): Int {
        val user = currentUser ?: return 0
        return MessageRepository.borrarCerradas(user)
    }

    fun markNotificationRead(id: Int) {
        currentUser?.let { MessageRepository.markRead(it.id, id) }
    }

    fun markAllNotificationsRead() {
        currentUser?.let { MessageRepository.markAllRead(it.id) }
    }


    fun importDocument(context: Context, uri: Uri): Pair<Documento?, String?> {
        val user = currentUser ?: return null to NO_SESSION
        return DocumentRepository.importFromUri(context, user, uri)
    }

    fun deleteDocument(context: Context, docId: Int): String? {
        val user = currentUser ?: return NO_SESSION
        return DocumentRepository.delete(context, user, docId)
    }

    private companion object {
        const val NO_SESSION = "Tu sesión terminó. Vuelve a ingresar."
    }
}
