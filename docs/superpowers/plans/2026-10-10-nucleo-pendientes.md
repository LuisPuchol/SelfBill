# Núcleo de pendientes: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar las fases 0–3 del spec: el esquema pasa a Flyway, se pueden borrar clientes y artículos sin romper los documentos, el IVA sale de los albaranes y se añaden el filtro de albaranes pendientes y la sugerencia de códigos.

**Architecture:** Spring Boot en capas (controller → service → repository/JPA) sobre SQLite. Flyway pasa a ser el dueño del esquema y Hibernate solo lo valida. Las reglas de dominio viven en los servicios; los tests de integración de servicio (`@SpringBootTest`) cubren el dominio y `@WebMvcTest` cubre los endpoints.

**Tech Stack:** Java 25, Spring Boot 3.5.7, Hibernate 6.6, sqlite-jdbc, Flyway 11.7.2 (versión gestionada por Boot), MapStruct, Lombok, JUnit 5, AssertJ, Mockito.

**Spec:** [docs/superpowers/specs/2026-10-10-pendientes-design.md](../specs/2026-10-10-pendientes-design.md)

## Requisitos previos

- `CONCEPTO.md` (sin seguimiento) y `PENDIENTES.md` (modificado) tienen cambios del usuario
  sin commitear, y las tareas los editan. Antes de la tarea 1, el usuario los commitea o
  autoriza a commitearlos tal cual, junto con este plan y el spec.
- Rama de trabajo: `git switch -c feature/nucleo-pendientes`.
- Los comandos se ejecutan desde `selfbill-api/` en Git Bash (`./mvnw`). En PowerShell, `.\mvnw.cmd`.
- Línea base: `./mvnw test` debe acabar con 43 tests y 0 fallos.

## Global Constraints

- Única dependencia nueva: `org.flywaydb:flyway-core`, sin versión en el pom (la gestiona Boot: 11.7.2).
- El código, los identificadores, los comentarios y los mensajes de `ErrorCode` van en inglés; CONCEPTO.md y PENDIENTES.md, en español.
- Se comenta solo el porqué no evidente de una regla, apuntando a `CONCEPTO.md §n`, como hace el código actual.
- Los errores de dominio se lanzan con `BusinessException` + `ErrorCode`, que el handler global convierte en ProblemDetail con `errorCode`.
- Las reglas de numeración de facturas no se tocan (CONCEPTO §5): el número se asigna al emitir y nunca se libera.
- Toda tarea termina con `./mvnw test` en verde.
- Commits: mensaje corto en inglés y en minúsculas, como los del `git log`, terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Volver a guardar un albarán con un artículo borrado después → 404 `ARTICLE_NOT_FOUND`, nunca un 500 (tarea 2).
2. Una fila borrada y una viva que comparten código o nombre → la búsqueda devuelve la viva (tarea 2).
3. Tipos iguales en valor y distintos en escala (4 frente a 4.00) → no cuentan como mezcla (tarea 4).
4. Cambio de régimen de un cliente con albaranes en un borrador y en una factura emitida → solo cambia lo del borrador (tarea 5).
5. Cambio de régimen sobre un albarán anterior a un cambio de `TaxConfig` → toma el tipo actual; es el límite documentado y queda fijado con un test (tarea 5).

## Mapa de ficheros

Raíz de código: `selfbill-api/src/main/java/com/luispuchol/selfbill/selfbill_api/` (en adelante `main/`).
Raíz de tests: `selfbill-api/src/test/java/com/luispuchol/selfbill/selfbill_api/` (en adelante `test/`).

| Fichero | Responsabilidad | Tareas |
|---|---|---|
| `selfbill-api/pom.xml` | Dependencia de Flyway | 1 |
| `src/main/resources/db/migration/V1__init.sql` | Esquema inicial | 1 |
| `src/main/resources/application.properties`, `src/test/resources/application-test.properties` | URL con FKs, `validate`, BD de test nueva por ejecución | 1 |
| `test/support/DomainIntegrationTest.java` | Base común de los tests de integración: limpieza y helpers | 1 |
| `test/SchemaTest.java` | Garantías del esquema | 1 |
| `main/entity/Client.java`, `Article.java` | Sin `@SQLRestriction` | 2 |
| `main/repository/ClientRepository.java`, `ArticleRepository.java` | Consultas solo de filas vivas; siguiente código | 2, 7 |
| `main/specification/ClientSpecification.java`, `ArticleSpecification.java` | Filtro `deletedAt is null` | 2 |
| `test/service/CatalogDeletionTest.java` | Borrado libre de catálogo | 2 |
| `main/service/DeliveryNoteService.java` | Tipos aplicados, propagación de régimen, código automático | 3, 5, 8 |
| `main/service/InvoiceService.java` | Importes desde los albaranes; recálculo al emitir | 4 |
| `main/service/ClientService.java` | Detecta el cambio de régimen | 2, 5, 7 |
| `test/service/TaxRatesTest.java` | Fase 2 | 3, 4, 5 |
| `main/dto/deliverynote/DeliveryNoteFilter.java`, `main/specification/DeliveryNoteSpecification.java` | Filtro `invoiced` | 6 |
| `main/dto/NextCodeResponse.java` | `{"code": n}` | 7 |
| `test/service/DeliveryNoteQueriesTest.java`, `test/service/CodeSuggestionTest.java` | Fase 3 (servicio) | 6, 7, 8 |
| `test/controller/DeliveryNoteControllerTest.java` | Fase 3 (controlador) | 6, 8 |

---

### Task 1: Flyway gestiona el esquema

**Files:**
- Modify: `selfbill-api/pom.xml`
- Create: `selfbill-api/src/main/resources/db/migration/V1__init.sql`
- Modify: `selfbill-api/src/main/resources/application.properties`
- Modify: `selfbill-api/src/test/resources/application-test.properties`
- Modify: `main/entity/Article.java`, `main/entity/Client.java`, `main/entity/DeliveryNote.java` (columna `code`)
- Modify: `main/repository/InvoiceRepository.java` (comentario de `findMaxCode`)
- Create: `test/support/DomainIntegrationTest.java`
- Modify: `test/service/InvoiceLifecycleTest.java`
- Create: `test/SchemaTest.java`
- Modify: `CONCEPTO.md` §7, `PENDIENTES.md`

