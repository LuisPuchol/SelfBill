# Fase 1: que funcione lo básico

Fecha: 2026-11-03, acordado con el usuario en el chat ("todo como propones"). Alcance: la Fase 1 de
[PENDIENTES.md](../../../PENDIENTES.md), Sencillo y Medio. Rama `feature/fase-1`. El usuario acepta spec y
plan sin revisarlos; tras cada grupo de tareas pasa una revisión de arquitectura y calidad un agente de un
modelo superior.

---

## 1. Objetivo

Que el programa no pueda dar un dato mal ni parecer que ha perdido datos, y que cada fallo conocido de la
Fase 1 quede resuelto. No añade pantallas nuevas salvo la vista de borrados del catálogo.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Ruta de la base | `${user.home}/.selfbill/selfbill.db`; la API crea la carpeta al arrancar | Usuario (PENDIENTES) |
| Base de desarrollo del usuario | Al final se mueve `selfbill-api/selfbill.db` (con `-wal`/`-shm`) a `~/.selfbill/`, con la API parada; se borra el `selfbill.db` suelto de la raíz | Claude, aprobado |
| Índice único | 409 con código nuevo `DATA_CONFLICT`; solo violaciones de unicidad, el resto sigue en 500 | Claude, aprobado |
| `by-name`, `by-nif` | Devuelven una lista (vacía si no hay ninguno) | Claude, aprobado |
| Totales del Libro | Endpoint `GET /api/invoices/totals` con los filtros de la lista | Claude, aprobado |
| Restaurar | `POST /api/{clients,articles}/{id}/restore`; en la lista, F6 "Ver borrados" y ahí F8 "Restaurar" | Claude, aprobado |
| Código ocupado al restaurar | 409 con el `*_DUPLICATE_CODE` de siempre | Claude, aprobado |
| Cliente de un albarán en factura en borrador | Campo bloqueado y aviso; no se deja escribir | Claude, aprobado |
| Imprimir sin diálogo | `abrir-adelina.cmd` (fuera del repo) abre Edge como app con `--kiosk-printing` y perfil propio | Claude, aprobado |
| Día del cambio, contador propio | Fuera: el primero es una operación del usuario, el segundo espera a saber si se usa | Claude, aprobado |

---

## 3. Diseño

### 3.1. La base de datos

- `spring.datasource.url` por defecto: `jdbc:sqlite:${user.home}/.selfbill/selfbill.db?…` (mismas opciones).
- SQLite no crea carpetas: antes de abrir la conexión, la carpeta del fichero de la URL se crea si falta.
  Vale para cualquier URL `jdbc:sqlite:<ruta>` (también las de los lanzadores y los tests); una URL en memoria
  o sin ruta no se toca.
- Los lanzadores que pasan su propia URL siguen igual.

### 3.2. Errores de la API

- Un `sort` por una propiedad que no existe → 400 `VALIDATION_ERROR` con `errors.sort`.
- `DataIntegrityViolationException` por un índice único → 409 `DATA_CONFLICT`. Texto en el front: *"No se ha
  guardado: otra operación ha usado el mismo código o número a la vez. Vuelve a intentarlo."* Otras
  violaciones de integridad siguen como error inesperado (500).
- `GET /api/clients/by-name/{name}` y `by-nif/{nif}` → `List<ClientResponse>`, ordenada por código; sin
  coincidencias, lista vacía (sin 404).

### 3.3. Totales del Libro

- `GET /api/invoices/totals` con los mismos filtros que `GET /api/invoices` →
  `{ count, subtotal, vatAmount, surchargeAmount, total }`, sumados en `BigDecimal` sobre todas las facturas
  que cumplen el filtro.
- El Libro pide la lista y los totales del mismo periodo a la vez; la línea de totales usa los de la API. El
  aviso "Solo se muestran las 2000 primeras: acorta las fechas" se mantiene.

### 3.4. Restaurar clientes y artículos

- API: `POST /api/clients/{id}/restore` y `POST /api/articles/{id}/restore` → la ficha restaurada. Restaurar
  uno vivo no cambia nada y lo devuelve; un id que no existe → 404. Si un vivo tiene ya ese código → 409
  `CLIENT_DUPLICATE_CODE` / `ARTICLE_DUPLICATE_CODE`.
