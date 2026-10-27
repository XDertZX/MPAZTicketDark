package com.fabiancg.bottomappbar.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


object LocalStore {

    private const val PREFS = "mpaz_ticket_store"
    private const val KEY_STATE = "state_v2"
    private const val KEY_SESSION = "session_user_id"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        p.getString(KEY_STATE, null)?.let { load(it) }
        StoreEvents.listener = { save() }
        if (p.getString(KEY_STATE, null) == null) save()
    }


    fun sessionUserId(): Int? = prefs?.getInt(KEY_SESSION, -1)?.takeIf { it > 0 }

    fun saveSession(userId: Int?) {
        prefs?.edit()?.apply {
            if (userId == null) remove(KEY_SESSION) else putInt(KEY_SESSION, userId)
            apply()
        }
    }



    fun save() {
        val p = prefs ?: return
        try {
            val root = JSONObject()
            root.put("users", JSONArray().apply { Users.registered.value.forEach { put(userToJson(it)) } })
            root.put("tickets", JSONArray().apply { TicketRepository.tickets.value.forEach { put(ticketToJson(it)) } })
            root.put("conversaciones", JSONArray().apply { MessageRepository.conversaciones.value.forEach { put(convToJson(it)) } })
            root.put("notificaciones", JSONArray().apply { MessageRepository.notificaciones.value.forEach { put(notifToJson(it)) } })
            root.put("contactos", JSONArray().apply { MessageRepository.contactos.value.forEach { put(contactoToJson(it)) } })
            root.put("documentos", JSONArray().apply { DocumentRepository.documentos.value.forEach { put(docToJson(it)) } })
            p.edit().putString(KEY_STATE, root.toString()).apply()
        } catch (e: JSONException) {
        }
    }

    private fun load(raw: String) {
        try {
            val root = JSONObject(raw)
            Users.restore(root.optJSONArray("users").mapObjects { jsonToUser(it) })
            TicketRepository.restore(root.optJSONArray("tickets").mapObjects { jsonToTicket(it) })
            MessageRepository.restore(
                root.optJSONArray("conversaciones").mapObjects { jsonToConv(it) },
                root.optJSONArray("notificaciones").mapObjects { jsonToNotif(it) },
                root.optJSONArray("contactos").mapObjects { jsonToContacto(it) }
            )
            DocumentRepository.restore(root.optJSONArray("documentos").mapObjects { jsonToDoc(it) })
        } catch (e: JSONException) {

            resetAll()
        }
    }


    fun resetAll() {
        Users.reset()
        TicketRepository.reset()
        MessageRepository.reset()
        appContext?.let { DocumentRepository.reset(it) }
        saveSession(null)
        save()
    }


    private fun <T> JSONArray?.mapObjects(block: (JSONObject) -> T?): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            try {
                block(obj)?.let { out += it }
            } catch (e: JSONException) {
            } catch (e: IllegalArgumentException) {

            }
        }
        return out
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it, "").takeIf { s -> s.isNotEmpty() } }
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.stringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.putOpt2(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)

    private fun strArray(list: List<String>) = JSONArray().apply { list.forEach { put(it) } }


    private fun userToJson(u: User) = JSONObject()
        .put("id", u.id).put("nombre", u.nombre).put("cargo", u.cargo).put("email", u.email)
        .put("hash", u.passwordHash).put("role", u.role.name).put("tel", u.telefono)

    private fun jsonToUser(o: JSONObject) = User(
        id = o.getInt("id"), nombre = o.getString("nombre"), cargo = o.getString("cargo"),
        email = o.getString("email"), passwordHash = o.getString("hash"),
        role = Role.valueOf(o.getString("role")), telefono = o.optString("tel", ""), registrado = true
    )



    private fun ticketToJson(t: Ticket): JSONObject = JSONObject()
        .put("id", t.id).put("titulo", t.titulo).put("descripcion", t.descripcion)
        .put("categoria", t.categoria.name).put("sub", t.subcategoria)
        .put("ubic", t.ubicacion).put("ubicDet", t.ubicacionDetalle)
        .put("sol", t.solicitanteId).put("estado", t.estado.name)
        .putOpt2("prioridad", t.prioridad?.name).put("gravedad", t.gravedad.name)
        .putOpt2("asignado", t.asignadoId).putOpt2("admin", t.adminId)
        .put("fCre", t.fechaCreacion).putOpt2("fAsig", t.fechaAsignacion)
        .putOpt2("fSol", t.fechaSolucion).putOpt2("fCie", t.fechaCierre)
        .put("fotos", strArray(t.fotos)).put("evid", strArray(t.evidencias))
        .put("coment", JSONArray().apply {
            t.comentarios.forEach {
                put(JSONObject().put("a", it.autorId).put("t", it.texto).put("f", it.fecha).put("k", it.tipo.name))
            }
        })
        .put("hist", JSONArray().apply {
            t.historial.forEach { put(JSONObject().put("f", it.fecha).put("u", it.usuarioId).put("d", it.descripcion)) }
        })
        .put("info", JSONArray().apply {
            t.solicitudesInfo.forEach {
                put(
                    JSONObject().put("id", it.id).put("admin", it.adminId).put("m", it.mensaje)
                        .put("f", it.fecha).put("h", it.plazoHoras)
                        .putOpt2("r", it.respuesta).putOpt2("fr", it.fechaRespuesta)
                )
            }
        })

    private fun jsonToTicket(o: JSONObject): Ticket = Ticket(
        id = o.getInt("id"), titulo = o.getString("titulo"), descripcion = o.getString("descripcion"),
        categoria = Categoria.valueOf(o.getString("categoria")), subcategoria = o.getString("sub"),
        ubicacion = o.getString("ubic"), ubicacionDetalle = o.optString("ubicDet", ""),
        solicitanteId = o.getInt("sol"), estado = EstadoTicket.valueOf(o.getString("estado")),
        prioridad = o.stringOrNull("prioridad")?.let { Prioridad.valueOf(it) },
        gravedad = o.stringOrNull("gravedad")?.let { Prioridad.valueOf(it) } ?: Prioridad.MEDIA,
        asignadoId = o.intOrNull("asignado"), adminId = o.intOrNull("admin"),
        fechaCreacion = o.getLong("fCre"), fechaAsignacion = o.longOrNull("fAsig"),
        fechaSolucion = o.longOrNull("fSol"), fechaCierre = o.longOrNull("fCie"),
        fotos = o.optJSONArray("fotos").strings(), evidencias = o.optJSONArray("evid").strings(),
        comentarios = o.optJSONArray("coment").mapObjects {
            Comentario(it.getInt("a"), it.getString("t"), it.getLong("f"), TipoComentario.valueOf(it.getString("k")))
        },
        historial = o.optJSONArray("hist").mapObjects {
            HistorialItem(it.getLong("f"), it.getInt("u"), it.getString("d"))
        },
        solicitudesInfo = o.optJSONArray("info").mapObjects {
            SolicitudInfo(
                id = it.getInt("id"), adminId = it.getInt("admin"), mensaje = it.getString("m"),
                fecha = it.getLong("f"), plazoHoras = it.getInt("h"),
                respuesta = it.stringOrNull("r"), fechaRespuesta = it.longOrNull("fr")
            )
        }
    )


    private fun convToJson(c: Conversacion) = JSONObject()
        .put("id", c.id).put("func", c.funcionarioId).put("admin", c.adminId).put("asunto", c.asunto)
        .put("abierta", c.abierta).put("fCre", c.fechaCreacion).putOpt2("fCie", c.fechaCierre)
        .put("nla", c.noLeidaAdmin).put("nlf", c.noLeidaFuncionario)
        .put("msgs", JSONArray().apply {
            c.mensajes.forEach { put(JSONObject().put("a", it.autorId).put("t", it.texto).put("f", it.fecha)) }
        })

    private fun jsonToConv(o: JSONObject) = Conversacion(
        id = o.getInt("id"), funcionarioId = o.getInt("func"), adminId = o.getInt("admin"),
        asunto = o.getString("asunto"), abierta = o.optBoolean("abierta", true),
        fechaCreacion = o.getLong("fCre"), fechaCierre = o.longOrNull("fCie"),
        noLeidaAdmin = o.optBoolean("nla", false), noLeidaFuncionario = o.optBoolean("nlf", false),
        mensajes = o.optJSONArray("msgs").mapObjects { Mensaje(it.getInt("a"), it.getString("t"), it.getLong("f")) }
    )

    private fun notifToJson(n: Notificacion) = JSONObject()
        .put("id", n.id).put("para", n.paraUserId).put("titulo", n.titulo).put("texto", n.texto)
        .put("f", n.fecha).put("tipo", n.tipo.name).putOpt2("ticket", n.ticketId)
        .putOpt2("conv", n.conversacionId).putOpt2("de", n.deUserId).put("leida", n.leida)

    private fun jsonToNotif(o: JSONObject) = Notificacion(
        id = o.getInt("id"), paraUserId = o.getInt("para"), titulo = o.getString("titulo"),
        texto = o.optString("texto", ""), fecha = o.getLong("f"), tipo = TipoNotificacion.valueOf(o.getString("tipo")),
        ticketId = o.intOrNull("ticket"), conversacionId = o.intOrNull("conv"), deUserId = o.intOrNull("de"),
        leida = o.optBoolean("leida", false)
    )

    private fun contactoToJson(c: RegistroContacto) = JSONObject()
        .put("id", c.id).put("func", c.funcionarioId).put("admin", c.adminId).put("tipo", c.tipo.name).put("f", c.fecha)

    private fun jsonToContacto(o: JSONObject) = RegistroContacto(
        o.getInt("id"), o.getInt("func"), o.getInt("admin"), TipoContacto.valueOf(o.getString("tipo")), o.getLong("f")
    )


    private fun docToJson(d: Documento) = JSONObject()
        .put("id", d.id).put("owner", d.ownerId).put("nombre", d.nombre).put("archivo", d.archivo)
        .put("mime", d.mime).put("size", d.tamano).put("f", d.fecha).put("origen", d.origen)

    private fun jsonToDoc(o: JSONObject) = Documento(
        id = o.getInt("id"), ownerId = o.getInt("owner"), nombre = o.getString("nombre"),
        archivo = o.getString("archivo"), mime = o.getString("mime"), tamano = o.optLong("size", 0L),
        fecha = o.getLong("f"), origen = o.optString("origen", "Cargado por el usuario")
    )
}
