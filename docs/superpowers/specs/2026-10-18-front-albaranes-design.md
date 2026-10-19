# Front: albaranes

Fecha: 2026-10-18, acordado con el usuario en el chat. Base:
[spec de la base](2026-10-13-front-base-design.md), [catálogo](2026-10-17-front-catalogo-design.md),
[botones](2026-10-18-front-botones-design.md), [ajustes](2026-10-18-front-ajustes-design.md),
[CONCEPTO.md](../../../CONCEPTO.md) §2, §3, §4, §6, §7 y §10, y [PENDIENTES.md](../../../PENDIENTES.md) §5.
Plan de ejecución: [2026-10-18-front-albaranes.md](../plans/2026-10-18-front-albaranes.md).

---

## 1. Objetivo

El trabajo diario: un albarán por cada entrega, tecleado con el pad numérico. Lista para buscar
y revisar, y un editor con cabecera (cliente y fecha) y líneas (artículo, lote, cantidad, precio).
Éxito: un albarán de 10 líneas sin tocar el ratón, más rápido que en el programa anterior.

Después vendrá Facturas, que agrupa estos albaranes.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Orden de cada línea | Código de artículo → Lote → Cantidad → Precio | Usuario |
| Lotes 2.º y 3.º | Casi nunca se usan: un solo lote en la línea y los otros en un detalle (F4) | Usuario |
| Precio propuesto | El último que se le cobró a ese cliente por ese artículo; se escribe encima o se acepta con Enter | Usuario |
| Lista | Los últimos primero, buscador como el catálogo (cifras = número, letras = cliente) y F3 "Solo pendientes" | Usuario |
| Tras guardar un alta | Otro albarán vacío con la misma fecha, cursor en Cliente | Usuario |
| Editor | Rejilla editable sobre `RecordForm` / `RecordScreen` | Usuario (enfoque recomendado) |
| Número del albarán | Lo asigna la API al guardar; no se teclea | Claude |
| Formato (simple / valorado) | El del cliente, como ya hace la API; en pantalla solo se informa | Claude |
| Estado de factura en el albarán | La API lo devuelve en cada albarán (`invoiceStatus`, `invoiceNumber`) | Claude |
| Imprimir y enviar | Fuera: con el bloque del PDF | Claude |

---

## 3. Estado actual

- `/delivery-notes` es un marcador de posición.
- API: `GET /api/delivery-notes` filtra por `codeFrom`/`codeTo`, `clientId`, `dateFrom`/`dateTo`,
  `invoiced`, `articleId` y `traceabilityCode`; `POST` asigna el número si no se manda; `PUT` y
  `DELETE` por id. La respuesta lleva `invoiceId`, `format`, `vatPercentage`, `surchargePercentage`,
  `total` (sin impuestos), `clientId`, `clientName` y las líneas (`articleId`, `articleCode`,
  `articleName`, `traceabilityCode1/2/3`, `quantity`, `price`, `total`). Cantidad y precio, 2 decimales.
- No dice si la factura está en borrador o emitida, ni su número; tampoco busca por nombre de cliente.
- `FormNav.validateAll()` devuelve `false` si un campo está comprobando (un código buscándose):
  el "+" justo después de Enter en un código se ignora (PENDIENTES §5).
- `RecordForm` lleva el cursor a `code` tras un alta en cadena; `RecordScreen` no tiene modo de
  solo lectura; `ListScreen` no admite teclas extra ni condición para borrar.

---

## 4. Diseño

### 4.1. API

- `DeliveryNoteResponse` gana `invoiceStatus` (`DRAFT` | `ISSUED` | null) e `invoiceNumber`
  (el número formateado de la factura, "23" o "S-3", solo si está emitida; null si no).
- `GET /api/delivery-notes` gana `q`: nombre comercial, razón social o NIF del cliente, sin
  distinguir tildes ni mayúsculas (la columna `search_text` del cliente, como `GET /api/clients?q=`).
- `schema.d.ts` se regenera con una API temporal en otro puerto, base y clave temporales.

### 4.2. Lista (`/delivery-notes`)

