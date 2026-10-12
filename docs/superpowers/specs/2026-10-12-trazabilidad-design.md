# Trazabilidad: búsqueda de albaranes por artículo y lote

Fecha: 2026-10-12, revisado el mismo día con las respuestas del usuario. Base:
[CONCEPTO.md](../../../CONCEPTO.md) §10 y [PENDIENTES.md](../../../PENDIENTES.md) ("Trazabilidad")
en su versión de esta fecha. Plan de ejecución:
[2026-10-12-trazabilidad.md](../plans/2026-10-12-trazabilidad.md).

---

## 1. Objetivo

Que el front pueda responder a una inspección sanitaria. La petición típica es "todos los
tomates entre tal y tal fecha": el usuario no conoce el lote, indica fechas y artículo, y
obtiene los albaranes que lo contienen. Después entra en cada albarán y ve la línea con su
código. Opcionalmente filtra también por cliente o por un lote concreto.

Además, el nombre `trazability` pasa a `traceability` en toda la API.

---

## 2. Estado actual

Los códigos se guardan y se copian, pero nada los consulta:

| Dónde | Qué hay |
|---|---|
| `DeliveryNoteLine` (`delivery_notes_articles`) | `trazabilityCode1/2/3`, enteros. El 1 es obligatorio |
| `V1__init.sql` | Un índice por columna (`idx_trazability_code1/2/3`), puestos para esta consulta y sin usar |
| `InvoiceService.buildLines` | Copia los tres a `InvoiceLine` (`invoice_lines`) |
| `DeliveryNoteLineResponse`, `InvoiceLineResponse` | Los devuelven dentro de cada línea |
| `DeliveryNoteFilter` | Filtra por código, cliente, fecha y facturado, pero no por artículo ni por lote |

Editar un albarán sustituye todas sus líneas: las antiguas quedan borradas en lógico
(`orphanRemoval` + `@SQLDelete`) y se insertan otras nuevas.

---

## 3. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Qué son los tres códigos | Lotes de proveedor, equivalentes entre sí. Una línea de tomate puede salir de una caja que sobró ayer de un proveedor y de otra comprada hoy a otro | Usuario |
| Formato del código | Se teclea a mano: 2 cifras de proveedor y 6 de fecha. Solo números; siguen siendo enteros | Usuario |
| Qué devuelve la búsqueda | Albaranes, no líneas | Usuario |
| Búsqueda principal | Fechas + artículo; cliente y lote, opcionales | Usuario |
| Búsqueda por lote | Exacta | Usuario |
| Trazabilidad hacia atrás | No hace falta: el proveedor va dentro del código y basta con encontrar los registros | Usuario |
| Albarán borrado | No cuenta como entrega | Usuario |
| Nombre | `traceability` en toda la API: Java, JSON, columnas e índices | Usuario |
| Dónde | Dos filtros nuevos en `GET /api/delivery-notes`, sin endpoint propio | Diseño (§4.1) |
| Filtro de artículo | Por `articleId`, como `clientId` | Diseño (§4.1) |

---

## 4. Diseño

### 4.1. Filtros nuevos en `GET /api/delivery-notes`

| Parámetro | Efecto |
|---|---|
| `articleId` | Albaranes con alguna línea de ese artículo |
| `traceabilityCode` | Albaranes con alguna línea que lleve ese código en cualquiera de sus tres huecos |

Se combinan con AND con los filtros que ya existen (`clientId`, `dateFrom`, `dateTo`,
`codeFrom`, `codeTo`, `invoiced`). La respuesta es la de siempre: `Page<DeliveryNoteResponse>`,
cada albarán con **todas** sus líneas, no solo las que coinciden. Es lo mismo que el usuario
vería al entrar en él.

| Pregunta | Llamada |
|---|---|
| Albaranes con tomate en octubre | `?articleId=&dateFrom=2026-10-01T00:00:00&dateTo=2026-10-31T23:59:59` |
| ...de un cliente | lo anterior `&clientId=` |
| ¿Quién recibió el lote 12241012? | `?traceabilityCode=12241012` |
| Tomate de ese lote | `?articleId=&traceabilityCode=12241012` |

**Por qué no un endpoint propio.** El resultado es un listado de albaranes, y ese listado ya
existe con paginación, orden y el resto de filtros. Un segundo endpoint lo duplicaría. La
pantalla de trazabilidad del front es un formulario sobre este listado.

**Por qué `articleId` y no el código del artículo.** El código de un artículo borrado queda
libre y puede reutilizarse (CONCEPTO §7); filtrar por código mezclaría dos productos en el
histórico. El front traduce el código que teclea el usuario con
`GET /api/articles/by-code/{code}`.

