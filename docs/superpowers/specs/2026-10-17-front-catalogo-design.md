# Front: catálogo (artículos y clientes)

Fecha: 2026-10-17, acordado sección a sección con el usuario. Base:
[spec de la base del front](2026-10-13-front-base-design.md), [CONCEPTO.md](../../../CONCEPTO.md) y
[PENDIENTES.md](../../../PENDIENTES.md) §5 en su versión de esta fecha. Plan de ejecución:
[2026-10-17-front-catalogo.md](../plans/2026-10-17-front-catalogo.md).

---

## 1. Objetivo

Las primeras pantallas de verdad: listado, alta, edición y borrado de artículos y clientes. El
patrón de lista y ficha que sale de aquí es el que usarán después albaranes y facturas.

Antes de construir encima, se endurecen los campos y el teclado de la base con lo que dejó apuntado
su revisión final (PENDIENTES §5).

El catálogo se usa sobre todo al principio, al dar de alta artículos y clientes, y después de vez en
cuando. El trabajo diario son los albaranes, que buscan clientes y artículos por código.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Lista y ficha | Pantallas separadas. Enter abre la ficha, Esc vuelve a la lista | Usuario |
| Búsqueda | Un solo buscador. Solo cifras: código exacto, y Enter lo abre. Letras: filtra por nombre | Usuario |
| Búsqueda de clientes | Nombre comercial, razón social o NIF: filtro `q` nuevo en la API | Usuario |
| Tildes y mayúsculas | La búsqueda las ignora ("garcia" encuentra "García" y "GARCÍA"), con una columna `search_text` normalizada | Usuario, al revisar el plan (§5) |
| Borrar con pendientes | Aviso con números y se deja borrar. Recuperar borrados sigue en PENDIENTES | Usuario |
| Avisos de la ficha de cliente | "Faltan datos para facturar" (no bloquea) y confirmar el cambio de IVA o recargo si hay pendientes | Usuario |
| Tras guardar | Alta → otra alta con el siguiente código. Edición → vuelta a la lista con la fila seleccionada | Usuario |
| Estructura | Piezas compartidas pequeñas (`ListScreen`, `useRecordForm`) y pantallas concretas | Usuario (§4) |
| Código repetido | Error bajo el campo Código, no en un diálogo | Diseño (§4.2) |
| Enter en el último campo | Vuelve al primero: guardar es solo "+" | Diseño (§4.2) |
| Cuándo avisa un campo | Al salir, de lo mal escrito; lo que falta, al pulsar Enter o al guardar | Diseño (§4.3) |
| Vacíos | Los opcionales vacíos viajan como `null` | Diseño (§4.3) |
| Precarga | `data-sveltekit-preload-data="tap"` | Diseño (§4.3) |

---

## 3. Estado actual

| Dónde | Qué hay |
|---|---|
| Base del front | `DataTable`, `Form`/`FormNav` (con `validateAll` y `bind:nav`), los seis campos, `Dialog`, `UnsavedGuard`, toasts, errores, `withParams`, `request`/`loaded`. `/articles` y `/clients` son secciones vacías |
| API de artículos | Código (entero, único entre vivos) y nombre. Listado paginado con filtro `name` (contiene) y rango de códigos, `next-code`, `by-code`, alta, edición y borrado lógico |
| API de clientes | 13 campos editables (§4.2). Filtros `name` y `businessName`, combinados con Y, y rango de códigos; `next-code`, `by-code`, alta, edición y borrado lógico |
| Validación de clientes | Código, nombre comercial y razón social obligatorios. Email y código postal con patrón: un `""` no pasa |
| Contar pendientes | `GET /api/delivery-notes?clientId=&invoiced=false`, `?articleId=&invoiced=false` y `GET /api/invoices?clientId=&status=DRAFT` |
| Consecuencias de borrar | Un cliente borrado deja sus albaranes pendientes y borradores sin poder editarse ni facturarse. Un artículo borrado impide volver a guardar los albaranes pendientes que lo usan. Solo se restaura por SQL |
| Cambio de régimen | Cambiar el IVA o el recargo de un cliente reaplica el régimen a sus albaranes fuera de facturas emitidas y recalcula sus borradores (CONCEPTO §3) |

---

## 4. Diseño

### 4.1. Lista (`ListScreen`)

