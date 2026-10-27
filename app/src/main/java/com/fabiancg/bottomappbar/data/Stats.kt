package com.fabiancg.bottomappbar.data

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
data class ResumenPeriodo(val titulo: String, val tickets: List<Ticket>) {
    val total: Int get() = tickets.size
    val pendientes: Int get() = tickets.count { it.estado.enCurso }
    val revisados: Int get() = total - pendientes
    val resueltos: Int get() = tickets.count { it.estado.resuelto }
    val cancelados: Int get() = tickets.count { it.estado == EstadoTicket.CANCELADO }

    val porPrioridad: List<Pair<Prioridad?, Int>>
        get() = listOf(Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA, null)
            .map { p -> p to tickets.count { it.prioridad == p } }

    val prioridadMasFrecuente: Prioridad?
        get() = porPrioridad.filter { it.first != null && it.second > 0 }.maxByOrNull { it.second }?.first

    val porCategoria: List<Pair<Categoria, Int>>
        get() = Categoria.entries.map { c -> c to tickets.count { it.categoria == c } }.sortedByDescending { it.second }

    val subcategoriaMasFrecuente: Pair<String, Int>?
        get() = tickets.groupingBy { it.subcategoria }.eachCount().maxByOrNull { it.value }?.toPair()
}

object Stats {
    val localeEs: Locale = Locale.forLanguageTag("es-CL")

    private fun zone(): ZoneId = ZoneId.systemDefault()

    fun monthName(month: Month): String =
        month.getDisplayName(TextStyle.FULL, localeEs).replaceFirstChar { it.uppercaseChar() }

    fun monthTitle(ym: YearMonth): String = "${monthName(ym.month)} ${ym.year}"

    private fun rango(tickets: List<Ticket>, desde: LocalDate, hastaExclusivo: LocalDate, titulo: String): ResumenPeriodo {
        val a = desde.atStartOfDay(zone()).toInstant().toEpochMilli()
        val b = hastaExclusivo.atStartOfDay(zone()).toInstant().toEpochMilli()
        return ResumenPeriodo(titulo, tickets.filter { it.fechaCreacion in a until b }.sortedBy { it.fechaCreacion })
    }

    fun mes(tickets: List<Ticket>, ym: YearMonth): ResumenPeriodo =
        rango(tickets, ym.atDay(1), ym.plusMonths(1).atDay(1), monthTitle(ym))

    fun semestre(tickets: List<Ticket>, year: Int, semestre: Int): ResumenPeriodo {
        val inicio = YearMonth.of(year, if (semestre == 1) 1 else 7)
        val nombre = if (semestre == 1) "Primer semestre" else "Segundo semestre"
        return rango(tickets, inicio.atDay(1), inicio.plusMonths(6).atDay(1), "$nombre $year")
    }

    fun bimestres(tickets: List<Ticket>, year: Int, semestre: Int): List<ResumenPeriodo> {
        val mesInicio = if (semestre == 1) 1 else 7
        return (0..2).map { i ->
            val ym = YearMonth.of(year, mesInicio + i * 2)
            val numero = (if (semestre == 1) 1 else 4) + i
            rango(
                tickets, ym.atDay(1), ym.plusMonths(2).atDay(1),
                "Bimestre $numero · ${monthName(ym.month)} – ${monthName(ym.plusMonths(1).month)}"
            )
        }
    }
}