- Columnas: `Nº · Fecha · Cliente · Importe · Estado`, de la fecha más reciente a la más antigua
  (y por número dentro del día). Importe = `total` (sin impuestos).
- Estado: "Pendiente" (sin factura), "En factura borrador" (`DRAFT`), "Facturado nº 23" (`ISSUED`).
- Buscador: cifras → ese número (`codeFrom` = `codeTo`); letras → `q`.
- `F3 · Solo pendientes` alterna (`invoiced=false`); con el filtro puesto el botón dice
  `Ver todos · F3`. El filtro va en la URL (`?pending=1`).
- `Nuevo · F2`, `Abrir · Enter`, `Borrar · F8` y páginas, como el catálogo. Borrar solo vale con
  un albarán pendiente (sin factura); la pregunta: "¿Borrar el albarán 1045?" con el cliente.
- Lista vacía: "Todavía no hay albaranes. Pulsa F2 para hacer el primero."

### 4.3. Editor (`/delivery-notes/new`, `/delivery-notes/[id]`)

**Cabecera**: Cliente (por código; al lado su nombre comercial; "No existe ningún cliente con ese
código") y Fecha (hoy en un alta). Al lado, en solo lectura, el formato: "Albarán simple" o
"Albarán valorado" (el del cliente en un alta; el congelado en uno guardado).

**Líneas** (rejilla): `Código · Artículo · Lote · Cantidad · Precio · Importe`.

| Campo | Regla |
|---|---|
| Código | Artículo por código; al lado (columna Artículo) su nombre. "No existe ningún artículo con ese código" |
| Lote | 8 cifras, obligatorio. Si la línea tiene más lotes, al lado "+1" o "+2" |
| Cantidad | Hasta 2 decimales, mayor que cero ("Tiene que ser mayor que cero") |
| Precio | Hasta 2 decimales. Al encontrar el artículo, si el precio está vacío, se propone el último precio de ese artículo a ese cliente (la línea con ese artículo del albarán más reciente del cliente); vacío si nunca se le sirvió |
| Importe | Cantidad × precio redondeado a céntimos (mitad hacia arriba), solo lectura |

- Una línea con los cuatro campos vacíos no cuenta: no se valida ni se envía. Una con algo escrito
  necesita los cuatro.
- Teclas en la rejilla:
  - Enter: siguiente campo; tras el Precio de la última línea, se añade una línea y el cursor va a
    su Código.
  - ↑ / ↓: la misma columna en la línea anterior / siguiente.
  - `F4 · Lotes`: detalle de la línea con Lote 1\*, Lote 2 y Lote 3 (8 cifras cada uno). Enter
    recorre; `+` o "Aceptar" guarda en la línea; Esc cancela.
  - `F7 · Quitar línea`: quita la línea donde está (o estuvo) el cursor; el cursor pasa a la misma
    columna de la siguiente (o de la anterior). Quitar la única línea la deja vacía.
- Botones: en el título `Guardar · +`, `Borrar · F8`, `Volver · Esc`; encima de la rejilla
  `Lotes · F4` y `Quitar línea · F7`, que actúan sobre la línea del cursor.

**Totales** (debajo, siempre a la vista): Base, IVA (x %), Recargo (y %) y Total.

- Base = suma de importes de línea. IVA y recargo = base × tipo, redondeados a céntimos.
- Tipos: los del periodo de IVA vigente en la fecha del albarán y el régimen del cliente (sin IVA →
  0; sin recargo → 0). En un albarán guardado, como hace la API (CONCEPTO §3), cada componente
  conserva su tipo congelado mientras el régimen del cliente para ese componente no cambie
  (cambiar solo la fecha no lo toca); si cambia (otro cliente con otro régimen), ese componente
  toma el tipo del periodo vigente en la fecha, o 0. En uno facturado, siempre los congelados.
  Son una vista previa: la API fija los definitivos al guardar.
- Sin IVA vigente en esa fecha: en lugar de los tipos, "No hay IVA configurado para esa fecha".

**Guardar (+)**: `validateAll` (cliente, fecha, al menos una línea con contenido, cada línea
completa), esperando a que acaben las búsquedas de código en curso. Se envía:

- `date`: la fecha con la hora actual en un alta; en una edición, la hora que tenía, en el día del
  formulario.
- `clientId` y, por línea, `articleId`, `traceabilityCode1`, `traceabilityCode2/3` (`null` si
  vacíos), `quantity`, `price`. En una edición, también `code`.
- Alta: toast "Albarán 1045 guardado" y otro vacío con la misma fecha, cursor en Cliente.
  Edición: toast y vuelta a la lista con ese albarán marcado.
- Errores de campo de la API bajo su celda (`lines[2].quantity`…); los demás, al diálogo de error.

**Estados**:

- Pendiente: se edita y se borra.
- En factura borrador: aviso en color de borrador "Está en una factura en borrador: si lo cambias,
  la factura se recalcula." Se edita; no se borra.
- En factura emitida: aviso "Está en la factura nº 23, emitida: no se puede modificar." Todo en
  solo lectura; sin Guardar, Borrar, F4 ni F7; Esc vuelve.

**Borrar (F8)**: solo pendiente. "¿Borrar el albarán 1045?" con el nombre del cliente; sin aviso de
pendientes (nada depende de él). Tras borrar, vuelta a la lista.

### 4.4. Piezas

- **API**: §4.1.
- **`FormNav.validateAll`** espera a la comprobación en curso en vez de devolver `false`.
- **`RecordForm`**: opción `firstField` (por defecto `code`) para el cursor tras un alta en cadena.
- **`RecordScreen`**: prop `readOnly` (sin Guardar ni Borrar).
- **`ListScreen`**: acciones extra (tecla + botón, como las demás) y `deletion.canDelete(row)`.
- **Campos compactos**: `Field` y los campos ganan `compact` (etiqueta solo para lectores de
  pantalla, error pequeño debajo), para las celdas de la rejilla.
- **`deliverynotes/`**: valores y payload, totales, último precio, búsquedas por código con caché
  (cliente y artículo: id, nombre y, del cliente, régimen y formato), `LinesGrid`, `LotsDialog`,
  `Totals`, `DeliveryNoteRecord`.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | `invoiceStatus` / `invoiceNumber` en un albarán sin factura, en borrador y emitido; `q` por nombre comercial con y sin tilde, por razón social y por NIF; OpenAPI con `q` |
| Unidad | Payload (lotes vacíos a `null`, códigos a ids, línea vacía fuera, hora del alta y de la edición); totales y redondeo; tipos según régimen y fecha; elección del último precio; buscador de la lista |
| Componente | `FormNav.validateAll` con un código buscándose; `RecordScreen readOnly`; `ListScreen` con acción extra y `canDelete`; rejilla: recorrido de Enter, línea nueva tras el precio, ↑/↓, F7, F4 con sus lotes, "+1"; precio propuesto; línea vacía ignorada; avisos de borrador y emitida; alta en cadena conserva la fecha y deja el cursor en Cliente |
| e2e | Un albarán con el teclado (cliente y dos líneas, un precio propuesto), queda "Pendiente" en la lista; buscar por cliente sin tilde y F3; editar y volver; uno en una factura emitida se abre en solo lectura |
| Arranque | Antes de la PR: la API arranca desde `target/classes` con el classpath del IDE y responde |

---

## 6. Fuera de alcance

- Imprimir y enviar por correo (bloque del PDF).
- Teclear el número del albarán, duplicar albaranes, tarifas por cliente.
- Facturar desde la lista (bloque de Facturas).

---

## 7. Riesgos

- **La rejilla y el recorrido de Enter** con filas que se añaden y se quitan: `FormNav` ordena por
  posición en la página, así que vale, pero los tests de la rejilla cubren altas, bajas y ↑/↓.
- **Totales de vista previa** distintos de la API: se usan el mismo redondeo por línea y los tipos
  congelados cuando no cambian fecha ni cliente; la API manda.
- **Cambiar `validateAll`** toca todos los formularios: sus tests existentes deben seguir igual.