**Interfaces:**
- Produces: `public abstract class DomainIntegrationTest` (`@SpringBootTest @ActiveProfiles("test")`), que en el `@BeforeEach` vacía las tablas, crea el `TaxConfig` (4 / 0,5), el cliente 1 (`VALUED`, `WITH_VAT`, `WITHOUT_SURCHARGE`, nombre `"Client 1"`, razón social `"Client 1 SL"`, NIF `"B00000001"`) y el artículo 1 (`"Tomate"`). Miembros `protected`:
  - campos `invoiceService`, `deliveryNoteService`, `clientService`, `articleService`, `taxConfigService`, `jdbc`, `clientId`, `articleId`
  - `Integer createClient(int code, DeliveryNoteFormat format)`
  - `Integer createClient(int code, VatType vatType, SurchargeType surchargeType)`: formato `PLAIN`
  - `static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName)`: `WITH_VAT`, `WITHOUT_SURCHARGE`
  - `static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName, VatType vatType)`: `WITHOUT_SURCHARGE`
  - `static ClientRequest clientRequest(int code, DeliveryNoteFormat format, String businessName, VatType vatType, SurchargeType surchargeType)`
  - `static ArticleRequest article(int code, String name)`
  - `static TaxConfigRequest taxes(String vat, String surcharge)`
  - `DeliveryNoteResponse createDeliveryNote(Integer client, String quantity, String price)`: código correlativo desde 1
  - `DeliveryNoteRequest deliveryNoteRequest(Integer code, Integer client, String quantity, String price)`: admite `code` nulo
  - `InvoiceResponse createInvoice(Integer... deliveryNoteIds)`: para `clientId`
  - `InvoiceResponse createInvoiceFor(Integer client, Integer... deliveryNoteIds)`
  - `InvoiceRequest invoiceRequest(Integer... deliveryNoteIds)`
  - `InvoiceResponse issue(Integer client, DeliveryNoteResponse deliveryNote)`
  - `static void assertFails(ErrorCode expected, ThrowingCallable call)`

- [ ] **Step 1: Extraer la base de los tests de integración**

  Mover a `test/support/DomainIntegrationTest.java` los campos, el `@BeforeEach` y los helpers privados de `InvoiceLifecycleTest`, sin cambiar sus cuerpos y haciéndolos `protected`. Añadir los helpers nuevos de la lista de *Interfaces* (`createClient` con régimen, `clientRequest` de 5 argumentos, `article`, `createInvoiceFor`). `InvoiceLifecycleTest` pasa a `extends DomainIntegrationTest` y conserva solo sus `@Test`.

- [ ] **Step 2: Comprobar que la extracción no cambia nada**

  Run: `./mvnw test -Dtest=InvoiceLifecycleTest`
  Expected: `Tests run: 14, Failures: 0, Errors: 0`

- [ ] **Step 3: Escribir el test del esquema**

```java
package com.luispuchol.selfbill.selfbill_api;

@DisplayName("Schema")
class SchemaTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Foreign keys are enforced")
    void foreignKeysAreEnforced() {
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("The schema comes from the Flyway migrations")
    void schemaComesFromMigrations() {
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("The code of a deleted article, client or delivery note can be reused")
    void deletedCodesCanBeReused() {
        articleService.deleteArticle(articleService.createArticle(article(10, "Pera")).getId());
        assertThat(articleService.createArticle(article(10, "Pera")).getCode()).isEqualTo(10);

        clientService.deleteClient(createClient(5, DeliveryNoteFormat.PLAIN));
        assertThat(createClient(5, DeliveryNoteFormat.PLAIN)).isNotNull();

        DeliveryNoteResponse deleted = createDeliveryNote(clientId, "1", "1");
        deliveryNoteService.deleteDeliveryNote(deleted.getId());
        assertThat(deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(deleted.getCode(), clientId, "1", "1")).getCode()).isEqualTo(deleted.getCode());
    }

    @Test
    @DisplayName("Live codes stay unique")
    void liveCodesStayUnique() {
        articleService.createArticle(article(10, "Pera"));

        assertFails(ErrorCode.ARTICLE_DUPLICATE_CODE, () -> articleService.createArticle(article(10, "Manzana")));
    }

    @Test
    @DisplayName("Invoice numbers are unique within a series")
    void invoiceNumbersAreUniqueWithinASeries() {
        String insert = "INSERT INTO invoices (series, code, status, client_id, created_at, updated_at) "
                + "VALUES (?, ?, 'ISSUED', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbc.update(insert, "GENERAL", 1, clientId);
        jdbc.update(insert, "WITHOUT_VAT", 1, clientId);

        assertThatThrownBy(() -> jdbc.update(insert, "GENERAL", 1, clientId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("UNIQUE constraint failed");
    }
}
```

- [ ] **Step 4: Ejecutarlo y ver que falla**

  Run: `./mvnw test -Dtest=SchemaTest`
  Expected: fallan `foreignKeysAreEnforced` (devuelve 0), `schemaComesFromMigrations` (`no such table: flyway_schema_history`), `deletedCodesCanBeReused` (`UNIQUE constraint failed: articles.code`) e `invoiceNumbersAreUniqueWithinASeries` (no lanza). Pasa `liveCodesStayUnique`.

- [ ] **Step 5: Añadir Flyway y la migración inicial**

  En `pom.xml`, junto a las dependencias de persistencia:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

  Crear `V1__init.sql` con este contenido exacto. Ya está comprobado que Hibernate lo valida y que los tests del ciclo de factura pasan contra él con las FKs activas:

