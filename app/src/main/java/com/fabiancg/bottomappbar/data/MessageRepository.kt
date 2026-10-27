package com.fabiancg.bottomappbar.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MessageRepository {

    private val lock = Any()

    private val _conversaciones = MutableStateFlow(SeedData.conversaciones())
    val conversaciones: StateFlow<List<Conversacion>> = _conversaciones.asStateFlow()

    private val _notificaciones = MutableStateFlow(SeedData.notificaciones())
    val notificaciones: StateFlow<List<Notificacion>> = _notificaciones.asStateFlow()

    private val _contactos = MutableStateFlow(SeedData.contactos())
    val contactos: StateFlow<List<RegistroContacto>> = _contactos.asStateFlow()

    private fun now() = System.currentTimeMillis()


    fun notify(
        paraUserId: Int, titulo: String, texto: String, tipo: TipoNotificacion,
        ticketId: Int? = null, conversacionId: Int? = null, deUserId: Int? = null
    ) {
        synchronized(lock) {
            val id = (_notificaciones.value.maxOfOrNull { it.id } ?: 0) + 1
            _notificaciones.value = _notificaciones.value +
                    Notificacion(id, paraUserId, titulo, texto, now(), tipo, ticketId, conversacionId, deUserId)
        }
        StoreEvents.changed()
    }

    fun notifyAdmins(
        titulo: String, texto: String, tipo: TipoNotificacion,
        ticketId: Int? = null, conversacionId: Int? = null, deUserId: Int? = null
    ) {
        Users.admins.filter { it.id != deUserId }.forEach {
            notify(it.id, titulo, texto, tipo, ticketId, conversacionId, deUserId)
        }
    }

    fun markRead(userId: Int, notificacionId: Int) {
        _notificaciones.value = _notificaciones.value.map {
            if (it.id == notificacionId && it.paraUserId == userId) it.copy(leida = true) else it
        }
        StoreEvents.changed()
    }

    fun markAllRead(userId: Int) {
        _notificaciones.value = _notificaciones.value.map { if (it.paraUserId == userId) it.copy(leida = true) else it }
        StoreEvents.changed()
    }


    fun registrarContacto(funcionario: User, admin: User, tipo: TipoContacto) {
        synchronized(lock) {
            val id = (_contactos.value.maxOfOrNull { it.id } ?: 0) + 1
            _contactos.value = _contactos.value + RegistroContacto(id, funcionario.id, admin.id, tipo, now())
        }
        val titulo = when (tipo) {
            TipoContacto.LLAMADA -> "${funcionario.nombre} intentó llamarte"
            TipoContacto.CORREO -> "${funcionario.nombre} te escribió un correo"
            TipoContacto.MENSAJE -> "Nuevo mensaje de ${funcionario.nombre}"
        }
        notify(admin.id, titulo, "${tipo.label} registrada desde «Contactar a un Administrador»",
            TipoNotificacion.CONTACTO, deUserId = funcionario.id)
    }


    fun nuevaConversacion(funcionario: User, admin: User, asunto: String, texto: String): Pair<String?, Int?> {
        if (funcionario.role == Role.ADMIN) return "Los administradores responden desde «Mensajes»." to null
        if (admin.role != Role.ADMIN) return "Destinatario no válido." to null
        Validators.asunto(asunto)?.let { return "Asunto: ${it.lowercase()}." to null }
        Validators.mensaje(texto)?.let { return "Mensaje: ${it.lowercase()}." to null }
        val id = synchronized(lock) {
            val nuevoId = (_conversaciones.value.maxOfOrNull { it.id } ?: 0) + 1
            val ahora = now()
            _conversaciones.value = _conversaciones.value + Conversacion(
                id = nuevoId, funcionarioId = funcionario.id, adminId = admin.id, asunto = asunto.trim(),
                mensajes = listOf(Mensaje(funcionario.id, texto.trim().take(500), ahora)),
                fechaCreacion = ahora, noLeidaAdmin = true
            )
            nuevoId
        }
        notify(admin.id, "Nuevo mensaje de ${funcionario.nombre}", asunto.trim(), TipoNotificacion.MENSAJE,
            conversacionId = id, deUserId = funcionario.id)
        return null to id
    }

    fun responder(user: User, conversacionId: Int, texto: String): String? {
        val c = _conversaciones.value.firstOrNull { it.id == conversacionId } ?: return "La conversación no existe."
        if (!TicketRules.canViewConversation(user, c)) return "No tienes acceso a esta conversación."
        if (!c.abierta) return "La conversación está cerrada. Envía una nueva solicitud de mensaje."
        Validators.obligatorio(texto, "El mensaje está vacío.")?.let { return it }
        val esAdmin = user.role == Role.ADMIN
        val actualizada = c.copy(
            mensajes = c.mensajes + Mensaje(user.id, texto.trim().take(500), now()),
            noLeidaAdmin = !esAdmin,
            noLeidaFuncionario = esAdmin
        )
        _conversaciones.value = _conversaciones.value.map { if (it.id == c.id) actualizada else it }
        if (esAdmin) {
            notify(c.funcionarioId, "${user.nombre} respondió tu mensaje", c.asunto, TipoNotificacion.MENSAJE,
                conversacionId = c.id, deUserId = user.id)
        } else {
            notify(c.adminId, "Nuevo mensaje de ${user.nombre}", c.asunto, TipoNotificacion.MENSAJE,
                conversacionId = c.id, deUserId = user.id)
        }
        return null
    }

    fun cerrar(admin: User, conversacionId: Int): String? {
        if (admin.role != Role.ADMIN) return "Solo un administrador puede cerrar mensajes."
        val c = _conversaciones.value.firstOrNull { it.id == conversacionId } ?: return "La conversación no existe."
        if (!c.abierta) return "La conversación ya estaba cerrada."
        _conversaciones.value = _conversaciones.value.map {
            if (it.id == c.id) it.copy(abierta = false, fechaCierre = now(), noLeidaAdmin = false, noLeidaFuncionario = true) else it
        }
        notify(c.funcionarioId, "Conversación cerrada", "«${c.asunto}» fue cerrada por ${admin.nombre}. " +
                "Si necesitas algo más, envía una nueva solicitud de mensaje.", TipoNotificacion.MENSAJE,
            conversacionId = c.id, deUserId = admin.id)
        return null
    }

    fun marcarLeida(user: User, conversacionId: Int) {
        val c = _conversaciones.value.firstOrNull { it.id == conversacionId } ?: return
        val esAdmin = user.role == Role.ADMIN
        if ((esAdmin && !c.noLeidaAdmin) || (!esAdmin && !c.noLeidaFuncionario)) return
        _conversaciones.value = _conversaciones.value.map {
            if (it.id != c.id) it
            else if (esAdmin) it.copy(noLeidaAdmin = false) else it.copy(noLeidaFuncionario = false)
        }
        StoreEvents.changed()
    }

    fun borrarCerradas(admin: User): Int {
        if (admin.role != Role.ADMIN) return 0
        val cerradas = _conversaciones.value.count { !it.abierta }
        if (cerradas == 0) return 0
        _conversaciones.value = _conversaciones.value.filter { it.abierta }
        StoreEvents.changed()
        return cerradas
    }


    fun restore(conversaciones: List<Conversacion>, notificaciones: List<Notificacion>, contactos: List<RegistroContacto>) {
        _conversaciones.value = conversaciones
        _notificaciones.value = notificaciones
        _contactos.value = contactos
    }

    fun reset() {
        restore(SeedData.conversaciones(), SeedData.notificaciones(), SeedData.contactos())
    }
}