Igual para artículos (`/articles`) y clientes (`/clients`). La búsqueda y la página van en la URL
(`?q=garcía&page=1`), y el orden es siempre por código (`sort=code`), 20 filas por página.

**Buscador.**

- Arriba, con el cursor dentro al entrar. Etiqueta: "Buscar (código o nombre)".
- Se lanza al dejar de teclear 300 ms.
- **Solo cifras:** filtra por ese código exacto (`codeFrom` = `codeTo`).
- **Con letras:** en artículos por nombre (`name`); en clientes por nombre comercial, razón social o
  NIF (`q`).

| Tecla | Efecto |
|---|---|
| Enter | Abre la fila seleccionada. Con un código exacto es la única: `7` + Enter abre el 7 |
| ↑ / ↓ | Mueven la selección de la tabla sin sacar el cursor del buscador |
| AvPág / RePág | Página siguiente o anterior |
| F2 | Nuevo |
| Esc | Con texto en el buscador, lo borra. Vacío, vuelve al inicio |

`ListScreen` atiende ↑/↓/Enter desde el buscador y mueve la selección de `DataTable`, que pasa a
exponer su fila seleccionada como enlazable (`bind:selected`).

**Columnas.**

- Artículos: Código y Nombre.
- Clientes: Código, Nombre, Razón social, Población y NIF.

**Mensajes.**

| Situación | Texto |
|---|---|
| Búsqueda por texto sin resultados | "No hay ningún cliente que coincida con «garcía»." |
| Código que no existe | "No existe ningún cliente con el código 7." |
| Catálogo vacío | "Todavía no hay clientes. Pulsa F2 para dar de alta el primero." |

En artículos, los mismos textos con "artículo".

**Vuelta desde la ficha.** `ListScreen` recuerda, por sección, la última URL de lista y el último
registro que abrió. Al volver, con Esc o tras guardar una edición, se recupera la búsqueda y la
página, con esa fila seleccionada si está en la página.

### 4.2. Ficha (`useRecordForm`)

**Rutas.** `/articles/new`, `/articles/[id]`, `/clients/new` y `/clients/[id]`. Un alta llega con el
código de `next-code`. Un id inexistente o borrado lleva a la 404 de la base.

**Teclas.**

| Tecla | Efecto |
|---|---|
| Enter | Siguiente campo. En el último, vuelve al primero |
| "+" (pad) | Guardar |
| F8 | Borrar (solo en edición) |
| Esc | Volver a la lista, pasando por el aviso de cambios sin guardar |

**Campos de artículo.** Código\* (número entero) y Nombre\*.

**Campos de cliente**, en bloques; Enter los recorre bloque a bloque y, dentro de cada bloque, de
izquierda a derecha:

| Bloque | Campos |
|---|---|
| Identificación | Código\*, Nombre comercial\*, Razón social\*, NIF |
| Dirección | Dirección, Código postal (5 cifras), Población, Provincia |
| Contacto | Teléfono, Teléfono 2, Email |
| Facturación | IVA (1 Con IVA · 2 Sin IVA), Recargo (1 Sin recargo · 2 Con recargo), Formato de albarán (1 Simple · 2 Valorado) |

- En un alta: Con IVA, Sin recargo, Simple.
- Longitudes máximas como la API: nombre comercial y razón social 255, NIF 20, email 50, dirección
  500, población y provincia 100, teléfonos 20.
- Formatos como la API: el código postal, 5 cifras; el email, con el mismo patrón que valida la API.

**Guardar.**