```sql
-- Initial schema. Derived from Hibernate's DDL for the SQLite dialect, with three changes it cannot express:
-- foreign keys, partial unique indexes on codes (only live rows), and the (series, code) invoice number index.
-- Enum columns carry no CHECK: SQLite cannot alter a CHECK without rebuilding the table.

create table articles (
    id integer,
    code integer not null,
    name varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table clients (
    id integer,
    code integer not null,
    business_name varchar(255) not null,
    name varchar(255) not null,
    nif varchar(20),
    email varchar(50),
    address varchar(500),
    city varchar(100),
    province varchar(100),
    postal_code varchar(10),
    phone varchar(20),
    phone2 varchar(20),
    vat_type varchar(255) not null,
    surcharge_type varchar(255) not null,
    default_delivery_note_format varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table invoices (
    id integer,
    series varchar(255) not null,
    code integer,
    date timestamp,
    status varchar(255) not null,
    client_id integer not null references clients (id),
    client_business_name varchar(255),
    client_nif varchar(20),
    client_address varchar(500),
    client_city varchar(100),
    client_province varchar(100),
    client_postal_code varchar(10),
    subtotal numeric(10,2),
    vat_percentage numeric(5,2),
    vat_amount numeric(10,2),
    surcharge_percentage numeric(5,2),
    surcharge_amount numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table delivery_notes (
    id integer,
    code integer not null,
    date timestamp not null,
    total numeric(10,2),
    format varchar(255) not null,
    vat_percentage numeric(5,2) not null,
    surcharge_percentage numeric(5,2) not null,
    client_id integer not null references clients (id),
    invoice_id integer references invoices (id),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table delivery_notes_articles (
    id integer,
    article_id integer not null references articles (id),
    delivery_note_id integer not null references delivery_notes (id),
    trazability_code1 integer not null,
    trazability_code2 integer,
    trazability_code3 integer,
    quantity numeric(10,2),
    price numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table invoice_lines (
    id integer,
    invoice_id integer not null references invoices (id),
    delivery_note_id integer references delivery_notes (id),
    article_id integer references articles (id),
    delivery_note_code integer not null,
    delivery_note_date timestamp not null,
    article_name varchar(255) not null,
    article_code integer not null,
    trazability_code1 integer not null,
    trazability_code2 integer,
    trazability_code3 integer,
    quantity numeric(10,2),
    price numeric(10,2),
    total numeric(10,2),
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,
    primary key (id)
);

create table tax_config (
    id integer,
    vat_percentage numeric(5,2) not null,
    surcharge_percentage numeric(5,2) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table user_profile (
    id integer,
    full_name varchar(255) not null,
    fiscal_name varchar(255) not null,
    nif varchar(20),
    address varchar(500),
    email varchar(50),
    phone varchar(20),
    smtp_host varchar(255),
    smtp_port integer,
    mail_password_encrypted varchar(500),
    logo_data blob,
    logo_content_type varchar(50),
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

-- A deleted row frees its code; only one live row per code (CONCEPTO.md §7)
create unique index ux_articles_code_live on articles (code) where deleted_at is null;
create unique index ux_clients_code_live on clients (code) where deleted_at is null;
create unique index ux_delivery_notes_code_live on delivery_notes (code) where deleted_at is null;

-- Invoice numbers are never freed, deleted rows included (CONCEPTO.md §5)
create unique index ux_invoices_series_code on invoices (series, code);

create index idx_trazability_code1 on delivery_notes_articles (trazability_code1);
create index idx_trazability_code2 on delivery_notes_articles (trazability_code2);
create index idx_trazability_code3 on delivery_notes_articles (trazability_code3);
```

- [ ] **Step 6: Configuración, entidades y BD local**

  - `application.properties`: `spring.datasource.url=jdbc:sqlite:./selfbill.db?foreign_keys=true` y `spring.jpa.hibernate.ddl-auto=validate`.
  - `application-test.properties`: `spring.jpa.hibernate.ddl-auto=validate` y
    `spring.datasource.url=jdbc:sqlite:target/selfbill-test-${random.uuid}.db?foreign_keys=true`, con este comentario encima:
    `# A fresh database per test run: Flyway's SQLite clean cannot drop tables with foreign keys on`.
  - `Article`, `Client` y `DeliveryNote`: `code` pasa a `@Column(nullable = false)` (sin `unique = true`), con el comentario
    `// Unique among live rows only: partial index in V1__init.sql (CONCEPTO.md §7)`.
  - `InvoiceRepository.findMaxCode`: el comentario pasa a
    `// Native so it also counts soft-deleted invoices: their numbers stay taken (ux_invoices_series_code)`.
  - BD local, que ya no coincide con el código: `mv selfbill.db selfbill.db.bak`.

- [ ] **Step 7: Ejecutar la suite completa**

  Run: `./mvnw test`
  Expected: `Tests run: 48, Failures: 0, Errors: 0`

- [ ] **Step 8: Probar el arranque contra una BD nueva**

  Arrancar `./mvnw spring-boot:run` en segundo plano y, cuando el log muestre `Successfully applied 1 migration`, ejecutar `curl -s localhost:8080/actuator/health`.
  Expected: `{"status":"UP"}`. Después, parar la app.

- [ ] **Step 9: Documentación**

  - CONCEPTO §7: sustituir la viñeta "Una fila borrada conserva su código y la restricción `unique` lo mantiene ocupado." por
    "Una fila borrada libera su código: el índice único de `code` es parcial (`WHERE deleted_at IS NULL`), así que solo puede haber una fila viva por código. Restaurar una fila cuyo código ya está vivo obliga a cambiárselo. Los números de factura son la excepción: no se liberan nunca (§5)."
  - CONCEPTO §7: añadir la viñeta "El esquema lo gestiona Flyway (`db/migration`) y Hibernate solo lo valida. Las FKs están declaradas y activas (`foreign_keys=true`)."
  - PENDIENTES: quitar "El código de un artículo, cliente o albarán borrado da un 500 al reutilizarse" y "Migraciones de esquema".
  - PENDIENTES, en "Rectificativas, anulación y Verifactu": añadir "Obligatorio desde el 1-ene-2027 (sociedades) o el 1-jul-2027 (resto), según el RDL 15/2025. Falta confirmar con la gestoría cuál aplica."

- [ ] **Step 10: Commit**

```bash
git add selfbill-api/pom.xml selfbill-api/src CONCEPTO.md PENDIENTES.md
git commit -m "manage schema with flyway"
```

---

### Task 2: Los documentos siguen leyendo clientes y artículos borrados

**Files:**
- Modify: `main/entity/Client.java`, `main/entity/Article.java`
- Modify: `main/repository/ClientRepository.java`, `main/repository/ArticleRepository.java`
- Modify: `main/specification/ClientSpecification.java`, `main/specification/ArticleSpecification.java`
- Modify: `main/service/ClientService.java`, `main/service/ArticleService.java`, `main/service/DeliveryNoteService.java` (`findClient`, `findArticle`), `main/service/InvoiceService.java` (`findClient`)
- Create: `test/service/CatalogDeletionTest.java`
- Modify: `CONCEPTO.md` §7, `PENDIENTES.md`

**Interfaces:**
- Consumes: `DomainIntegrationTest` (tarea 1).
- Produces. Estos métodos sustituyen a los actuales del mismo nombre sin el sufijo, que desaparecen:
  - `ClientRepository`: `Optional<Client> findByIdAndDeletedAtIsNull(Integer id)`, `findByCodeAndDeletedAtIsNull(Integer code)`, `findByNameIgnoreCaseAndDeletedAtIsNull(String name)`, `findByNifAndDeletedAtIsNull(String nif)`, `boolean existsByCodeAndDeletedAtIsNull(Integer code)`
  - `ArticleRepository`: `Optional<Article> findByIdAndDeletedAtIsNull(Integer id)`, `findByCodeAndDeletedAtIsNull(Integer code)`, `findByNameIgnoreCaseAndDeletedAtIsNull(String name)`, `boolean existsByCodeAndDeletedAtIsNull(Integer code)`

