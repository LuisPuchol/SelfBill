# Trazabilidad: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Renombrar `trazability` a `traceability` en toda la API y añadir a `GET /api/delivery-notes` los filtros `articleId` y `traceabilityCode`, para encontrar los albaranes que pide una inspección.

**Architecture:** El renombrado es una migración Flyway `V3` más el cambio de nombre en entidades, DTO y servicio. Los filtros siguen el patrón de `DeliveryNoteFilter` + `DeliveryNoteSpecification`, con una subconsulta `EXISTS` sobre las líneas para que artículo y lote coincidan en la misma línea y cada albarán salga una vez.

**Tech Stack:** Java 25, Spring Boot 3.5.7, Spring Data JPA, Hibernate 6.6 (metamodelo con `hibernate-jpamodelgen`), Flyway, sqlite-jdbc 3.49.1, MapStruct, Lombok, JUnit 5, AssertJ, Mockito.

**Spec:** [docs/superpowers/specs/2026-10-12-trazabilidad-design.md](../specs/2026-10-12-trazabilidad-design.md)

## Requisitos previos

- Rama de trabajo desde `main` limpio: `git switch -c feature/trazabilidad`. El primer commit de la rama lleva el spec y este plan.
- Los comandos se ejecutan desde `selfbill-api/` en Git Bash (`./mvnw`). En PowerShell, `.\mvnw.cmd`.
- Línea base: `./mvnw test` acaba con 93 tests y 0 fallos.

## Global Constraints

- Ninguna dependencia nueva. Ningún endpoint nuevo.
- `V1__init.sql` y `V2__delivery_note_tax_regime.sql` no se tocan: el renombrado va en `V3__traceability_names.sql`.
- Tras la tarea 1, `trazab` solo aparece en `V1__init.sql` y en los specs y planes antiguos de `docs/`.
- El código, los identificadores, los comentarios y los textos de Swagger van en inglés; CONCEPTO.md y PENDIENTES.md, en español.
- Se comenta solo el porqué no evidente, apuntando a `CONCEPTO.md §n`, como el código actual.
- Toda tarea termina con `./mvnw test` en verde.
- Commits: mensaje corto en inglés y en minúsculas, como los del `git log`, terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Un albarán con dos líneas del mismo lote → sale una vez, y `totalElements` lo cuenta una vez (tarea 2, `filtersByTraceabilityCodeInAnySlot`).
2. Artículo de una línea y lote de otra en el mismo albarán → no coincide (tarea 2, `articleAndCodeMatchTheSameLine`).
3. Albarán editado → su artículo y su lote anteriores dejan de encontrarlo (tarea 2, `ignoresReplacedLines`).
4. Lote con cero inicial (`05241012`) → se lee como `5241012` en decimal, ni en octal ni con 400 (tarea 2, controlador).
5. Lote no numérico (`L-24`) → 400, nunca 500 (tarea 2, controlador).

## Mapa de ficheros

Raíz de código: `selfbill-api/src/main/java/com/luispuchol/selfbill/selfbill_api/` (en adelante `main/`).
Raíz de tests: `selfbill-api/src/test/java/com/luispuchol/selfbill/selfbill_api/` (en adelante `test/`).

| Fichero | Responsabilidad | Tarea |
|---|---|---|
| `src/main/resources/db/migration/V3__traceability_names.sql` | Columnas e índices con el nombre nuevo | 1 |
| `main/billing/deliverynote/DeliveryNoteLine.java`, `DeliveryNoteLineRequest.java`, `DeliveryNoteLineResponse.java` | Campos renombrados | 1 |
| `main/billing/invoice/InvoiceLine.java`, `InvoiceLineResponse.java`, `InvoiceService.java` | Campos renombrados | 1 |
| `test/support/DomainIntegrationTest.java` | Helper renombrado | 1 |
| `test/SchemaTest.java` | No queda `trazab` en el esquema | 1 |
| `main/billing/deliverynote/DeliveryNoteFilter.java`, `DeliveryNoteSpecification.java` | Filtros `articleId` y `traceabilityCode` | 2 |
| `test/billing/deliverynote/DeliveryNoteQueriesTest.java`, `DeliveryNoteControllerTest.java`, `test/OpenApiDocsTest.java` | Tests de los filtros | 2 |
| `CONCEPTO.md` §10, `PENDIENTES.md` | Documentación | 1, 2 |