- Las listas admiten `deleted=true` (solo los borrados; por defecto, solo los vivos). Las respuestas de
  cliente y artículo llevan `deletedAt` (null si está vivo).
- Front, Clientes y Artículos: F6 "Ver borrados" cambia la lista a los borrados (misma búsqueda, en la URL
  `?deleted=1`), con una columna "Borrado" (fecha). En esa vista: título "Clientes borrados" / "Artículos
  borrados", sin F2 ni Enter de abrir; F8 es "Restaurar", con pregunta de confirmación, y al restaurar sale un
  aviso y la lista se recarga; F6 "Ver vivos" y Esc vuelven a la lista normal.

### 3.5. Resumen por cliente

- `ClientSummaryResponse` lleva `clientDeleted`. Dentro de un mismo código, el vivo va primero. La fila del
  borrado muestra el nombre seguido de " (borrado)".

### 3.6. Albaranes

- Escribir en el campo Cliente vacía los precios que siguen tal como los propuso la rejilla; al encontrar el
  cliente se proponen los suyos. Un precio tecleado no se toca.
- Si el albarán está en una factura en borrador, el campo Cliente es de solo lectura y el aviso del borrador
  dice además: "El cliente no se puede cambiar."
- `DeliveryNoteResponse` lleva `vatType` y `surchargeType`: el régimen con el que se congelaron sus tipos. La
  vista previa de los tipos compara con ese régimen, no con el del cliente actual.
- Si se pierde la conexión con el diálogo de error abierto, al volver el cursor va a Cliente (lo que espera a
  que se cierre el diálogo espera también a que vuelva la conexión).

### 3.7. Facturar, Libro e imprimir

- Un error inesperado al emitir en lote se escribe con `console.error` (el diálogo no cambia).
- Al volver la conexión, el cursor vuelve a la fecha y Enter recarga, sin pulsar Esc antes.
- Si se pierde la conexión al recargar tras emitir en lote, el resumen no se abre sobre la pantalla de sin
  conexión.
- Una conexión perdida al pedir el PDF (factura o albarán) lleva a la pantalla de sin conexión; cualquier otro
  fallo sigue con "No se ha podido imprimir…".

### 3.8. Catálogo y formularios

- Un `VALIDATION_ERROR` sin ningún campo en pantalla que lo recoja abre el diálogo de error.
- Buscar un código mayor que 2.147.483.647 da la lista vacía, como un código que no existe, sin error.
- `CodeField`: un código inicial que no existe no enseña el error hasta que se intenta salir del campo, y Enter
  no pasa (al no encontrarlo, `value = null`).
- `RecordForm`: mientras se guarda no se puede borrar y viceversa; el bloqueo de borrar dura hasta volver a la
  lista.
- Catálogo: con texto, un Enter pulsado antes de que llegue la búsqueda no abre nada.
- Artículos: el aviso de borrar cuenta los albaranes sin factura y los que están en facturas en borrador. La
  lista de albaranes admite `invoiceStatus` (`DRAFT`, `ISSUED`).

### 3.9. Ajustes

- Al guardar con otro email: si el servidor y el puerto del formulario son los predeterminados del proveedor
  del email anterior, se ponen los del nuevo, o se vacían si el nuevo no es un proveedor conocido.
- `UserProfileResponse` lleva `mailPasswordUnreadable`: hay contraseña guardada pero esta `secret.key` no la
  descifra. Ajustes dice entonces *"No se puede leer la contraseña guardada (se guardó en otro equipo).
  Escríbela de nuevo."* en vez de "Guardada".

### 3.10. Trazabilidad

- Los albaranes de un lote se ordenan por día y, dentro del día, por número (la hora no cuenta).

---

## 4. Pruebas

- API: tests de integración por endpoint cambiado; `OpenApiDocsTest` para los nuevos.
- Front: unitarios y de componente en cada arreglo; e2e para restaurar y para los totales del Libro.
- Al final: todas las suites, el e2e y el arranque de la API desde `target/classes` en un puerto y una base
  temporales.

---

## 5. Fuera

- El día del cambio y el contador propio (ver la tabla).
- La comprobación de la contraseña al restaurar una copia va con las copias de seguridad (Fase 3); aquí solo se
  avisa al leer.