1. `validateAll()`. Si falla, el cursor queda en el primer error.
2. Se monta lo que se envía: textos recortados y opcionales vacíos como `null`.
3. `POST` o `PUT`.
   - Un `*_DUPLICATE_CODE` se pinta bajo Código ("Ya existe un cliente con ese código", o "un
     artículo") y el cursor va ahí.
   - Un `VALIDATION_ERROR` se pinta campo a campo (`fieldErrorMessages`).
   - Cualquier otro error va a `showError`.
4. **Alta:** toast "Cliente 7 guardado" (o "Artículo 101 guardado") y ficha nueva vacía con el
   siguiente código, con el cursor en Código.
5. **Edición:** toast y vuelta a la lista (§4.1), con esa fila seleccionada.

- Un "+" mientras se guarda se ignora.
- El error que marcó la API en un campo se borra al cambiar ese campo.

**Borrar (F8).** Primero cuenta los pendientes y después pregunta:

| Ficha | Con pendientes | Sin pendientes |
|---|---|---|
| Cliente | "Tiene 5 albaranes sin facturar y 1 factura en borrador. Si lo borras no se podrán editar ni facturar." | "¿Borrar el cliente 7?" |
| Artículo | "Está en 3 albaranes sin facturar. Si lo borras no se podrán volver a guardar." | "¿Borrar el artículo 101?" |

- El diálogo se titula "¿Borrar el cliente 7?" (o "el artículo 101") y lleva el nombre.
- Teclas: Enter "No borrar" (por defecto) y F8 "Borrar".
- Tras borrar: toast "Cliente 7 borrado" y vuelta a la lista.
- En artículos solo se cuentan los albaranes sin facturar: los que están en una factura en borrador
  no se pueden filtrar por artículo con la API actual.

**Avisos de la ficha de cliente.**

- **Faltan datos para facturar.** Recuadro discreto, en color de borrador y no en rojo, que se
  actualiza mientras se escribe: "Para poder facturarle faltan: NIF y código postal." Comprueba NIF,
  dirección, código postal y población (CONCEPTO §5). No impide guardar.
- **Cambio de IVA o recargo.** Al guardar una edición en la que cambió `vatType` o `surchargeType`,
  se cuentan los albaranes sin facturar y los borradores del cliente. Si hay alguno, se pide
  confirmación: "Cambiar el IVA o el recargo recalculará 5 albaranes sin facturar y 1 factura en
  borrador de este cliente." Teclas: Enter "Volver a la ficha" (por defecto) y "+" "Guardar
  igualmente". Sin pendientes, guarda sin preguntar.

**`useRecordForm`.** Reúne lo común a las dos fichas:

- valores y copia original, para saber si hay cambios;
- errores de la API por campo;
- bloqueo mientras guarda;
- guardar con validación;
- alta en cadena;
- borrado con su aviso;
- el `UnsavedGuard`.

La API y la navegación se le inyectan, para poder probarlo fuera de la app. Cada ficha aporta sus
campos, cómo se monta lo que se envía, cómo se cuentan sus pendientes y sus avisos.

### 4.3. Endurecimiento de campos y teclado

| Tema | Decisión |
|---|---|
| Clic con el ratón | Una acción `selectOnFocus` común a Text, Number, Digits, Date y Code. Al entrar se selecciona todo, también con clic. Un segundo clic en el campo ya enfocado coloca el cursor con normalidad |
| Decimal del pad | `NumberField` solo actúa con `NumpadDecimal` si la tecla es "." o "," y sin Ctrl, Alt ni Meta. Con Bloq Num apagado, Supr borra |
| Error externo | `error \|\| ownError` en los seis campos |
| Cuándo se avisa | Al salir de un campo: lo mal escrito (número, fecha, longitud fija, patrón). Lo que falta ("Este dato es obligatorio"): al pulsar Enter o con `validateAll`. `CodeField` solo consulta con Enter o `validateAll`. Cambia el comportamiento de `DateField`, que ya no avisa de "obligatorio" al salir |
| Patrón en texto | `TextField` acepta `validate?: (value: string) => string \| undefined` para el email |
| Línea de error | `Field` reserva su altura: un error no desplaza el contenido |
| Autocompletado | `autocomplete="off"` en todos los campos |
| Tecla mantenida | `dispatch` ignora `event.repeat` salvo ↑, ↓, AvPág y RePág |
| "+" en un campo | Test: el "+" del pad guarda con el foco en un campo; el "+" principal escribe |
| Tras "Seguir editando" | El cursor vuelve al último campo de la ficha, no al enlace de la barra superior. `FormNav` recuerda el último campo enfocado |
| "Guardar y salir" falla | Se cierra el aviso y el cursor va al primer error |
| Conexión perdida | `<main>` y la barra superior quedan `inert` mientras se muestra `ConnectionScreen` |
| Enter en la tabla | No abre si el foco está en un botón o un enlace. La condición se reevalúa al moverse el foco |
| `request()` | `NETWORK` solo para fallos de `fetch` (`TypeError`) y tiempo agotado; cualquier otro rechazo es `UNKNOWN` con su detalle |
| Errores sin capturar | Además del diálogo, `console.error`. Un `throw` síncrono en una acción de teclado también llega a `showError` |
| `loaded()` | Se quitan los mensajes que `+error` no usa; solo mapea el estado |
| Precarga | `data-sveltekit-preload-data="tap"` en `app.html` |
| Vacíos | La ficha manda los opcionales vacíos como `null` |

---

## 5. Cambios en la API

| Cambio | Motivo |
|---|---|
| Columna `search_text` en `clients` y `articles`: el texto de búsqueda sin tildes ni diéresis y en minúsculas, calculado en Java (`SearchText.normalize`) al guardar. Migración Flyway V4 en Java, que añade la columna y rellena las filas existentes. Es necesaria porque el `lower()` de SQLite solo entiende ASCII: "garcia" no encontraba "García" | §4.1 |
| `ClientFilter.q`: contiene, sin tildes ni mayúsculas, en `name`, `businessName` o `nif` (sobre `search_text`). Se combina con Y con los demás filtros, y un `q` vacío se ignora | §4.1 |
| El filtro `name` de artículos compara sobre `search_text`, también sin tildes ni mayúsculas | §4.1 |

Después se regeneran los tipos del front (`npm run api:types`). Lo demás ya existe: `sort=code`,
`next-code`, `by-code` y los filtros para contar pendientes.

---

## 6. Tests

| Nivel | Qué cubre |
|---|---|
| API | `q` encuentra por nombre comercial, razón social y NIF, sin tildes ni mayúsculas; se combina con el rango de códigos; un `q` vacío se ignora; aparece en el OpenAPI. El nombre de artículos ignora tildes y mayúsculas. La migración rellena las filas existentes y la columna se mantiene al editar |
| Unitario (node) | Montar lo que se envía (recorte y `null`); datos que faltan para facturar; código frente a texto en el buscador; clasificación de `request()`; `event.repeat` y modificadores en `dispatch` |
| Componente | Cada punto de §4.3; `ListScreen` (búsqueda tras la pausa, cifras como código, ↑/↓/Enter desde el buscador, Esc borra y luego sale, F2); `useRecordForm` con API y navegación inyectadas (valida antes de enviar, código repetido bajo el campo, alta en cadena, edición vuelve a la lista, doble "+" guarda una vez, borrar con números, confirmación del régimen solo con pendientes) |
| Punta a punta | Ver abajo |

**Punta a punta** (contra el jar, base temporal, puerto 18080):

- **Artículos:** alta en cadena de dos, búsqueda por nombre y por código, editar y volver con la fila
  seleccionada, código repetido y borrar.
- **Clientes:**
  - Alta con lo mínimo muestra "faltan datos"; al completar, desaparece.
  - Búsqueda por razón social y por NIF.
  - Con un albarán pendiente, creado por la API en la preparación junto con el IVA y el artículo que
    necesita: cambiar el IVA pide confirmación con el número, y borrar avisa con los números.
- **Pendientes de la base:**
  - Aviso de cambios sin guardar: Enter sigue editando y el cursor vuelve al campo; "+" guarda y
    sale; F8 sale sin guardar.
  - `/clients/9999` muestra la 404, y Esc vuelve al inicio.

Cada prueba crea sus datos con códigos únicos, y Playwright va en serie (`workers: 1`) porque todas
comparten la base temporal.

---

## 7. Fuera de alcance

- Recuperar clientes y artículos borrados (PENDIENTES §3).
- `CodeField` con un código inicial inexistente: se trata con los albaranes.
- `SpaFallbackResolver` sin `checkResource`, e iconos de Lucide importados uno a uno.
- Importar datos del programa antiguo, lanzador, CI y despliegue.

---

## 8. Riesgos

| Riesgo | Qué se hace |
|---|---|
| Que la búsqueda en vivo dispare demasiadas peticiones | Pausa de 300 ms y `replaceState`, para no llenar el historial |
| Que al volver a la lista no se encuentre la fila abierta | Se selecciona solo si está en la página; si no, la primera |
| Que el "+" dentro del diálogo de confirmación de régimen se confunda con el de guardar | El diálogo es una capa modal: su "+" es el único activo mientras está abierto |
| Pruebas de punta a punta que dependan del orden o de datos de otra prueba | Códigos únicos por prueba y ejecución en serie |
