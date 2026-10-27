# MPAZ Ticket

Aplicación móvil Android para la **gestión de solicitudes de mantención** de la Escuela Escritora Marcela Paz (San Bernardo).
Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC).

> Prototipo académico: todos los usuarios, reportes, teléfonos, correos y mensajes son **ficticios**.
> La aplicación **no reemplaza los procedimientos de emergencia** de la escuela.

## Integrantes

- _Nombre Apellido 1_
- _Nombre Apellido 2_

## Problema y solución

Hoy los problemas de infraestructura, mobiliario y tecnología se informan de forma verbal, sin registro ni seguimiento.
MPAZ Ticket centraliza los reportes: cada funcionario registra el problema (categoría, ubicación, descripción, gravedad y fotos),
recibe un número de ticket (`MP-0001`) y puede seguir su estado. El encargado de TI prioriza, asigna, pide más información
y revisa estadísticas; el personal responsable actualiza el avance hasta cerrar el ticket.

## Funcionalidades implementadas

**General**
- Barra superior (azul marino `#062451` / dorado `#B88A00`) con menú lateral y notificaciones.
- Barra inferior: Inicio · Reportes · Cuenta.
- Vistas de registro, inicio de sesión y cierre de sesión. Menú desplegable con cuentas de demostración.
- Formularios validados por campo con íconos de error/válido y mensajes de ayuda.
- Animaciones: transiciones entre pantallas, contenido animado, barras de estadísticas, listas animadas.

**Funcionario**
- Inicio con resumen de la app y creación de reporte en la misma página (pide login/registro al enviar).
- Mis reportes con selector Pendientes / Revisados y edición de reportes activos (detalles, gravedad, fotos).
- Respuesta a solicitudes de información de TI con plazo (Alta 2 h · Media 6 h · Baja 8 h).
- Contactar a un administrador por correo, teléfono o mensaje interno.

**Administrador**
- Inicio con resumen mensual (reportes, pendientes, resueltos y prioridades más frecuentes).
- Ver reportes: activos con resumen de 3 líneas, botón Entrar, filtros e historial.
- Revisar y priorizar, asignar/reasignar, cambiar estado, solicitar más información.
- Panel de administrador: resumen mensual y semestral (por bimestres) con descarga en PDF.
- Mensajes agrupados por funcionario: responder, llamar, cerrar y borrar historial de cerrados.

**Responsable de la solución**
- Tickets asignados: iniciar trabajo, comentar, pedir materiales, adjuntar evidencia y marcar solucionado.

## Tecnologías y arquitectura

- Kotlin + Jetpack Compose + **Material 3**
- **MVVM**: `ui/screens` (vistas) → `AppViewModel` (estado) → repositorios con `StateFlow` (`data/`)
- Navigation Compose
- Persistencia local: `SharedPreferences` + JSON (`LocalStore.kt`)
- Validaciones centralizadas en `Validators` (`data/Models.kt`) con pruebas unitarias (`ValidatorsTest`)

```
app/src/main/java/com/fabiancg/bottomappbar/
├── data/         Modelos, reglas, repositorios, almacenamiento local, estadísticas y PDF
├── ui/
│   ├── components/   Barras, menú y componentes reutilizables
│   ├── screens/      Pantallas de la app
│   ├── theme/        Colores y tema Material 3
│   ├── AppNavigation.kt
│   └── AppViewModel.kt
└── MainActivity.kt
```

## Recursos nativos

| Recurso | Uso | Permiso |
|---|---|---|
| Cámara (`TakePicture`) | Foto del problema y evidencia | No requiere (usa la app de cámara del sistema) |
| Galería (Photo Picker) | Adjuntar imágenes | No requiere |
| Selector de archivos | Cargar PDF / Word | No requiere |
| `PdfDocument` / `PdfRenderer` | Generar y ver informes PDF | No requiere |
| MediaStore Descargas | Guardar PDF en el teléfono | No requiere (Android 10+) |
| Teléfono (`ACTION_DIAL`) y correo (`mailto:`) | Contactar a un administrador | No requiere |

Los archivos se guardan en el almacenamiento privado de la app y se comparten solo mediante `FileProvider`.

## Cómo ejecutar

1. Clonar el repositorio y abrir la carpeta en **Android Studio**.
2. Esperar la sincronización de Gradle.
3. Ejecutar en un emulador o teléfono con **Android 13 (API 33) o superior**.
4. Ingresar con una cuenta de demostración (contraseña `1234`):

| Correo | Perfil |
|---|---|
| ana@demo.cl | Funcionario |
| luis@demo.cl | Funcionario |
| tomas@demo.cl | Administrador (Encargado TI) |
| carla@demo.cl | Administradora |
| jorge@demo.cl | Responsable |
| marta@demo.cl | Responsable |

Para volver a los datos iniciales: **Cuenta → Restablecer datos de demostración**.

## Pruebas

Pruebas unitarias de validaciones en `app/src/test/.../ValidatorsTest.kt`
(Android Studio: clic en el ícono ▶ junto a la clase, o `./gradlew test`).
