# Facturas: Facturar y Libro de facturas

Fecha: 2026-10-23, acordado con el usuario en el chat. Base:
[spec de la base del front](2026-10-13-front-base-design.md), [CONCEPTO.md](../../../CONCEPTO.md) §2, §5 y §9.
Plan: [2026-10-23-facturas.md](../plans/2026-10-23-facturas.md).

---

## 1. Objetivo

La sección **2 · Facturas** sustituye a dos ventanas del programa antiguo:

- **Facturar**: los albaranes pendientes hasta una fecha, agrupados por cliente, se convierten en
  facturas emitidas. Es el paso de fin de mes.
- **Libro de facturas** (el "libro de ingresos" del antiguo): las facturas emitidas entre dos fechas,
  con sus albaranes, para consultar, imprimir, enviar o borrar.

Ventanas simples, todo con teclado y con botones en pantalla, como el resto.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Estructura | "2 · Facturas" abre un menú como el de Ajustes: "1 · Facturar" y "2 · Libro de facturas" | Claude, aprobado |
| Qué factura "Facturar" | Toda la lista ("+ Facturar todos"), y una sola fila con "F3 · Facturar este" | Usuario (captura del antiguo), aprobado |
| Una fila de Facturar | Una factura: un cliente, un mes y unos tipos de IVA y recargo (CONCEPTO §2 y §4). Un cliente con pendientes de dos meses sale dos veces | Claude, aprobado |
| Fecha de la factura | La del día en que se factura; la pone la API y no se elige | Claude, aprobado |
| Libro | Desde hoy menos 30 días hasta hoy por defecto; de la más antigua a la más reciente; sin páginas; totales del periodo | Usuario / Claude, aprobado |
| Columnas del libro | Sin IRPF ni "% Ret." (SelfBill no tiene retención); con Recargo | Claude, aprobado |
| Borradores | Ninguna pantalla los crea ni los muestra: el libro solo enseña facturas emitidas | Claude, aprobado |
| Dejar fuera un albarán concreto | No se hace ni se anota: si algún día hace falta, se hará | Usuario |
| Confirmar una emisión | Como borrar: la misma tecla que la abrió emite, y Enter o Esc cancelan | Claude (convención de la app) |

---

## 3. Estado actual

- `/invoices` es un `SectionPlaceholder`.
- `POST /api/invoices/issue` (bloque anterior) crea y emite una factura en una sola transacción.
- `GET /api/invoices` lista facturas con filtros (`status`, `dateFrom`, `dateTo`…) y sin secciones;
  `GET /api/invoices/{id}` trae las secciones (una por albarán, con sus líneas).
- `GET/POST /api/invoices/{id}/pdf|email` y `DELETE /api/invoices/{id}` existen.
- No hay forma de pedir "los pendientes agrupados": `GET /api/delivery-notes?invoiced=false` da
  albaranes sueltos, sin el código del cliente ni lo que le falta para facturar.

---

## 4. Diseño

### 4.1. API

**`GET /api/invoices/pending?dateTo=2026-10-31`** (`dateTo` obligatoria, inclusiva). Devuelve las
facturas que se pueden crear con los albaranes pendientes (sin factura) de fecha hasta `dateTo`,
una por cada cliente, mes y par de tipos (IVA, recargo), ordenadas por código de cliente, mes y tipos:

| Campo | Contenido |
|---|---|
| `clientId`, `clientCode`, `clientName` | El cliente (nombre comercial) |
| `month` | `"2026-10"` |
| `vatPercentage`, `surchargePercentage` | Los tipos de sus albaranes |
| `subtotal`, `vatAmount`, `surchargeAmount`, `total` | Lo que saldrá en la factura, con la misma regla que al emitir |
| `clientDeleted` | El cliente está borrado: no se puede facturar |
| `missingFiscalData` | Lo que le falta al cliente para emitir: `nif`, `address`, `postalCode`, `city` (vacío si nada) |
| `deliveryNotes` | `id`, `code`, `date`, `total` de cada albarán, del más antiguo al más reciente |

La lista de datos que faltan sale de la misma regla que usa emitir (una sola implementación). No
mira el perfil del titular: el front ya lo sabe.

**Cambios en respuestas existentes:**

- `InvoiceResponse.clientCode`: el código del cliente (columna Cód. del libro).
- `InvoiceSectionResponse.subtotal`: la suma de las líneas de esa sección, el importe del albarán.

Facturar no tiene endpoint propio: usa `POST /api/invoices/issue` una vez por fila. Cada factura es
atómica, y un fallo no para las demás.