- [ ] **Step 1: Escribir los tests**

```java
@DisplayName("Catalog deletion")
class CatalogDeletionTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Documents of a deleted client stay readable")
    void deletedClientKeepsItsDocumentsReadable() {
        DeliveryNoteResponse invoiced = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");
        InvoiceResponse invoice = invoiceService.issueInvoice(createInvoice(invoiced.getId()).getId());

        clientService.deleteClient(clientId);

        assertThat(deliveryNoteService.getDeliveryNoteById(pending.getId()).getClientName()).isEqualTo("Client 1");
        assertThat(deliveryNoteService.getAllDeliveryNotes(new DeliveryNoteFilter(), Pageable.unpaged())
                .getContent()).hasSize(2);
        assertThat(invoiceService.getInvoiceById(invoice.getId()).getClientName()).isEqualTo("Client 1");
        assertThat(invoiceService.getAllInvoices(new InvoiceFilter(), Pageable.unpaged()).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Delivery notes using a deleted article stay readable")
    void deletedArticleKeepsItsDocumentsReadable() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");

        articleService.deleteArticle(articleId);

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.getId()).getLines().get(0).getArticleName())
                .isEqualTo("Tomate");
    }

    @Test
    @DisplayName("The catalog hides deleted clients and articles")
    void catalogHidesDeletedRows() {
        clientService.deleteClient(clientId);
        articleService.deleteArticle(articleId);

        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.getClientById(clientId));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.getClientByCode(1));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> clientService.deleteClient(clientId));
        assertThat(clientService.getAllClients(new ClientFilter(), Pageable.unpaged()).getContent()).isEmpty();
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> articleService.getArticleById(articleId));
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> articleService.getArticleByCode(1));
        assertThat(articleService.getAllArticles(new ArticleFilter(), Pageable.unpaged()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("New documents cannot use a deleted client or article")
    void newDocumentsRejectDeletedCatalogRows() {
        DeliveryNoteResponse pending = createDeliveryNote(clientId, "1", "1");

        articleService.deleteArticle(articleId);
        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> createDeliveryNote(clientId, "1", "1"));

        clientService.deleteClient(clientId);
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> createDeliveryNote(clientId, "1", "1"));
        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> createInvoice(pending.getId()));
    }

    @Test
    @DisplayName("Re-saving a delivery note whose article was deleted fails with a not-found error")
    void resavingWithDeletedArticleFailsCleanly() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "1", "1");
        articleService.deleteArticle(articleId);

        assertFails(ErrorCode.ARTICLE_NOT_FOUND, () -> deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), clientId, "2", "1")));
    }

    @Test
    @DisplayName("Lookups return the live row when a deleted one shares its code, name or NIF")
    void lookupsPreferTheLiveRow() {
        clientService.deleteClient(clientId);
        articleService.deleteArticle(articleId);
        Integer liveClient = createClient(1, DeliveryNoteFormat.PLAIN);
        Integer liveArticle = articleService.createArticle(article(1, "Tomate")).getId();

        assertThat(clientService.getClientByCode(1).getId()).isEqualTo(liveClient);
        assertThat(clientService.getClientByName("Client 1").getId()).isEqualTo(liveClient);
        assertThat(clientService.getClientByNif("B00000001").getId()).isEqualTo(liveClient);
        assertThat(articleService.getArticleByCode(1).getId()).isEqualTo(liveArticle);
        assertThat(articleService.getArticleByName("Tomate").getId()).isEqualTo(liveArticle);
    }
}
```

- [ ] **Step 2: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=CatalogDeletionTest`
  Expected: fallan `deletedClientKeepsItsDocumentsReadable` y `deletedArticleKeepsItsDocumentsReadable` (`Unable to find ... with id`). Los otros cuatro pasan ya y protegen el cambio.

- [ ] **Step 3: Implementar**

  - `Client` y `Article`: quitar `@SQLRestriction` y su import, manteniendo `@SQLDelete`. Comentario sobre la clase:
    `// No @SQLRestriction: documents must still load a deleted row; the catalog filters deleted rows itself (CONCEPTO.md §7)`.
  - Repositorios: sustituir los métodos por los de *Interfaces*.
  - `ClientSpecification.withFilter` y `ArticleSpecification.withFilter`: añadir `.and(notDeleted())`, con `notDeleted()` = `deletedAt is null`.
  - Servicios: cada búsqueda de cliente o artículo pasa a la variante `...AndDeletedAtIsNull`: catálogo, `requireUnusedCode`, `findClient`/`findArticle` de `DeliveryNoteService` y `findClient` de `InvoiceService`.

- [ ] **Step 4: Comprobar que no queda ninguna búsqueda sin filtrar**

  Run: `grep -rnE "(clientRepository|articleRepository)\.(findById|findByCode|findByNameIgnoreCase|findByNif|existsByCode)\(" selfbill-api/src/main/java`
  Expected: sin salida.

- [ ] **Step 5: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: `CatalogDeletionTest` con 6 tests en verde; total sin fallos.

- [ ] **Step 6: Documentación**

  - CONCEPTO §7: sustituir la viñeta de `@SQLRestriction` por dos:
    "`Client` y `Article` no llevan `@SQLRestriction`. Un documento sigue leyendo su cliente y sus artículos aunque estén borrados, porque el albarán no guarda copia de ellos, y restaurar una fila es vaciar `deleted_at`. El catálogo filtra las filas borradas en sus consultas, y no se puede crear un documento nuevo con un cliente o artículo borrado."
    y "Los documentos (albarán, factura y sus líneas) sí llevan `@SQLRestriction`. Ninguna relación viva apunta a un documento borrado: un albarán facturado no se borra, y borrar una factura desvincula antes sus albaranes."
  - PENDIENTES: quitar "Borrar un cliente con albaranes o facturas los deja inservibles". Si la sección "2. Bugs conocidos" queda vacía, poner "Ninguno conocido."
  - PENDIENTES, en "Generación real de PDF": añadir "Antes hay que decidir dos snapshots que faltan (CONCEPTO §6): los datos del cliente en el albarán y los del titular (`UserProfile`) en la factura. Sin ellos, editar un cliente o el perfil reescribe la reimpresión de documentos ya entregados."

