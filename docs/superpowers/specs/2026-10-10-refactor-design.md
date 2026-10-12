# Segunda pasada: refactorización y código sostenible

Fecha: 2026-10-10. Rama: `feature/nucleo-pendientes`, sobre el trabajo del spec
[2026-10-10-pendientes-design.md](2026-10-10-pendientes-design.md). Plan:
[2026-10-10-refactor.md](../plans/2026-10-10-refactor.md).

## 1. Encargo

Revisar `selfbill-api` como analista de refactorización con cuatro criterios:

1. Eliminar los comentarios.
2. Dejar solo los que expliquen algo que el código no explica al leerlo y que no se
   entendería sin CONCEPTO.md.
3. Usar la tecnología adecuada de Spring Boot.
4. Que la arquitectura del proyecto tenga sentido.

El refactor **no cambia el comportamiento**: las reglas de CONCEPTO.md y los tests existentes
se mantienen. Hay dos excepciones, ambas decididas por el usuario: la URL de albaranes y la
forma de los DTOs.

## 2. Decisiones del usuario

| Tema | Decisión |
|---|---|
| Paquetes | Módulos de negocio: `catalog`, `billing`, `settings`, `mail` y `shared` |
| DTOs | Records de Java (requests, responses y filtros) |
| URLs | Kebab-case: `/api/deliveryNotes` pasa a `/api/delivery-notes` |

## 3. Comentarios

**Criterio.** Un comentario sobrevive solo si el porqué no se ve en el código y, sin él,
alguien lo "arreglaría" rompiéndolo. Lo que ya explica CONCEPTO.md no se repite: CLAUDE.md
obliga a leerlo antes de tocar el dominio, y los tests fijan las reglas.

Sobreviven solo estos:

| Dónde | Por qué |
|---|---|
| `Client` y `Article`, sobre la clase | Les falta `@SQLRestriction` a propósito; parece un olvido |
| `InvoiceRepository.findMaxCode` | Es nativa para que cuente las facturas borradas; en JPQL dejaría de hacerlo |
| `application.properties`, ruta de la clave de cifrado | Va fuera de `selfbill.db` por seguridad |
| `application-test.properties`, URL de la BD | `${random.uuid}` porque el `clean` de Flyway no funciona con FKs en SQLite |

Las migraciones de Flyway (`V1`, `V2`) **no se tocan**: cambiar su contenido cambia el checksum
y una base ya migrada dejaría de arrancar.

## 4. Arquitectura objetivo

```
com.luispuchol.selfbill.selfbill_api
├── SelfbillApiApplication
├── shared/       BaseEntity, SoftDeletableEntity, BusinessException, ErrorCode,
│                 GlobalExceptionHandler, RequestLoggingFilter, ClockConfig
├── catalog/      NextCodeResponse
│   ├── client/   Client, VatType, SurchargeType, DeliveryNoteFormat,
│   │             ClientTaxRegimeChanged + controller/service/repo/mapper/spec/DTOs
│   └── article/  Article + controller/service/repo/mapper/spec/DTOs
├── billing/
│   ├── deliverynote/  DeliveryNote, DeliveryNoteLine, DeliveryNoteTaxRegime + ...
│   ├── invoice/       Invoice, InvoiceLine, InvoiceSeries, InvoiceStatus + ...
│   └── document/      PdfService, DocumentSender, DocumentMessages, DocumentType,
│                      PdfFile, PdfResponses
├── settings/
│   ├── taxconfig/     TaxConfig + ...
│   └── profile/       UserProfile, CredentialEncryptionService, CredentialProperties,
│                      SmtpAccount, SmtpProviderDefaults + ...
└── mail/         EmailService, EmailMessage
```

**Dependencias entre módulos, en un solo sentido:**

- `billing` depende de `catalog`, `settings.taxconfig`, `mail` y `shared`.
- `mail` depende de `settings.profile`.
- `catalog` y `settings` dependen solo de `shared`.
- `shared` no depende de nadie.

