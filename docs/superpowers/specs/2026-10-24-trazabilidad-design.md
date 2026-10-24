# Trazabilidad

Fecha: 2026-10-24, acordado con el usuario en el chat. Base:
[spec de la base del front](2026-10-13-front-base-design.md), [CONCEPTO.md](../../../CONCEPTO.md) §10,
[spec de Facturas](2026-10-23-facturas-design.md) (el Libro de facturas es el modelo de pantalla).
Plan: [2026-10-24-trazabilidad.md](../plans/2026-10-24-trazabilidad.md).
Se trabaja directamente en `main` (decisión del usuario).

---

## 1. Objetivo

La sección **5 · Trazabilidad** sustituye a la ventana del programa antiguo: entre dos fechas, qué
artículos se vendieron; de un artículo, con qué lotes y cuánto; y de un lote, en qué albaranes, a qué
clientes y cuánto. Ese último listado se imprime para una inspección o una retirada.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Huecos de lote | Los tres cuentan, no solo el Lote 1 (CONCEPTO §10: son equivalentes). Una línea con dos lotes suma su cantidad entera en los dos | Claude, aprobado |
| Albaranes del lote | Solo los de las fechas buscadas: su total coincide con la cantidad del lote | Claude, aprobado |
| Periodo por defecto | Hoy menos 30 días hasta hoy, como el Libro de facturas | Usuario |
| Impresión | Un listado montado en el front e impreso con el diálogo del navegador, como el albarán; sin PDF de la API | Claude, aprobado |
| Abrir el albarán desde el listado | No | Claude, aprobado |

---

## 3. Estado actual

- `/traceability` es un `SectionPlaceholder`.
- `GET /api/delivery-notes` filtra por `articleId` y `traceabilityCode` (cualquier hueco) y devuelve
  albaranes enteros, paginados y sin el código del cliente: no sirve para agregar cantidades.
- Las líneas de albarán guardan `traceabilityCode1` (obligatorio), `2` y `3`, de ocho cifras.

---

## 4. Diseño

### 4.1. API

Tres consultas de solo lectura. Fechas `dateFrom` y `dateTo` obligatorias e inclusivas (día entero).
Solo cuentan albaranes vivos y líneas vivas (CONCEPTO §10).

| Endpoint | Devuelve |
|---|---|
| `GET /api/traceability/articles?dateFrom&dateTo` | Los artículos con alguna línea en el periodo: `articleId`, `articleCode`, `articleName`, por código |
| `GET /api/traceability/lots?articleId&dateFrom&dateTo` | Los lotes de ese artículo en el periodo: `code`, `quantity` (suma de las líneas que llevan el lote en cualquier hueco; una línea con dos lotes cuenta entera en los dos; un lote repetido en dos huecos de la misma línea cuenta una vez), por código |
| `GET /api/traceability/deliveries?articleId&code&dateFrom&dateTo` | Los albaranes con una línea de ese artículo que lleva ese lote: `deliveryNoteId`, `deliveryNoteCode`, `date`, `clientId`, `clientCode`, `clientName` (nombre comercial), `quantity` (suma de esas líneas del albarán), por fecha y número |

Falta un parámetro o un lote con otro formato que ocho cifras: 400 (`VALIDATION_ERROR`).

### 4.2. Pantalla (`/traceability`)

```
Trazabilidad                                             [F9 Imprimir] [Esc Volver]
Desde [24/09/2026]  Hasta [24/10/2026]

 Cód. Artículo              │ Lote       Cantidad │ Fecha       Cód. Cliente               Alb.   Cantidad
  100 ACELGAS               │ 02010624     228,00 │ 20/05/2026    6  GRUPO FERRER GESTION  31594     20,00
▸ 101 ESPINACA BOLSA 250GR  │▸02200524     151,00 │ 20/05/2026    9  GRUPO FERRER GESTION  31596     20,00
                            │                     │ 11 albaranes · 151,00
```

- **Fechas.** Al entrar se carga el periodo por defecto y el cursor está en "Desde", seleccionada.
  Enter en Desde pasa a Hasta; Enter en Hasta carga y pasa el cursor a los artículos (si no hay
  ninguno, se queda en Hasta con el texto seleccionado). Cambiar una fecha vacía las tres columnas
  hasta el siguiente Enter.
- **Artículos** (Cód., Artículo). Sin ninguno: "No se ha vendido nada entre el 24/09/2026 y el
  24/10/2026."
- **Lotes** del artículo marcado (Lote, Cantidad) y **albaranes** del lote marcado (Fecha, Cód.,
  Cliente, Alb., Cantidad; debajo "11 albaranes · 151,00", "1 albarán · 20,00"). Se actualizan solos
  al moverse, sin pulsar nada; una respuesta que llega tarde para una fila ya dejada no se muestra.
- **Columna activa.** Solo una responde a ↑↓, y se ve cuál. Artículos: Enter pasa a los lotes; Esc
  vuelve a "Desde". Lotes: Esc vuelve a los artículos. Desde las fechas, Esc vuelve al inicio. Un clic
  en una fila la marca y activa su columna. Los albaranes solo se leen: si no caben, se desplazan
  dentro de su recuadro.
- Cantidades con dos decimales ("151,00"), fechas dd/mm/aaaa.

### 4.3. Imprimir (F9)

Activo con un lote marcado y sus albaranes cargados. Abre el diálogo de impresión del navegador con:

```
Trazabilidad 02200524 ESPINACA BOLSA 250GR    Total cantidad 151,00
Del 24/09/2026 al 24/10/2026

Fecha        Cliente                              Nº albarán    Cantidad
20/05/2026     6  GRUPO FERRER GESTION SL              31594       20,00
```

Si falla: "No se ha podido imprimir el listado." La impresión usa el mismo marco oculto que el
albarán; la función pasa a llamarse por lo que hace (imprime cualquier documento, PDF o HTML). Los
textos que vienen de la base (nombres) se escapan.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | Artículos del periodo por código; fuera las fechas, los albaranes y las líneas borrados. Lotes: los tres huecos, una línea con dos lotes en los dos, un lote repetido en una línea una vez, por código. Albaranes: suma por albarán, orden, código y nombre del cliente. 400 sin parámetros o con un lote mal formado. OpenAPI lista los tres |
| Unidad | El listado impreso (títulos, periodo, filas, total, nombres escapados) |
| Componente | `DataTable` inactiva no responde a las teclas. Carga al entrar; Enter, Enter recarga; las columnas siguen a la fila marcada; una respuesta tardía no se muestra; Enter y Esc entre columnas; editar una fecha vacía todo; F9 imprime y su fallo; vacío |
| e2e | Un artículo con dos lotes (uno en el hueco 2) en un mes aislado: los lotes salen con sus cantidades, los albaranes del segundo, y F9 llama a imprimir |
| Arranque | La API arranca desde `target/classes` y responde a las tres consultas |

---

## 6. Fuera de alcance

- Buscar directamente por lote sin elegir artículo.
- Abrir el albarán desde el listado.
- Exportar el listado (CSV).