---

### Task 1: `trazability` → `traceability`

**Files:**
- Create: `src/main/resources/db/migration/V3__traceability_names.sql`
- Modify: `main/billing/deliverynote/DeliveryNoteLine.java:43-45`, `DeliveryNoteLineRequest.java:17-25`, `DeliveryNoteLineResponse.java:16-18`
- Modify: `main/billing/invoice/InvoiceLine.java:60-67`, `InvoiceLineResponse.java:18-20`, `InvoiceService.java:217-219`
- Modify: `test/support/DomainIntegrationTest.java:126`
- Test: `test/SchemaTest.java`
- Modify: `CONCEPTO.md:388`

**Interfaces:**
- Produces: campos `traceabilityCode1/2/3` en `DeliveryNoteLine`, `InvoiceLine`, `DeliveryNoteLineRequest` (y su builder), `DeliveryNoteLineResponse` e `InvoiceLineResponse`; metamodelo `DeliveryNoteLine_.traceabilityCode1/2/3`; columnas `traceability_code1/2/3`.

- [ ] **Step 1: Escribir el test de esquema en `SchemaTest`**

```java
    @Test
    @DisplayName("Traceability columns and indexes use the English name")
    void traceabilityUsesTheEnglishName() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sqlite_master WHERE sql LIKE '%trazab%'",
                Integer.class)).isZero();
        assertThat(jdbc.queryForList("SELECT name FROM sqlite_master WHERE type = 'index' "
                + "AND tbl_name = 'delivery_notes_articles' AND sql LIKE '%traceability_code%'", String.class))
                .containsExactlyInAnyOrder("idx_traceability_code1", "idx_traceability_code2",
                        "idx_traceability_code3");
    }
```

- [ ] **Step 2: Comprobar que falla**

  Run: `./mvnw test -Dtest=SchemaTest`
  Expected: `traceabilityUsesTheEnglishName` falla con `expected: 0` (dos tablas y tres índices contienen `trazab`).

- [ ] **Step 3: Crear `V3__traceability_names.sql`**

```sql
-- English name for the traceability codes. V1 keeps the old one: an applied migration does not change.
-- Renaming a column rewrites the indexes on it, but not their names.
alter table delivery_notes_articles rename column trazability_code1 to traceability_code1;
alter table delivery_notes_articles rename column trazability_code2 to traceability_code2;
alter table delivery_notes_articles rename column trazability_code3 to traceability_code3;
alter table invoice_lines rename column trazability_code1 to traceability_code1;
alter table invoice_lines rename column trazability_code2 to traceability_code2;
alter table invoice_lines rename column trazability_code3 to traceability_code3;

drop index idx_trazability_code1;
drop index idx_trazability_code2;
drop index idx_trazability_code3;
create index idx_traceability_code1 on delivery_notes_articles (traceability_code1);
create index idx_traceability_code2 on delivery_notes_articles (traceability_code2);
create index idx_traceability_code3 on delivery_notes_articles (traceability_code3);
```

- [ ] **Step 4: Renombrar en Java**

  En `src/main/java` y `src/test/java`: `trazabilityCode` → `traceabilityCode`, `TrazabilityCode` → `TraceabilityCode` (getters en `InvoiceService.buildLines`), `trazability_code` → `traceability_code` (los `@Column` de `InvoiceLine`) y el mensaje `"Trazability code 1 cannot be null"` → `"Traceability code 1 cannot be null"`. `DeliveryNoteLine` no lleva `@Column(name)`: la estrategia de nombres ya da `traceability_code1`.

- [ ] **Step 5: Suite completa y búsqueda de restos**

  Run: `./mvnw test`
  Expected: `Tests run: 94, Failures: 0, Errors: 0`. Hibernate valida las columnas renombradas al arrancar cada `@SpringBootTest`.

  Run: `grep -rni trazab src`
  Expected: solo líneas de `src/main/resources/db/migration/V1__init.sql`.