Dentro de `billing` sus subpaquetes se usan entre sí; albarán y factura forman un mismo
agregado.

**Cómo se rompen las dos dependencias que hoy van al revés:**

- **Catálogo → facturación.** `ClientService` deja de llamar a `DeliveryNoteService`. Publica
  el evento `ClientTaxRegimeChanged`, que `DeliveryNoteService` escucha con `@EventListener`.
  El oyente es síncrono y se ejecuta en la misma transacción, así que el comportamiento no
  cambia.
- **Mail → catálogo y facturación.** `EmailService` pasa a ser genérico
  (`send(EmailMessage)`). La parte de documento (comprobar el email del cliente, asunto,
  cuerpo y adjunto) pasa a `billing.document.DocumentSender`.

**Verificación automática con Spring Modulith 1.4.13** (la línea para Boot 3.5):

- `ApplicationModules.of(...).verify()` en un test.
- Los subpaquetes que otros módulos usan se exponen con `@NamedInterface` en su
  `package-info.java`: `catalog.client`, `catalog.article`, `settings.taxconfig` y
  `settings.profile`.
- Comprobado antes de escribir este spec: Modulith lee clases de Java 25. Con módulos `OPEN`
  no detecta ciclos, de ahí que se usen interfaces con nombre.

## 5. Tecnología

| Ahora | Pasa a | Motivo |
|---|---|---|
| `@Validated` en controladores (AOP) | Validación integrada de Spring 6.1 (`HandlerMethodValidationException`) | Es el mecanismo nativo de Spring MVC; el error de parámetro se informa como `id` y no como `getArticleById.id` |
| id, createdAt, updatedAt y deletedAt repetidos en 8 entidades | `@MappedSuperclass` `BaseEntity` / `SoftDeletableEntity`, solo con getters | Sin duplicación; MapStruct deja de necesitar unos 40 `@Mapping(ignore)` y nadie puede asignar esos campos |
| `@Table(indexes / uniqueConstraints)` | Se eliminan | El esquema es de Flyway; esas anotaciones solo valen para generar DDL |
| `root.get("name")` en specifications | Metamodelo JPA (`Article_.name`) con `hibernate-jpamodelgen` | Los errores salen al compilar y no en ejecución; comprobado que se genera con Java 25 |
| Lombok y MapStruct sin `lombok-mapstruct-binding` | Se añade 0.2.0 | Combinación que exige la documentación de MapStruct |
| DTOs `@Data` y respuestas completadas después del mapper | Records inmutables; el mapper construye la respuesta entera (secciones de factura, contraseña de perfil) | Una sola responsabilidad por capa |
| Ruta de la clave fija en `user.home`, resuelta en el constructor | `@ConfigurationProperties("selfbill.credentials")` + `@ConfigurationPropertiesScan` | Configurable; los tests dejan de escribir en el `~/.selfbill` real |
| Errores que pierden la causa (email, logo) | `BusinessException` con causa; `UncheckedIOException` | Errores diagnosticables |
| El filtro de log reimplementa los textos de estado y mide con `Instant` | `HttpStatus` y `System.nanoTime()` | API estándar y reloj monótono |

**Dependencias nuevas (todas comprobadas en Maven Central y con Java 25):**

- `spring-modulith-api` en compilación y `spring-modulith-starter-test` en test, ambas vía
  `spring-modulith-bom` 1.4.13.
- `lombok-mapstruct-binding` 0.2.0 como procesador.
- `hibernate-jpamodelgen` `${hibernate.version}` como procesador.

## 6. Fuera de alcance

- **La forma de modelar el dominio.** Se mantienen los servicios con entidades anémicas, que
  es el estilo del proyecto. Pasar a un modelo rico es otro debate.
- **Mensajes de error y de validación internacionalizados.**
- **Cualquier funcionalidad de PENDIENTES.md.**