- [ ] **Step 7: Commit**

```bash
git add selfbill-api/src CONCEPTO.md PENDIENTES.md
git commit -m "keep deleted clients and articles readable from documents"
```

---

### Task 3: El albarán congela los tipos aplicados

**Files:**
- Modify: `main/service/DeliveryNoteService.java`
- Create: `test/service/TaxRatesTest.java`
- Modify: `test/service/InvoiceLifecycleTest.java` (`deliveryNoteKeepsItsFormatAndTaxes`)
- Modify: `CONCEPTO.md` §3

**Interfaces:**
- Consumes: `DomainIntegrationTest` (tarea 1).
- Produces: `private void applyTaxRegime(DeliveryNote deliveryNote, Client client)` en `DeliveryNoteService`, que la tarea 5 reutiliza.

- [ ] **Step 1: Escribir los tests**

```java
@DisplayName("Tax rates")
class TaxRatesTest extends DomainIntegrationTest {

    @Test
    @DisplayName("A delivery note freezes the rates its client actually pays")
    void deliveryNoteFreezesAppliedRates() {
        Integer withoutVatWithSurcharge = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE);

        DeliveryNoteResponse withVat = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse withSurcharge = createDeliveryNote(withoutVatWithSurcharge, "10", "2");

        assertThat(withVat.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(withVat.getSurchargePercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.getVatPercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.getSurchargePercentage()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("Moving a delivery note to another client applies that client's regime")
    void movingDeliveryNoteAppliesNewClientRegime() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        DeliveryNoteResponse moved = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), withoutVat, "10", "2"));

        assertThat(moved.getVatPercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a delivery note keeps the rates it was created with")
    void editingKeepsFrozenRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse edited = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), clientId, "5", "2"));

        assertThat(edited.getVatPercentage()).isEqualByComparingTo("4");
    }
}
```

  En `InvoiceLifecycleTest.deliveryNoteKeepsItsFormatAndTaxes`, en lugar de `clientId` usar un cliente propio creado con
  `clientRequest(2, DeliveryNoteFormat.VALUED, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE)`.
  Actualizarlo con `clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL", VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE)`,
  es decir, con el mismo régimen. Las aserciones no cambian: `VALUED`, `4` y `0.5`.

- [ ] **Step 2: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=TaxRatesTest`
  Expected: fallan `deliveryNoteFreezesAppliedRates` (el cliente 1 tiene recargo 0.5) y `movingDeliveryNoteAppliesNewClientRegime` (sigue en 4). Pasa `editingKeepsFrozenRates`.

- [ ] **Step 3: Implementar `applyTaxRegime`**

  Sustituye a `freezeTaxRates`:
  - `vatPercentage` = IVA del `TaxConfig` vigente si el cliente es `WITH_VAT`; si no, `BigDecimal.ZERO`.
  - `surchargePercentage` = recargo vigente si es `WITH_SURCHARGE`; si no, `BigDecimal.ZERO`.

  Se llama en `createDeliveryNote` y en `updateDeliveryNote` cuando cambia el cliente, junto al formato.
  Comentario: `// Frozen with the client's regime applied, so a reprint never consults the client (CONCEPTO.md §3, §4)`.

- [ ] **Step 4: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos. `InvoiceLifecycleTest` sigue con 14 tests en verde.

- [ ] **Step 5: Documentación**

  CONCEPTO §3, "Dónde reside", tras la viñeta de `DeliveryNote`: "Los tipos son los **aplicados**, con el régimen del cliente ya resuelto: el IVA es 0 si el cliente no lleva IVA, y el recargo es 0 si no lleva recargo. Se fijan al crear el albarán y al cambiarlo de cliente."

- [ ] **Step 6: Commit**

```bash
git add selfbill-api/src CONCEPTO.md
git commit -m "freeze applied tax rates in delivery notes"
```

---

### Task 4: La factura se calcula con los tipos de sus albaranes

**Files:**
- Modify: `main/exception/ErrorCode.java`
- Modify: `main/service/InvoiceService.java`
- Modify: `test/service/TaxRatesTest.java`
- Modify: `CONCEPTO.md` §4 y §5, `PENDIENTES.md`

**Interfaces:**
- Consumes: tipos aplicados del albarán (tarea 3).
- Produces: `ErrorCode.INVOICE_MIXED_TAX_RATES` (409). `InvoiceService` deja de depender de `TaxConfigService`.

- [ ] **Step 1: Añadir el código de error**

  En `ErrorCode`, dentro del bloque de factura:
  `INVOICE_MIXED_TAX_RATES("All delivery notes of an invoice must have the same VAT and surcharge rates", HttpStatus.CONFLICT)`.

- [ ] **Step 2: Escribir los tests (en `TaxRatesTest`)**

```java
    @Test
    @DisplayName("An invoice uses the rates frozen in its delivery notes, not the current ones")
    void invoiceUsesFrozenRates() {
        Integer withSurcharge = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withSurcharge, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        InvoiceResponse invoice = createInvoiceFor(withSurcharge, deliveryNote.getId());

        assertThat(invoice.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(invoice.getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoice.getSurchargePercentage()).isEqualByComparingTo("0.5");
        assertThat(invoice.getSurchargeAmount()).isEqualByComparingTo("0.10");
        assertThat(invoice.getTotal()).isEqualByComparingTo("20.90");
    }

    @Test
    @DisplayName("Delivery notes with different rates cannot share an invoice")
    void mixedRatesAreRejected() {
        DeliveryNoteResponse before = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));
        DeliveryNoteResponse after = createDeliveryNote(clientId, "10", "2");

        assertFails(ErrorCode.INVOICE_MIXED_TAX_RATES, () -> createInvoice(before.getId(), after.getId()));
    }

    @Test
    @DisplayName("Rates equal in value but written differently are not mixed")
    void equalRatesWithDifferentScaleAreNotMixed() {
        DeliveryNoteResponse first = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("4.00", "0.50"));
        DeliveryNoteResponse second = createDeliveryNote(clientId, "10", "2");

        assertThat(createInvoice(first.getId(), second.getId()).getVatAmount()).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("Issuing recalculates a draft whose stored amounts went stale")
    void issuingRecalculatesTheDraft() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").getId());
        jdbc.update("UPDATE invoices SET vat_amount = 0, total = 0 WHERE id = ?", draft.getId());

        InvoiceResponse issued = invoiceService.issueInvoice(draft.getId());

        assertThat(issued.getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(issued.getTotal()).isEqualByComparingTo("20.80");
    }
```