- [ ] **Step 6: CONCEPTO.md §10**

  En la primera frase de §10, `` `trazabilityCode1/2/3` `` → `` `traceabilityCode1/2/3` ``. La sección se reescribe entera en la tarea 2.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/db/migration/V3__traceability_names.sql src/main/java src/test/java ../CONCEPTO.md
git commit -m "rename trazability to traceability"
```

---

### Task 2: Filtros de artículo y lote en el listado de albaranes

**Files:**
- Modify: `main/billing/deliverynote/DeliveryNoteFilter.java`, `DeliveryNoteSpecification.java`
- Test: `test/billing/deliverynote/DeliveryNoteQueriesTest.java`, `test/billing/deliverynote/DeliveryNoteControllerTest.java`, `test/OpenApiDocsTest.java`
- Modify: `CONCEPTO.md` §10, `PENDIENTES.md` §3

**Interfaces:**
- Consumes: `DeliveryNoteLineRequest.builder().traceabilityCode1/2/3(...)` y `DeliveryNoteLine_.traceabilityCode1/2/3` (tarea 1); de `DomainIntegrationTest`, `clientId`, `articleId` (`"Tomate"`), `createClient`, `article`, `articleService`, `deliveryNoteService`.
- Produces: `DeliveryNoteFilter` con dos componentes nuevos al final, `Integer articleId` e `Integer traceabilityCode`, y sus métodos de builder.

- [ ] **Step 1: Escribir los tests de servicio en `DeliveryNoteQueriesTest`**

  Constante, cuatro tests y helpers nuevos. El `codes(client, invoiced)` existente pasa a delegar en `codes(DeliveryNoteFilter)`.

```java
    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 8, 0);

    @Test
    @DisplayName("Delivery notes can be found by article and date, as an inspection asks for them")
    void filtersByArticleAndDate() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        Integer otherClient = createClient(2, DeliveryNoteFormat.PLAIN);
        deliver(101, clientId, OCT_1, line(pepper, 1), line(articleId, 2));
        deliver(102, clientId, OCT_1, line(pepper, 3));
        deliver(103, clientId, OCT_1.plusDays(1), line(articleId, 4));
        deliver(104, otherClient, OCT_1.plusDays(1), line(articleId, 5));
        deliver(105, clientId, OCT_1.plusDays(2), line(articleId, 6));

        DeliveryNoteFilter.DeliveryNoteFilterBuilder tomatoes = DeliveryNoteFilter.builder()
                .articleId(articleId).dateFrom(OCT_1).dateTo(OCT_1.plusDays(1));
        assertThat(codes(tomatoes.build())).containsExactlyInAnyOrder(101, 103, 104);
        assertThat(codes(tomatoes.clientId(clientId).build())).containsExactlyInAnyOrder(101, 103);
    }

    @Test
    @DisplayName("A traceability code is found in any of the three slots, once per delivery note")
    void filtersByTraceabilityCodeInAnySlot() {
        deliver(101, clientId, OCT_1, line(articleId, 12241012));
        deliver(102, clientId, OCT_1, line(articleId, 15241011, 12241012));
        deliver(103, clientId, OCT_1, line(articleId, 15241011, 16241010, 12241012));
        deliver(104, clientId, OCT_1, line(articleId, 15241011, 16241010));
        deliver(105, clientId, OCT_1, line(articleId, 12241012), line(articleId, 12241012));

        DeliveryNoteFilter lot = DeliveryNoteFilter.builder().traceabilityCode(12241012).build();
        assertThat(codes(lot)).containsExactlyInAnyOrder(101, 102, 103, 105);
        assertThat(deliveryNoteService.getAllDeliveryNotes(lot, PageRequest.of(0, 2)).getTotalElements())
                .isEqualTo(4);
    }

    @Test
    @DisplayName("Article and traceability code must be on the same line")
    void articleAndCodeMatchTheSameLine() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        deliver(101, clientId, OCT_1, line(articleId, 12241012), line(pepper, 15241011));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode(12241012).build()))
                .containsExactly(101);
        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).traceabilityCode(15241011).build()))
                .isEmpty();
    }

    @Test
    @DisplayName("Lines replaced by an edit no longer match")
    void ignoresReplacedLines() {
        Integer pepper = articleService.createArticle(article(2, "Pimiento")).id();
        DeliveryNoteResponse edited = deliver(101, clientId, OCT_1, line(articleId, 12241012));
        deliveryNoteService.updateDeliveryNote(edited.id(),
                deliveryNote(101, clientId, OCT_1, line(pepper, 15241011)));

        assertThat(codes(DeliveryNoteFilter.builder().articleId(articleId).build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode(12241012).build())).isEmpty();
        assertThat(codes(DeliveryNoteFilter.builder().traceabilityCode(15241011).build())).containsExactly(101);
    }

    private DeliveryNoteResponse deliver(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return deliveryNoteService.createDeliveryNote(deliveryNote(code, client, date, lines));
    }

    private static DeliveryNoteRequest deliveryNote(int code, Integer client, LocalDateTime date,
            DeliveryNoteLineRequest... lines) {
        return DeliveryNoteRequest.builder().code(code).date(date).clientId(client).lines(List.of(lines)).build();
    }

    private static DeliveryNoteLineRequest line(Integer article, Integer... traceabilityCodes) {
        return DeliveryNoteLineRequest.builder()
                .articleId(article)
                .traceabilityCode1(traceabilityCodes[0])
                .traceabilityCode2(traceabilityCodes.length > 1 ? traceabilityCodes[1] : null)
                .traceabilityCode3(traceabilityCodes.length > 2 ? traceabilityCodes[2] : null)
                .quantity(BigDecimal.ONE)
                .price(BigDecimal.ONE)
                .build();
    }

    private List<Integer> codes(DeliveryNoteFilter filter) {
        return deliveryNoteService.getAllDeliveryNotes(filter, Pageable.unpaged())
                .map(DeliveryNoteResponse::code).getContent();
    }