### 4.2. Menú de Facturas (`/invoices`)

Dos recuadros, como Ajustes: **1 · Facturar** y **2 · Libro de facturas**. Teclas 1–2; Esc vuelve al
inicio.

### 4.3. Facturar (`/invoices/billing`)

```
Facturar                                [+ Facturar todos] [F3 Facturar este] [Esc Volver]
Facturar hasta [23/10/2026]

 Cód. Cliente                  Mes      Alb.    Importe  Aviso         │ GRUPO FERRER GESTION SL · octubre 2026
▸  5  INDUS TIPICOS MALLORCA   10/2026    1       40,80                │ Base 1.187,71 · IVA 4 % 47,51 · Total 1.235,22
   6  GRUPO FERRER GESTION     10/2026    4    1.187,71                │  Fecha       Alb.     Importe
 120  VARIOS                   10/2026    4      665,13  Faltan datos  │  02/10/2026  31790     310,20
                                                                       │  …
 27 facturas · 12.345,67
```

- **Fecha.** Al entrar, el cursor está en "Facturar hasta", con la fecha de hoy seleccionada.
  Se teclea como en los albaranes (`311026` → 31/10/2026). Enter carga la lista y pasa el cursor a
  la tabla. Cambiar la fecha vacía la lista hasta el siguiente Enter, para no facturar una lista
  que no corresponde a la fecha escrita.
- **Tabla de la izquierda**: Cód., Cliente, Mes, Alb. (cuántos), Importe (sin impuestos, como en la
  lista de albaranes) y Aviso ("Faltan datos" o "Cliente borrado"). ↑↓ mueven; Enter no hace nada
  en la tabla. Si no cabe, se desplaza dentro de su recuadro con los títulos fijos. Debajo, cuántas
  filas hay y la suma de sus importes (todas, también las que no se pueden facturar).
- **Derecha**, siempre de la fila marcada, sin pulsar nada: el cliente y el mes; "Base · IVA x % ·
  Recargo y % · Total" (el recargo, solo si lo lleva); y sus albaranes (Fecha, Alb., Importe). Si la
  fila no se puede facturar, el motivo: "Faltan datos del cliente para facturar: NIF y código postal
  (se corrigen en Clientes)" o "El cliente está borrado".
- **Esc**: desde la tabla, vuelve a la fecha; desde la fecha, al menú de Facturas.
- Sin pendientes: "No hay albaranes pendientes hasta el 23/10/2026."

**"+ Facturar todos"** (activo con la lista cargada y alguna fila facturable):

1. Diálogo "¿Emitir 27 facturas?": "Con fecha de hoy, 23/10/2026." y, si las hay, "Se dejan sin
   facturar 2 filas con aviso." ("Se deja sin facturar 1 fila con aviso."). Teclas: "+ Emitir", "Enter No emitir" (y Esc).
2. Emite una por una, en el orden de la lista, con un diálogo "Emitiendo facturas… 12 de 27" sin
   teclas.
3. Al terminar recarga la lista (las facturadas desaparecen) y:
   - todo bien: aviso "27 facturas emitidas: 101 a 127" (con las dos series: "101 a 120, S-5 a S-9";
     una sola: "Factura 101 emitida");
   - algo sin facturar: diálogo "Facturas emitidas" con el resumen y la lista "No se han facturado:"
     con cada cliente y su motivo (datos que faltan, cliente borrado o el error de la API en
     español). Enter cierra.
   - Si se pierde la conexión, se para ahí: lo emitido queda emitido, el resto sigue pendiente, y
     se muestra la pantalla de sin conexión.

**"F3 Facturar este"** (activo si la fila marcada es facturable): diálogo "¿Emitir la factura de
GRUPO FERRER GESTION SL?" con "octubre 2026 · 4 albaranes · 1.235,22 €"; "F3 Emitir", "Enter No
emitir". Bien: aviso "Factura 23 emitida", recarga y el cursor queda en la misma posición de la
tabla. Mal: diálogo de error con el texto de la API.

**Sin datos del titular**: los dos botones grises y, en la cabecera, "Faltan los datos del titular
(Ajustes)".

### 4.4. Libro de facturas (`/invoices/ledger`)

```
Libro de facturas                 [F9 Imprimir] [F4 Enviar por correo] [F8 Borrar] [Esc Volver]
Desde [23/09/2026]  Hasta [23/10/2026]

 Fecha       Nº    Cód. Cliente             Base     IVA   Rec.    Total │ Factura 3847 · 31/10/2026
▸31/10/2026  3847   37  JULIO TUNDIDOR    732,23   29,29   0,00   761,52 │ JULIO TUNDIDOR MOLINA SL
 …                                                                       │  Fecha       Alb.     Importe
                                                                         │  05/10/2026  31804      40,80
 14 facturas · Base 9.999,99 · IVA 399,99 · Recargo 0,00 · Total 10.399,98
```