- [ ] **Step 3: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=TaxRatesTest`
  Expected: fallan `invoiceUsesFrozenRates` (usa 10 %), `mixedRatesAreRejected` (no lanza) e `issuingRecalculatesTheDraft` (IVA 0). Pasa `equalRatesWithDifferentScaleAreNotMixed`.

- [ ] **Step 4: Implementar**

  - `applyFinancials(Invoice, List<DeliveryNote>)`: los tipos salen de
    `private static BigDecimal commonRate(List<DeliveryNote> deliveryNotes, Function<DeliveryNote, BigDecimal> rate)`.
    Devuelve el tipo del primer albarán; si alguno difiere según `compareTo` (nunca `equals`, que compara la escala),
    lanza `BusinessException(ErrorCode.INVOICE_MIXED_TAX_RATES)`; con la lista vacía devuelve `BigDecimal.ZERO`.
    Las cuotas son base × tipo con `percentageOf`, como hoy. Desaparecen la lectura de `TaxConfig`, las comprobaciones
    de `VatType`/`SurchargeType` y el campo `taxConfigService`.
    Comentario: `// One rate per invoice: the business never mixes them, so a mix is refused, not broken down (CONCEPTO.md §8)`.
  - `issueInvoice`: tras `requireDraft`, `recalculate(invoice, invoice.getDeliveryNotes())`, con el comentario
    `// Safety net: amounts and series must match the delivery notes and client being issued`.

- [ ] **Step 5: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos. `TaxRatesTest` con 7 tests.

- [ ] **Step 6: Documentación**

  - CONCEPTO §4: tras "El albarán congela el tipo con el que se valoró, y la factura usa el de sus albaranes.", añadir: "Todos sus albaranes deben llevar el mismo tipo; si no, la factura se rechaza (409). El negocio no mezcla tipos en una factura, así que no hay desglose (§8)."
  - CONCEPTO §5, "Reglas": añadir "Emitir recalcula la factura desde sus albaranes antes de numerarla."
  - PENDIENTES: quitar "IVA por fecha de operación".

- [ ] **Step 7: Commit**

```bash
git add selfbill-api/src CONCEPTO.md PENDIENTES.md
git commit -m "compute invoice amounts from delivery note rates"
```

---

### Task 5: El cambio de régimen del cliente llega a lo pendiente

**Files:**
- Modify: `main/repository/DeliveryNoteRepository.java`
- Modify: `main/service/DeliveryNoteService.java`
- Modify: `main/service/ClientService.java`
- Modify: `test/service/TaxRatesTest.java`
- Modify: `CONCEPTO.md` §3, `PENDIENTES.md`

**Interfaces:**
- Consumes: `applyTaxRegime` (tarea 3), `InvoiceService.recalculateDraft(Invoice)` (existente).
- Produces:
  - `List<DeliveryNote> DeliveryNoteRepository.findEditableByClientId(Integer clientId)`
  - `@Transactional public void DeliveryNoteService.refreshTaxRegime(Client client)`
  - `ClientService` recibe `DeliveryNoteService` por constructor. No hay ciclo: `DeliveryNoteService` no depende de `ClientService`.

- [ ] **Step 1: Escribir los tests (en `TaxRatesTest`)**

```java
    @Test
    @DisplayName("Changing a client's regime reaches its pending delivery notes and drafts")
    void regimeChangeReachesPendingDocuments() {
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse free = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse draft = createInvoice(inDraft.getId());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE));

        DeliveryNoteResponse refreshed = deliveryNoteService.getDeliveryNoteById(free.getId());
        assertThat(refreshed.getVatPercentage()).isEqualByComparingTo("0");
        assertThat(refreshed.getSurchargePercentage()).isEqualByComparingTo("0.5");
        InvoiceResponse recalculated = invoiceService.getInvoiceById(draft.getId());
        assertThat(recalculated.getSeries()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(recalculated.getVatAmount()).isEqualByComparingTo("0");
        assertThat(recalculated.getSurchargeAmount()).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("Changing a client's regime leaves its issued invoices and their delivery notes alone")
    void regimeChangeLeavesIssuedDocumentsAlone() {
        DeliveryNoteResponse issuedNote = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse issued = invoiceService.issueInvoice(createInvoice(issuedNote.getId()).getId());
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").getId());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE));

        assertThat(deliveryNoteService.getDeliveryNoteById(issuedNote.getId()).getVatPercentage())
                .isEqualByComparingTo("4");
        assertThat(invoiceService.getInvoiceById(issued.getId()).getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoiceService.getInvoiceById(draft.getId()).getVatAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a client without changing its regime does not refresh rates")
    void nonRegimeEditKeepsRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Renamed SL"));

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.getId()).getVatPercentage())
                .isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("A regime change applies today's rate, even to older delivery notes (no rate history)")
    void regimeChangeUsesTodaysRate() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withoutVat, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(withoutVat, clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL",
                VatType.WITH_VAT, SurchargeType.WITHOUT_SURCHARGE));

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.getId()).getVatPercentage())
                .isEqualByComparingTo("10");
    }
```

- [ ] **Step 2: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=TaxRatesTest`
  Expected: fallan `regimeChangeReachesPendingDocuments`, `regimeChangeLeavesIssuedDocumentsAlone` (en la aserción del borrador) y `regimeChangeUsesTodaysRate`. Pasa `nonRegimeEditKeepsRates`.

- [ ] **Step 3: Implementar**

  Consulta del repositorio:

```java
    // Not locked by an issued invoice (CONCEPTO.md §5)
    @Query("SELECT dn FROM DeliveryNote dn LEFT JOIN dn.invoice i WHERE dn.client.id = :clientId "
            + "AND (i IS NULL OR i.status = com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus.DRAFT)")
    List<DeliveryNote> findEditableByClientId(@Param("clientId") Integer clientId);
```

  - `refreshTaxRegime(Client client)`: `applyTaxRegime` sobre cada albarán de `findEditableByClientId`, y después
    `invoiceService.recalculateDraft` una vez por cada factura distinta (no nula) de esos albaranes.
  - `ClientService.updateClient`: antes de `clientMapper.updateEntity`, guardar si cambian `vatType` o `surchargeType`;
    después de guardar, si cambiaron, llamar a `deliveryNoteService.refreshTaxRegime(client)`.
    Comentario: `// Usually a misconfigured client being fixed: pending documents follow it (CONCEPTO.md §3)`.

- [ ] **Step 4: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos. `TaxRatesTest` con 11 tests.

