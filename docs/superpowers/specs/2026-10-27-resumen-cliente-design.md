# Resumen por cliente

Fecha: 2026-10-27, acordado con el usuario en el chat. Base: [spec de Facturas](2026-10-23-facturas-design.md)
(el Libro de facturas), [spec de Trazabilidad](2026-10-24-trazabilidad-design.md) (la pantalla de columnas
en cascada es el modelo). Modelo visual: el "Listado Resumen Cliente" del programa antiguo (captura en el
chat), sin su parte de cobros. Se trabaja directamente en `main`.

---

## 1. Objetivo

Ver, entre dos fechas, cuánto se ha facturado a cada cliente; de un cliente, sus facturas; y de una
factura, sus albaranes. La factura marcada se puede imprimir.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Cobros (F.Cobro, Pendiente, "Conforme") | Fuera: queda para el bloque de seguimiento de cobro | Usuario |
| Periodo por defecto | Hoy menos 30 días hasta hoy, como el Libro y Trazabilidad | Usuario |
| Imprimir | F9 imprime la factura marcada, como en el Libro | Usuario |
| Qué facturas cuentan | Solo emitidas, por su fecha de emisión; las dos series (la S con IVA 0) | Claude, aprobado |
| Cliente | Código y nombre comercial, como el resto de listas | Claude, aprobado |
| Dónde | Menú de Facturas, "3 · Resumen por cliente" | Claude, aprobado |
| Alto de los paneles | Fechas en la línea del título, filas compactas en esta pantalla, los albaranes entran en la cascada (el texto de 4.2) | Usuario |
| Reutilizar | La carga del panel de detalle (caché y respuestas tardías) pasa a una pieza común que usan Trazabilidad, el Libro y esta pantalla; la tabla de albaranes de una factura, compartida con el Libro | Claude, aprobado |

---

## 3. Estado actual

- `GET /api/invoices` filtra por `clientId`, `status`, `dateFrom` y `dateTo` (día entero), paginado, sin
  secciones. `GET /api/invoices/{id}` devuelve la factura con sus secciones (número, fecha e importe de
  cada albarán). Los nombres de cliente de las listas son el comercial (`client.name`).
- No hay ninguna consulta que agrupe facturas por cliente.
- La carga del detalle de la fila marcada (caché por clave, solo se muestra la última petición, una
  respuesta de otro periodo se descarta) está escrita tres veces: lotes y albaranes en
  `Traceability.svelte`, secciones en `Ledger.svelte`.
- El Libro ya tiene la tabla de albaranes de una factura (Fecha, Alb., Importe) sobre sus secciones.

---

## 4. Diseño

### 4.1. API

`GET /api/invoices/summary?dateFrom&dateTo`: una fila por cliente con alguna factura emitida y viva
cuya fecha cae en el periodo (fechas obligatorias e inclusivas, día entero). Cada fila: `clientId`,
`clientCode`, `clientName` (nombre comercial actual; también de un cliente borrado), `invoiceCount`,
`subtotal`, `vatAmount`, `surchargeAmount`, `total` (sumas de las facturas). Por código de cliente. Falta
una fecha: 400 (`VALIDATION_ERROR`).

Las facturas de un cliente y los albaranes de una factura salen de las consultas que ya existen.

### 4.2. Pantalla (`/invoices/summary`)

```
Resumen por cliente   Desde [27/09/2026]  Hasta [27/10/2026]   [F9 Imprimir factura] [Esc Volver]

 Cód. Cliente          Fact.     Base    IVA  Recargo     Total │ Fecha       Nº      Base   IVA  Recargo   Total
▸  5 INDUS TIPICOS        5  1.027,22  41,09     0,00  1.068,31 │▸31/01/2026 3719  158,93  6,36     0,00  165,29
   6 GRUPO FERRER         4 12.871,86    …                      │ 5 facturas · 1.068,31
 38 clientes · 214 facturas · 160.512,40                        ├──────────────────────────────────────────────
                                                                │ Fecha       Alb.    Importe
                                                                │ 02/01/2026  31397    10,07
                                                                │ 4 albaranes · 158,93
```

- **Clientes** (Cód., Cliente, Fact., Base, IVA, Recargo, Total) y su pie: "38 clientes · 214 facturas ·
  160.512,40" (el total). Sin ninguno: "No hay facturas entre el 27/09/2026 y el 27/10/2026."
