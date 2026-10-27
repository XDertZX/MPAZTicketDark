package com.fabiancg.bottomappbar

import com.fabiancg.bottomappbar.data.Prioridad
import com.fabiancg.bottomappbar.data.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun email_valido_e_invalido() {
        assertNull(Validators.email("ana@demo.cl"))
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("ana@demo"))
        assertNotNull(Validators.email("ana.demo.cl"))
    }

    @Test
    fun email_registro_rechaza_correo_existente() {
        assertNotNull(Validators.emailRegistro("ana@demo.cl", yaExiste = true))
        assertNull(Validators.emailRegistro("nuevo@demo.cl", yaExiste = false))
    }

    @Test
    fun nombre_solo_letras_y_minimo_tres() {
        assertNull(Validators.nombre("Ana Torres"))
        assertNotNull(Validators.nombre("An"))
        assertNotNull(Validators.nombre("Ana123"))
    }

    @Test
    fun telefono_chileno_de_nueve_digitos() {
        assertNull(Validators.telefono("9 1234 5678"))
        assertNull(Validators.telefono("+56 9 1234 5678"))
        assertNotNull(Validators.telefono("1234"))
        assertNotNull(Validators.telefono("8 1234 5678"))
    }

    @Test
    fun password_nueva_requiere_letras_y_numeros() {
        assertNull(Validators.nuevaPassword("clave123"))
        assertNotNull(Validators.nuevaPassword("abc"))
        assertNotNull(Validators.nuevaPassword("soloLetras"))
        assertNotNull(Validators.nuevaPassword("12345678"))
    }

    @Test
    fun confirmacion_de_password() {
        assertNull(Validators.confirmacion("clave123", "clave123"))
        assertNotNull(Validators.confirmacion("clave123", "clave124"))
        assertNotNull(Validators.confirmacion("clave123", ""))
    }

    @Test
    fun titulo_y_descripcion_del_reporte() {
        assertNull(Validators.titulo("Silla rota"))
        assertNotNull(Validators.titulo("Sil"))
        assertNull(Validators.descripcion("La silla tiene una pata suelta"))
        assertNotNull(Validators.descripcion("Rota"))
    }

    @Test
    fun seleccion_obligatoria() {
        assertNotNull(Validators.seleccion(null, "Selecciona la gravedad"))
        assertNull(Validators.seleccion(Prioridad.ALTA, "Selecciona la gravedad"))
    }

    @Test
    fun edicion_sin_cambios_no_se_permite() {
        assertEquals("No hay cambios para guardar.", Validators.edicionReporte("", cambiaGravedad = false, fotosNuevas = 0))
        assertNull(Validators.edicionReporte("", cambiaGravedad = true, fotosNuevas = 0))
        assertNotNull(Validators.edicionReporte("corto", cambiaGravedad = false, fotosNuevas = 0))
        assertNull(Validators.edicionReporte("Ahora también gotea el techo", cambiaGravedad = false, fotosNuevas = 0))
    }

    @Test
    fun plazos_de_respuesta_segun_prioridad() {
        assertEquals(2, Prioridad.ALTA.plazoHoras)
        assertEquals(6, Prioridad.MEDIA.plazoHoras)
        assertEquals(8, Prioridad.BAJA.plazoHoras)
    }
}