- [ ] **Step 5: Documentación**

  - CONCEPTO §3, tras el párrafo añadido en la tarea 3: "Cambiar el régimen de un cliente reaplica los tipos a sus albaranes editables (sin factura o con factura en borrador) y recalcula sus borradores. Los de facturas emitidas no se tocan. Cambiar el `TaxConfig` no afecta a nada que ya exista."
  - PENDIENTES: quitar "Un borrador no se entera de los cambios en su cliente ni en el IVA" y añadir en "1. Lógica":
    "### `TaxConfig` sin histórico\nEl albarán congela el tipo vigente cuando se crea, no el de su fecha: si se da de alta tarde un albarán anterior a un cambio de tipo, recibe el tipo nuevo. Lo mismo ocurre al reaplicar el régimen de un cliente. Si llega a importar, `TaxConfig` necesita vigencias (desde/hasta)."

- [ ] **Step 6: Commit**

```bash
git add selfbill-api/src CONCEPTO.md PENDIENTES.md
git commit -m "propagate client tax regime changes to pending documents"
```

---

### Task 6: Filtro de albaranes pendientes de facturar

**Files:**
- Modify: `main/dto/deliverynote/DeliveryNoteFilter.java`
- Modify: `main/specification/DeliveryNoteSpecification.java`
- Create: `test/service/DeliveryNoteQueriesTest.java`
- Create: `test/controller/DeliveryNoteControllerTest.java`
- Modify: `PENDIENTES.md`

**Interfaces:**
- Produces: `DeliveryNoteFilter.invoiced` (`Boolean`; `getInvoiced()`/`setInvoiced(Boolean)` por Lombok). Con `true` devuelve los albaranes que tienen factura, borrador o emitida; con `false`, los que no tienen; con `null` no filtra.

- [ ] **Step 1: Añadir el campo `private Boolean invoiced;` a `DeliveryNoteFilter`**

- [ ] **Step 2: Escribir los tests**

```java
@DisplayName("Delivery note queries")
class DeliveryNoteQueriesTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Delivery notes can be filtered by whether they are invoiced")
    void filtersByInvoiced() {
        Integer otherClient = createClient(2, DeliveryNoteFormat.PLAIN);
        DeliveryNoteResponse free = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "1", "1");
        DeliveryNoteResponse inIssued = createDeliveryNote(clientId, "1", "1");
        createDeliveryNote(otherClient, "1", "1");
        createInvoice(inDraft.getId());
        invoiceService.issueInvoice(createInvoice(inIssued.getId()).getId());

        assertThat(codes(clientId, false)).containsExactly(free.getCode());
        assertThat(codes(clientId, true)).containsExactlyInAnyOrder(inDraft.getCode(), inIssued.getCode());
        assertThat(codes(clientId, null)).hasSize(3);
    }

    private List<Integer> codes(Integer client, Boolean invoiced) {
        DeliveryNoteFilter filter = new DeliveryNoteFilter();
        filter.setClientId(client);
        filter.setInvoiced(invoiced);
        return deliveryNoteService.getAllDeliveryNotes(filter, Pageable.unpaged())
                .map(DeliveryNoteResponse::getCode).getContent();
    }
}
```

```java
@WebMvcTest(DeliveryNoteController.class)
@DisplayName("Delivery Note Controller Tests")
class DeliveryNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryNoteService deliveryNoteService;

    @Test
    @DisplayName("GET /api/deliveryNotes - Debe pasar el filtro de facturado al servicio")
    void getAllDeliveryNotes_ShouldBindInvoicedFilter() throws Exception {
        when(deliveryNoteService.getAllDeliveryNotes(any(DeliveryNoteFilter.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/deliveryNotes").param("clientId", "1").param("invoiced", "false"))
                .andExpect(status().isOk());

        ArgumentCaptor<DeliveryNoteFilter> filter = ArgumentCaptor.forClass(DeliveryNoteFilter.class);
        verify(deliveryNoteService).getAllDeliveryNotes(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().getClientId()).isEqualTo(1);
        assertThat(filter.getValue().getInvoiced()).isFalse();
    }
}
```

- [ ] **Step 3: Ejecutarlos y ver que falla el de servicio**

  Run: `./mvnw test -Dtest=DeliveryNoteQueriesTest,DeliveryNoteControllerTest`
  Expected: falla `filtersByInvoiced` (con `false` devuelve los 3). El de controlador pasa: el binding ya existe al añadir el campo.

- [ ] **Step 4: Implementar `invoiced(Boolean)` en `DeliveryNoteSpecification`**

  `true` → `invoice is not null`; `false` → `invoice is null`; `null` → sin restricción. Se encadena en `withFilter`.

- [ ] **Step 5: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos.

- [ ] **Step 6: Documentación**

  PENDIENTES: quitar "Filtro de albaranes sin facturar".

- [ ] **Step 7: Commit**

```bash
git add selfbill-api/src PENDIENTES.md
git commit -m "filter delivery notes by invoiced"
```

---

### Task 7: Código sugerido para cliente y artículo

**Files:**
- Create: `main/dto/NextCodeResponse.java`
- Modify: `main/repository/ClientRepository.java`, `main/repository/ArticleRepository.java`
- Modify: `main/service/ClientService.java`, `main/service/ArticleService.java`
- Modify: `main/controller/ClientController.java`, `main/controller/ArticleController.java`
- Create: `test/service/CodeSuggestionTest.java`
- Modify: `test/controller/ClientControllerTest.java`, `test/controller/ArticleControllerTest.java`

**Interfaces:**
- Produces:
  - `public record NextCodeResponse(Integer code) {}`
  - `int ClientRepository.findNextCode()` con `@Query("SELECT COALESCE(MAX(c.code), 0) + 1 FROM Client c WHERE c.deletedAt IS NULL")`; `ArticleRepository.findNextCode()`, igual sobre `Article a`
  - `NextCodeResponse ClientService.getNextCode()`, `NextCodeResponse ArticleService.getNextCode()`
  - `GET /api/clients/next-code`, `GET /api/articles/next-code` → `200 {"code": n}`

- [ ] **Step 1: Escribir los tests**

