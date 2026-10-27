# MPAZ Ticket – Guía del proyecto (para estudiar y defender)

MVP de la asignatura **DSY1105 Desarrollo de Aplicaciones Móviles** · Caso: Escuela Escritora Marcela Paz.
Base: tu proyecto `BottomAppBar` (Kotlin + Jetpack Compose + Material 3).

## 1. Cómo ejecutarlo
1. Abre la carpeta `MPAZTicket` en Android Studio y espera el *Gradle Sync*.
2. Ejecuta en un emulador o teléfono con Android 13+ (minSdk 33, el de tu proyecto base).
3. Ingresa con una cuenta demo (contraseña **1234**): `ana@demo.cl` (solicitante), `tomas@demo.cl` (administrador TI), `jorge@demo.cl` / `marta@demo.cl` (responsables).
4. Si Android Studio sugiere actualizar la versión de `navigationCompose` en `gradle/libs.versions.toml`, acéptala.

## 2. Qué tecnologías usé y por qué
| Pieza | Para qué sirve | Dónde |
|---|---|---|
| **Jetpack Compose + Material 3** | Interfaz declarativa (ya venía en tu proyecto) | `ui/` |
| **BottomAppBar + FAB** | Barra inferior con acciones (lista, filtros, ayuda, salir) y botón «+» | `HomeScreen.kt` |
| **Navigation Compose** | Moverse entre Login → Home → Crear / Detalle | `AppNavigation.kt` |
| **ViewModel** (`lifecycle-viewmodel-compose`) | Guarda sesión y filtros; sobrevive a rotaciones | `AppViewModel.kt` |
| **StateFlow** | El repositorio emite la lista de tickets y la UI se actualiza sola | `TicketRepository.kt` |
| **Activity Result API** (`TakePicture`, `PickVisualMedia`) | Cámara y galería sin pedir permisos peligrosos | `Components.kt` |
| **FileProvider** | Compartir con la cámara el archivo destino de forma segura | `AndroidManifest.xml`, `res/xml/file_paths.xml` |
| **Kotlin Coroutines** | Decodificar fotos en segundo plano sin congelar la pantalla | `PhotoThumb` |

No agregué Room, Retrofit ni librerías de imágenes a propósito: el caso permite una "base de datos de prueba independiente" y menos dependencias = menos cosas que se rompen.

## 3. Arquitectura (capas)
```
ui/  (pantallas Compose)  →  AppViewModel  →  TicketRepository (datos en memoria)
                                                  ↑ valida con TicketRules (permisos y estados)
data/ Models · SeedData (datos ficticios) · TicketRules · TicketRepository · PhotoStorage
```
Idea clave: **la UI nunca modifica los datos directamente**. Pide la acción al ViewModel → Repositorio → este vuelve a validar con `TicketRules` y devuelve `null` (ok) o un mensaje de error.

## 4. Cómo se cubre el caso
| Requisito del caso | Implementación |
|---|---|
| 3 perfiles (solicitante, administrador, responsable) | `Role` + `TicketRules.canView / allowedTransitions` |
| Estudiantes sin cuenta | No existen como usuarios |
| Crear ticket: categoría, ubicación, título, descripción, foto opcional | `CreateTicketScreen.kt` (con validación) |
| Número único de ticket | `Ticket.codigo` → `MP-0001` |
| Estados pendiente → … → cerrado/cancelado | `EstadoTicket` + máquina de estados en `TicketRules` |
| Bandeja del admin, prioridad, asignar/reasignar, cerrar | `TicketDetailScreen.kt` (botones según perfil y estado) |
| Responsable ve solo lo asignado, comenta, pide materiales, evidencia, marca solucionado | Filtro `canView` + tipos de comentario + `canAddEvidence` |
| Filtros por categoría, prioridad, estado, ubicación, responsable, fecha | `FiltersPanel` + `AppViewModel.visibleTickets` |
| Historial y fechas (creación, asignación, solución, cierre) | `Ticket.historial` + campos `fecha*` |
| Sin GPS / sin pagos / sin datos reales | No hay permiso de ubicación; usuarios y tickets ficticios |
| Botones claros, textos legibles, confirmaciones | Botones de ancho completo, diálogos de confirmación, chips con texto |

## 5. Decisiones de seguridad (útil para la defensa)
1. **Mínimo privilegio**: no se declara `CAMERA`, ni ubicación, ni almacenamiento. La cámara se usa vía la app del sistema y la galería con el *Photo Picker*.
2. **Fotos en almacenamiento privado** (`filesDir/photos`) y expuestas solo con `FileProvider` (`exported=false`).
3. **Anti path-traversal**: `File(name).name` en `PhotoStorage.fileFor`.
4. **Autorización en dos capas**: la UI oculta botones y el repositorio vuelve a validar cada acción.
5. **Guardia de sesión**: si no hay usuario, `AppNavigation` devuelve al login; se limpia la sesión al salir.
6. **`allowBackup="false"`**: los datos de la app no se copian a respaldos en la nube.
7. **Datos 100 % ficticios**; las contraseñas "1234" están en el código **solo porque es demo** (en producción: backend + hash).
8. **Validación y límites de entrada** (`take(n)`, mínimos de caracteres, máximo 4 fotos).
9. Aviso visible de que la app **no reemplaza los procedimientos de emergencia**.

## 6. Guion de prueba (5 minutos)
1. Ingresa como **Ana** → «+» → crea un ticket con foto → anota el código.
2. Cierra sesión → entra como **Tomás** → abre el ticket → *Revisar y priorizar* → *Asignar* a Jorge.
3. Entra como **Jorge** → abre el ticket → *Iniciar trabajo* → adjunta evidencia → comenta → *Marcar como solucionado*.
4. Entra como **Tomás** → *Confirmar solución y cerrar* → mira el historial.
5. Entra como **Ana** → verifica el estado y los comentarios. Prueba los filtros como Tomás.

## 7. Limitaciones conocidas y mejoras
- Los tickets viven **en memoria**: al cerrar la app vuelven los datos demo (las fotos sí quedan en el teléfono). Mejora: Room.
- Sin notificaciones (el caso las deja como mejora futura).
- Login simple de demostración; no hay recuperación de contraseña.
- Las fotos "antes/después" de demostración debes aportarlas tú desde la galería del emulador (fotos simuladas, sin rostros).
- Se mantiene el paquete `com.fabiancg.bottomappbar` de tu base; puedes renombrarlo con *Refactor → Rename* si quieres.