- **Fechas.** Al entrar ya se ve el periodo por defecto (hoy menos 30 días hasta hoy) y el cursor
  está en "Desde", seleccionada. Enter en Desde pasa a Hasta; Enter en Hasta carga y pasa a la tabla.
  Esc desde la tabla vuelve a "Desde"; desde las fechas, al menú de Facturas.
- **Tabla**: solo facturas emitidas, de la más antigua a la más reciente, todas las del periodo
  (sin páginas; si pasan de 2000, aviso "Solo se muestran las 2000 primeras: acorta las fechas").
  Columnas: Fecha, Nº (con el prefijo "S-" en la serie sin IVA), Cód., Cliente, Base, IVA, Rec.,
  Total (importes). Debajo, el número de facturas y los totales del periodo. ↑↓ mueven; Enter no
  hace nada en la tabla.
- **Derecha**, de la factura marcada: número, fecha y cliente, y sus albaranes (Fecha, Alb., Importe).
- **F9 Imprimir**: el PDF de la factura en el diálogo de impresión del navegador, como el albarán.
  Si no llega: "No se ha podido imprimir la factura".
- **F4 Enviar por correo**: al email del cliente, con la cuenta de Ajustes. Bien: aviso "Factura 3847
  enviada por correo". Mal: diálogo con el texto de la API (cliente sin email, correo sin
  configurar, fallo al enviar).
- **F8 Borrar**: el diálogo de borrar de siempre: "¿Borrar la factura 3847?", el cliente y "Sus
  albaranes vuelven a quedar pendientes de facturar. Su número no se vuelve a usar." Bien: aviso "Factura
  3847 borrada" y recarga.
- Sin facturas: "No hay facturas entre el 23/09/2026 y el 23/10/2026."

**Con Verifactu (CONCEPTO §9):** F8 sigue valiendo para la serie S, que se queda en local (§9.2). En
la serie con IVA, una factura registrada no se borra: F8 pasará a "Rectificar" (§9.1). Se cambia
entonces, a propósito; este bloque no lo prepara.

### 4.5. Piezas del front

- `DataTable`: `onOpen` pasa a ser opcional (sin él, Enter no hace nada) y gana un modo con su propio
  recuadro de desplazamiento y títulos fijos (`fillViewport`), para listas largas.
- Una tabla de solo lectura, sin teclas, para la parte derecha de las dos pantallas (dos `DataTable`
  en la misma pantalla se pelearían por las flechas).
- Los nombres de los datos que faltan ("NIF y código postal") salen de la misma función que ya usa
  "Emitir factura" en el albarán.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | `pending`: agrupa por cliente, mes y tipos; incluye la fecha límite y excluye lo posterior, lo facturado (también en borrador) y lo borrado; orden; los importes son los mismos que da emitir ese grupo; `missingFiscalData` y `clientDeleted`. `clientCode` en la factura; `subtotal` de la sección igual al total del albarán; OpenAPI lista el endpoint |
| Unidad | Resumen de números emitidos (una serie, dos, una sola factura); mes en texto; totales del libro en céntimos; periodo por defecto |
| Componente | `DataTable` sin `onOpen` y con desplazamiento. Facturar: Enter en la fecha carga; cambiar la fecha vacía; la derecha sigue a la fila; "+" pide confirmación, Enter cancela, "+" emite todas las facturables en orden y salta las demás; el resumen con y sin fallos; F3 emite una y el cursor se queda en su sitio; sin titular, todo gris. Libro: carga al entrar con el periodo por defecto; Enter, Enter recarga; la derecha sigue a la fila; F9 imprime; F4 envía; F8 borra con confirmación; totales |
| e2e | Facturar con "+" deja facturados los albaranes de un mes aislado y la factura sale en el libro con su importe; F3 factura una sola fila; el libro borra una factura y sus albaranes vuelven a Facturar |
| Arranque | La API arranca desde `target/classes` y responde a `pending` antes de entregar |

---

## 6. Fuera de alcance

- Imprimir o enviar de una vez todas las facturas de un periodo (con el PDF real): a PENDIENTES.
- Elegir la fecha de la factura.
- Borradores en el front.
- Rectificativas y Verifactu (CONCEPTO §9).
- El contenido real del PDF (bloque del PDF).