```java
@DisplayName("Code suggestion")
class CodeSuggestionTest extends DomainIntegrationTest {

    @Test
    @DisplayName("The suggested client code follows the highest live one")
    void suggestsNextClientCode() {
        Integer seventh = createClient(7, DeliveryNoteFormat.PLAIN);
        assertThat(clientService.getNextCode().code()).isEqualTo(8);

        clientService.deleteClient(seventh);
        assertThat(clientService.getNextCode().code()).isEqualTo(2);
    }

    @Test
    @DisplayName("The suggested article code follows the highest live one, starting at 1")
    void suggestsNextArticleCode() {
        assertThat(articleService.getNextCode().code()).isEqualTo(2);

        articleService.deleteArticle(articleId);
        assertThat(articleService.getNextCode().code()).isEqualTo(1);
    }
}
```

  En `ClientControllerTest` (y su equivalente en `ArticleControllerTest`, con `/api/articles/next-code`):

```java
    @Test
    @DisplayName("GET /api/clients/next-code - Debe devolver el código sugerido")
    void getNextCode_ShouldReturnSuggestedCode() throws Exception {
        when(clientService.getNextCode()).thenReturn(new NextCodeResponse(8));

        mockMvc.perform(get("/api/clients/next-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(8));
    }
```

- [ ] **Step 2: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=CodeSuggestionTest,ClientControllerTest,ArticleControllerTest`
  Expected: error de compilación (`getNextCode` y `NextCodeResponse` no existen).

- [ ] **Step 3: Implementar** el record, las consultas, los métodos de servicio y los endpoints (`@Operation(summary = "Suggest the next free code")`) según *Interfaces*.

- [ ] **Step 4: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos. Fijarse en que `/next-code` no lo captura `/{id}`: daría 400 por `@Positive`.

- [ ] **Step 5: Commit**

```bash
git add selfbill-api/src
git commit -m "suggest next client and article codes"
```

---

### Task 8: El servidor asigna el código del albarán si viene vacío

**Files:**
- Modify: `main/dto/deliverynote/DeliveryNoteRequest.java`
- Modify: `main/repository/DeliveryNoteRepository.java`
- Modify: `main/service/DeliveryNoteService.java`
- Modify: `test/service/CodeSuggestionTest.java`
- Modify: `test/controller/DeliveryNoteControllerTest.java`
- Modify: `PENDIENTES.md`

**Interfaces:**
- Consumes: `DeliveryNoteControllerTest` (tarea 6), `CodeSuggestionTest` (tarea 7).
- Produces: `int DeliveryNoteRepository.findNextCode()` con `@Query("SELECT COALESCE(MAX(dn.code), 0) + 1 FROM DeliveryNote dn")` (el `@SQLRestriction` de `DeliveryNote` ya excluye los borrados). `DeliveryNoteRequest.code` pasa a ser opcional: conserva `@Positive` y pierde `@NotNull`.

- [ ] **Step 1: Escribir los tests**

  En `CodeSuggestionTest`:

```java
    @Test
    @DisplayName("A delivery note without code gets the next free one")
    void deliveryNoteWithoutCodeGetsTheNextOne() {
        deliveryNoteService.createDeliveryNote(deliveryNoteRequest(5, clientId, "1", "1"));

        DeliveryNoteResponse automatic = deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(null, clientId, "1", "1"));

        assertThat(automatic.getCode()).isEqualTo(6);
    }

    @Test
    @DisplayName("Editing a delivery note without code keeps its current one")
    void editingWithoutCodeKeepsIt() {
        DeliveryNoteResponse deliveryNote = deliveryNoteService.createDeliveryNote(
                deliveryNoteRequest(5, clientId, "1", "1"));

        DeliveryNoteResponse edited = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(null, clientId, "2", "1"));

        assertThat(edited.getCode()).isEqualTo(5);
    }
```

  En `DeliveryNoteControllerTest`:

```java
    @Test
    @DisplayName("POST /api/deliveryNotes - Debe aceptar un albarán sin código")
    void createDeliveryNote_WithoutCode_ShouldReturnCreated() throws Exception {
        DeliveryNoteRequest request = DeliveryNoteRequest.builder()
                .date(LocalDateTime.of(2026, 10, 1, 8, 0)).clientId(1).build();
        when(deliveryNoteService.createDeliveryNote(any(DeliveryNoteRequest.class)))
                .thenReturn(DeliveryNoteResponse.builder().id(1).code(6).build());

        mockMvc.perform(post("/api/deliveryNotes").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(6));
    }

    @Test
    @DisplayName("POST /api/deliveryNotes - Debe rechazar un código no positivo")
    void createDeliveryNote_WithNonPositiveCode_ShouldReturnBadRequest() throws Exception {
        DeliveryNoteRequest request = DeliveryNoteRequest.builder()
                .code(-1).date(LocalDateTime.of(2026, 10, 1, 8, 0)).clientId(1).build();

        mockMvc.perform(post("/api/deliveryNotes").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(deliveryNoteService, never()).createDeliveryNote(any());
    }
```

- [ ] **Step 2: Ejecutarlos y ver que fallan**

  Run: `./mvnw test -Dtest=CodeSuggestionTest,DeliveryNoteControllerTest`
  Expected: fallan los dos de servicio (`NOT NULL constraint failed: delivery_notes.code`) y `createDeliveryNote_WithoutCode_ShouldReturnCreated` (400). Pasa el del código no positivo.

- [ ] **Step 3: Implementar**

  - `DeliveryNoteRequest.code`: quitar `@NotNull` y conservar `@Positive`.
  - `createDeliveryNote`: el código es el del request o, si viene nulo, `findNextCode()`. Pasa por `requireUnusedCode` y se fija en la entidad después del mapper.
  - `updateDeliveryNote`: el código es el del request o, si viene nulo, el actual. `requireUnusedCode` solo si cambia, y se fija después de `updateEntity`.

- [ ] **Step 4: Ejecutar la suite**

  Run: `./mvnw test`
  Expected: sin fallos.

- [ ] **Step 5: Documentación**

  - PENDIENTES: quitar "Código sugerido para Cliente y Artículo".
  - PENDIENTES, "Huecos en los tests": quedan pendientes los tests de controlador de facturas, IVA y perfil, los del resto de endpoints de albaranes y los de `*Specification` que no se ejercitan en los tests de servicio.

- [ ] **Step 6: Commit**

```bash
git add selfbill-api/src PENDIENTES.md
git commit -m "assign delivery note code on server when empty"
```

---

## Cierre

- `./mvnw test` en verde y el arranque contra una BD nueva (paso 8 de la tarea 1) funcionando.
- CONCEPTO.md y PENDIENTES.md coherentes entre sí: lo resuelto ha salido de PENDIENTES y las reglas nuevas están en CONCEPTO §3, §4, §5 y §7.
- Lo siguiente en la hoja de ruta del spec (§4) es el spec del PDF real.