```

- [ ] **Step 2: Escribir los tests de controlador en `DeliveryNoteControllerTest`**

```java
    @Test
    @DisplayName("GET /api/delivery-notes - Debe pasar los filtros de artículo y trazabilidad, con el cero inicial en decimal")
    void getAllDeliveryNotes_ShouldBindTraceabilityFilters() throws Exception {
        when(deliveryNoteService.getAllDeliveryNotes(any(DeliveryNoteFilter.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/delivery-notes").param("articleId", "3").param("traceabilityCode", "05241012"))
                .andExpect(status().isOk());

        ArgumentCaptor<DeliveryNoteFilter> filter = ArgumentCaptor.forClass(DeliveryNoteFilter.class);
        verify(deliveryNoteService).getAllDeliveryNotes(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().articleId()).isEqualTo(3);
        assertThat(filter.getValue().traceabilityCode()).isEqualTo(5241012);
    }

    @Test
    @DisplayName("GET /api/delivery-notes - Debe rechazar un código de trazabilidad no numérico")
    void getAllDeliveryNotes_WithNonNumericTraceabilityCode_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/delivery-notes").param("traceabilityCode", "L-24"))
                .andExpect(status().isBadRequest());

        verify(deliveryNoteService, never()).getAllDeliveryNotes(any(), any());
    }
```

  Y en `OpenApiDocsTest.documentsTheApi`:

```java
                .andExpect(jsonPath("$.paths['/api/delivery-notes'].get.parameters[?(@.name == 'traceabilityCode')]")
                        .exists())
```

- [ ] **Step 3: Comprobar que fallan**

  Run: `./mvnw test -Dtest=DeliveryNoteQueriesTest,DeliveryNoteControllerTest,OpenApiDocsTest`
  Expected: `COMPILATION ERROR`, `cannot find symbol` en `articleId(...)` y `traceabilityCode(...)` del builder de `DeliveryNoteFilter`.

- [ ] **Step 4: Añadir los dos filtros a `DeliveryNoteFilter`**

  Al final del record, detrás de `invoiced`:

```java
        @Schema(description = "Only delivery notes with a line of this article")
        Integer articleId,

        @Schema(description = """
                Only delivery notes with a line that carries this traceability code in any of its three slots. \
                With articleId, both must be on the same line""", example = "12241012")
        Integer traceabilityCode
```

- [ ] **Step 5: Añadir el filtro de líneas a `DeliveryNoteSpecification`**

  `withFilter` añade `.and(withLine(filter.articleId(), filter.traceabilityCode()))`. Es un `EXISTS` y no un `join`, para que cada albarán salga una vez y el recuento de la página sea correcto:

```java
    // Article and code on the same line: "tomatoes of lot X" (CONCEPTO.md §10)
    private static Specification<DeliveryNote> withLine(Integer articleId, Integer traceabilityCode) {
        return (root, query, cb) -> {
            if (articleId == null && traceabilityCode == null) return null;
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<DeliveryNoteLine> line = subquery.from(DeliveryNoteLine.class);
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(line.get(DeliveryNoteLine_.deliveryNote), root));
            if (articleId != null) {
                conditions.add(cb.equal(line.get(DeliveryNoteLine_.article).get(BaseEntity_.id), articleId));
            }
            if (traceabilityCode != null) {
                conditions.add(cb.or(
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode1), traceabilityCode),
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode2), traceabilityCode),
                        cb.equal(line.get(DeliveryNoteLine_.traceabilityCode3), traceabilityCode)));
            }
            return cb.exists(subquery.select(line.get(BaseEntity_.id)).where(conditions.toArray(Predicate[]::new)));
        };
    }