- **Facturas** del cliente marcado en el periodo (Fecha, Nº con su serie, Base, IVA, Recargo, Total), de
  la más antigua a la más reciente; pie "5 facturas · 1.068,31", "1 factura · …".
- **Albaranes** de la factura marcada: la misma tabla que el Libro (Fecha, Alb., Importe), tercera columna de la cascada; pie
  "4 albaranes · 158,93", "1 albarán · …".
- Los paneles de la derecha se actualizan solos al moverse; una respuesta que llega tarde para una fila ya
  dejada, o de un periodo anterior, no se muestra.
- **Teclas, como Trazabilidad.** Al entrar se carga el periodo y el cursor está en "Desde", seleccionada.
  Enter en Desde pasa a Hasta; Enter en Hasta carga y pasa a los clientes (si no hay, se queda en Hasta
  con el texto seleccionado). Cambiar una fecha vacía los tres paneles hasta el siguiente Enter.
  Clientes: Enter pasa a las facturas; Esc vuelve a "Desde". Facturas: Enter pasa a los albaranes
  (si los hay); Esc vuelve a los clientes. Albaranes: ↑↓ los recorren, Enter no hace nada y Esc vuelve a
  las facturas; la marca vuelve al primer albarán al cambiar de factura. Desde las fechas, Esc vuelve al
  menú de Facturas. Un clic en una fila la marca y activa su columna. Solo la
  columna activa responde a ↑↓, y se ve cuál (la barra naranja). F9 imprime la factura marcada desde
  cualquier columna.
- **Alto.** Las fechas van en la línea del título y las filas son más compactas en esta pantalla (mismo
  tamaño de letra), para que quepan al menos cuatro filas por panel de la derecha en 1280×800 y en
  1366×768, sin que la página se desplace ni las tablas de lado.
- **F9 Imprimir factura**: activo con una factura marcada; imprime su PDF con el diálogo del navegador,
  como el Libro. Si falla: "No se ha podido imprimir la factura."
- Importes con `formatMoney`, fechas dd/mm/aaaa.

### 4.3. La pieza común

Una pieza del front que carga el detalle de la fila marcada: recibe la clave de la fila marcada (o
ninguna) y la función que pide su detalle; da el detalle de esa clave cuando llega; guarda en caché cada
respuesta por clave hasta que se vacía; no muestra una respuesta de otra clave ni una que empezó antes de
vaciarla (otro periodo). Trazabilidad (lotes y albaranes), el Libro (secciones) y esta pantalla
(facturas y albaranes) la usan. Las pruebas de Trazabilidad y del Libro no cambian y siguen pasando.

La tabla de albaranes de una factura (columnas sobre las secciones) se comparte entre el Libro y esta
pantalla.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | Una fila por cliente con sus sumas, por código; fuera borradores, facturas borradas y fechas fuera del periodo (el último día entra entero); las dos series; un cliente borrado sigue saliendo; 400 sin fechas; OpenAPI la lista |
| Unidad | La pieza común: da el detalle de la clave marcada, usa la caché, descarta la respuesta de una clave dejada y la de antes de vaciar, sin clave no hay detalle |
| Componente | Trazabilidad y el Libro: sus pruebas sin cambios. Resumen: carga al entrar; Enter, Enter recarga; los paneles siguen a la fila marcada; una respuesta tardía no se muestra; Enter y Esc entre columnas (también a los albaranes, con su marca); clic en una fila de facturas o de albaranes activa su columna; F9 desde los albaranes; editar una fecha vacía todo; pies; vacío; F9 imprime la factura marcada y su fallo |
| e2e | Dos clientes con facturas en un periodo aislado: las filas y sus sumas, las facturas del segundo, sus albaranes, y F9 llama a imprimir. A 1280×800: ninguna tabla se desplaza de lado ni la página, cuatro filas por panel, y el teclado llega a los albaranes |
| Arranque | La API arranca desde `target/classes` y responde a la consulta nueva |

---

## 6. Fuera de alcance

- Cobros: fecha de cobro, pendiente, marcar como cobrada.
- Imprimir o exportar el resumen.
- Abrir el albarán desde el panel.