Las fechas siguen exigiendo fecha y hora (PENDIENTES, "Filtros de fecha exigen fecha y
hora"): el front envía `T00:00:00` y `T23:59:59`.

### 4.2. Semántica

- **Artículo y lote, en la misma línea.** "Tomate del lote X" no encuentra un albarán con
  tomate de otro lote y pimiento del lote X.
- **Un albarán sale una vez**, aunque coincidan varias de sus líneas. La paginación y el
  total cuentan albaranes.
- **Solo líneas vivas.** Una línea sustituida al editar un albarán no cuenta: era un error
  corregido. Los albaranes borrados ya no salen en el listado.
- **Ceros a la izquierda.** Un proveedor `05` pierde el cero al guardarse como entero
  (`05241012` se guarda como `5241012`). La búsqueda no se ve afectada porque el parámetro
  se convierte igual, en decimal. Si el front quiere mostrar el código tal como se tecleó,
  lo rellena hasta 8 cifras.

Se implementa con un único `EXISTS` sobre `DeliveryNoteLine`, correlacionado con el
albarán y con las condiciones de artículo y de lote que vengan. Un `join` con `distinct`
también evitaría duplicados, pero complica el recuento de la paginación. El
`@SQLRestriction` de `DeliveryNoteLine` deja fuera las líneas sustituidas; un test lo fija.

Los tres índices de los códigos sirven a la subconsulta. `article_id` no tiene índice, pero
con el volumen de un solo negocio no hace falta.

### 4.3. Renombrado `trazability` → `traceability`

- **Java:** `DeliveryNoteLine`, `InvoiceLine`, `DeliveryNoteLineRequest` (incluido el mensaje
  de validación), `DeliveryNoteLineResponse`, `InvoiceLineResponse`,
  `InvoiceService.buildLines` y el helper de tests `DomainIntegrationTest`.
- **JSON:** `trazabilityCode1/2/3` pasa a `traceabilityCode1/2/3` en requests y respuestas.
  Rompe a cualquier cliente que los use, pero el front todavía no lo hace.
- **BD:** migración `V3__traceability_names.sql`. Renombra las seis columnas
  (`alter table ... rename column`, en `delivery_notes_articles` e `invoice_lines`) y
  sustituye `idx_trazability_code1/2/3` por `idx_traceability_code1/2/3`. SQLite actualiza
  la definición de los índices al renombrar la columna, pero no su nombre. `V1` no se toca:
  una migración aplicada no cambia. La `selfbill.db` local se migra sola al arrancar,
  conservando sus datos.
- Hibernate (`validate`) comprueba las columnas en cada arranque de test, y un test de
  esquema comprueba que no queda `trazab` en ningún objeto de la BD.

**Enmienda (revisión del usuario).** Los códigos pasan a texto de exactamente 8 dígitos para
conservar el cero inicial: `varchar(8)` en la BD, `String` en Java y en JSON, y
`@Pattern("\d{8}")` al crear o editar líneas y en el filtro, con 400 si no se cumple. Como la
BD está en pruebas y se puede recrear, el esquema definitivo va directamente en `V1` y `V3`
desaparece: una `selfbill.db` existente se borra y se regenera. Esto sustituye lo dicho en §3,
§4.2 y esta sección sobre enteros, ceros perdidos y la migración `V3`.

---

## 5. Fuera de alcance

- Un listado de líneas o un endpoint propio de trazabilidad.
- Filtro por hueco concreto, búsqueda parcial o por rango de lote.
- Validar el formato del código (8 cifras).
- Exportar el resultado a un fichero.
- Trazabilidad hacia atrás y entidad de lotes.

---

## 6. Documentación

- CONCEPTO §10: qué son los códigos, su formato, la búsqueda típica y su semántica (§4.2).
- PENDIENTES: quitar "Trazabilidad" de funcionalidad futura.

---

## 7. Pruebas

- `SchemaTest`: ningún objeto del esquema contiene `trazab`, y los tres índices existen con
  su nombre nuevo.
- `DeliveryNoteQueriesTest`:
  - Fechas + artículo encuentra los albaranes con alguna línea de ese artículo; con cliente,
    solo los suyos.
  - El lote se encuentra en cualquiera de los tres huecos. Un albarán con dos líneas del
    mismo lote sale una vez, y el total paginado lo cuenta una vez.
  - Artículo y lote tienen que coincidir en la misma línea.
  - Tras editar un albarán, su artículo y su lote anteriores dejan de encontrarlo.
- `DeliveryNoteControllerTest`: enlace de `articleId` y `traceabilityCode` (con un cero
  inicial, que se lee en decimal), y 400 con un lote no numérico.
- `OpenApiDocsTest`: `traceabilityCode` aparece documentado en `/api/delivery-notes`.
- Línea base del 2026-10-12: 93 tests en verde.