```

- [ ] **Step 6: Comprobar que pasan**

  Run: `./mvnw test -Dtest=DeliveryNoteQueriesTest,DeliveryNoteControllerTest,OpenApiDocsTest`
  Expected: `Tests run: 13, Failures: 0, Errors: 0`

  Si `ignoresReplacedLines` encuentra el albarán por su artículo o lote anterior, la subconsulta no está aplicando el `@SQLRestriction` de `DeliveryNoteLine`: se añade a `conditions` `cb.isNull(line.get(SoftDeletableEntity_.deletedAt))`.

- [ ] **Step 7: Reescribir CONCEPTO.md §10**

  Sustituye la sección entera (de `## 10. Trazabilidad` al `---` siguiente, sin incluirlo) por:

```markdown
## 10. Trazabilidad

Cada línea de albarán lleva hasta tres códigos de trazabilidad (`traceabilityCode1/2/3`), el
primero obligatorio, y se copian a `InvoiceLine`. Son lotes de proveedor, y los tres huecos
son equivalentes: una misma línea de tomate puede salir de una caja que sobró ayer de un
proveedor y de otra comprada hoy a otro. No hay entidad ni catálogo de lotes detrás.

El código se teclea a mano: dos cifras de proveedor y seis de fecha. Es un entero, así que un
proveedor `05` pierde el cero inicial; la búsqueda no se ve afectada, y mostrarlo con ocho
cifras es cosa del front. SelfBill no registra compras: el proveedor se lee en el propio
código, y basta con encontrar los registros.

La consulta típica de una inspección es "todos los tomates entre tal y tal fecha", sin
conocer el lote. `GET /api/delivery-notes` filtra por artículo (`articleId`) y por lote
(`traceabilityCode`, en cualquiera de los tres huecos), además de por fecha y cliente. Devuelve
los albaranes enteros, y en cada línea se ve su código.

- Si se filtran artículo y lote a la vez, tienen que estar en la misma línea.
- Un albarán sale una vez aunque coincidan varias de sus líneas.
- Solo cuentan las líneas vivas. Un albarán borrado no fue una entrega, y una línea
  sustituida al editar un albarán era un error corregido.
```

- [ ] **Step 8: PENDIENTES.md**

  §3: borrar la sección `### Trazabilidad` entera.

- [ ] **Step 9: Suite completa**

  Run: `./mvnw test`
  Expected: `Tests run: 100, Failures: 0, Errors: 0`

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/luispuchol/selfbill/selfbill_api/billing/deliverynote/DeliveryNoteFilter.java \
        src/main/java/com/luispuchol/selfbill/selfbill_api/billing/deliverynote/DeliveryNoteSpecification.java \
        src/test/java/com/luispuchol/selfbill/selfbill_api/billing/deliverynote \
        src/test/java/com/luispuchol/selfbill/selfbill_api/OpenApiDocsTest.java \
        ../CONCEPTO.md ../PENDIENTES.md
git commit -m "filter delivery notes by article and traceability code"
```
