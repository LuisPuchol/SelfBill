# Pendientes: análisis, diseño y hoja de ruta

Fecha: 2026-10-10. Base: [CONCEPTO.md](../../../CONCEPTO.md) y [PENDIENTES.md](../../../PENDIENTES.md)
en su versión de esta fecha. Plan de ejecución de las fases 0–3:
[2026-10-10-nucleo-pendientes.md](../plans/2026-10-10-nucleo-pendientes.md).

---

## 1. Objetivo y supuestos

Convertir PENDIENTES.md en trabajo ejecutable sin romper las bases de CONCEPTO.md.

Supuestos (no dichos explícitamente; corregir si no valen):

- Alcance: solo backend (`selfbill-api`). El front es todavía el esqueleto de SvelteKit.
- La `selfbill.db` local es desechable. Contiene datos de prueba y ya no coincide con el
  código: no tiene la columna `series` y su `invoices.code` es `unique` global (§2.3).

---

## 2. Análisis

### 2.1. Verifactu está más cerca de lo que dice PENDIENTES

El RDL 15/2025 fija la obligatoriedad el **1-ene-2027 para contribuyentes del Impuesto sobre
Sociedades** y el **1-jul-2027 para el resto**, incluidos los autónomos. No se sabe aún cuál
aplica al titular, así que se planifica con el caso peor: 1-ene-2027, a 12 semanas de esta
fecha. Ese día se cierra la vía de corrección actual (borrar la factura emitida).

Consecuencias para el orden:

- El PDF real pasa a ser prerrequisito de Verifactu: la factura lleva QR.
- El front está sin empezar y está en el camino crítico para usar SelfBill en producción.
  No figura en PENDIENTES; se señala aquí como riesgo.

### 2.2. Lo de "§1 Lógica" es un solo problema

`freezeTaxRates` copia al albarán los tipos del `TaxConfig` **sin aplicar el régimen del
cliente**: un cliente sin IVA o sin recargo guarda igualmente 4 % y 0,5 %. CONCEPTO §3 dice
que el albarán guarda "los tipos de IVA y de recargo aplicados" y que el PDF valorado "lee
lo que el albarán congeló, sin consultar al cliente"; hoy tendría que consultarlo.

A la vez, `applyFinancials()` lee el `TaxConfig` vigente y el régimen vivo del cliente, de
ahí los dos puntos de PENDIENTES (IVA por fecha de operación y borradores desactualizados).
La solución común es que el albarán congele el tipo **aplicado** y que la factura salga
solo de sus albaranes (§5.3).

### 2.3. El esquema no es el que el código cree

El DDL que genera el dialecto de SQLite de Hibernate:

- No crea **ninguna FK**.
- No crea el `unique (series, code)` de `invoices`. La unicidad del número de factura solo
  la sostiene `findMaxCode`; el comentario del repositorio que la atribuye a la restricción
  es inexacto.
- Pone `CHECK` en cada columna enum, que SQLite no permite alterar sin reconstruir la tabla.

Con `ddl-auto=update`, la base local ya ha divergido del modelo (§1).

### 2.4. El bug de borrado afecta también a `Article`

`DeliveryNoteMapper` lee `article.name` en cada línea. Borrar un artículo usado deja sus
albaranes en 500, igual que con el cliente.

Además, **los albaranes no guardan copia ni del cliente ni del artículo**: solo `Invoice`
(datos fiscales del cliente, al emitir) e `InvoiceLine` (artículo y albarán) tienen
snapshot. Para que un documento siga legible tras borrar su cliente o su artículo, tiene
que poder seguir la relación hasta la fila borrada (§5.2).

### 2.5. Otros huecos de §6 (inmutabilidad) que no están en PENDIENTES

Afectan a lo que se imprime, así que se resuelven en el sub-proyecto de PDF (§7.1):

- El **albarán** imprime nombre y dirección del cliente sin copia propia: editar un cliente
  reescribe la reimpresión de todos sus albaranes.
- La **factura** imprime los datos del titular (`UserProfile`) sin copia propia: editar el
  perfil reescribe todas las facturas emitidas. Verifactu exige además el NIF del emisor en
  el registro.

---

## 3. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Fecha Verifactu | Desconocida; se planifica con 1-ene-2027 | Usuario |
| Borrar cliente/artículo con documentos | Permitido. Los documentos siguen leyendo la fila borrada; restaurar es vaciar `deleted_at` | Usuario |
| Código de cliente/artículo/albarán borrado | Se libera. Un solo código **vivo** a la vez | Usuario |
| Números de factura | No se liberan nunca, ni de facturas borradas (CONCEPTO §5, sin cambios) | CONCEPTO |
| IVA mixto en una factura | No ocurre en el negocio. Se protege con un 409 en lugar de calcular mal en silencio | Usuario + diseño |
| Tipo del albarán | El aplicado: 0 si el cliente no lleva IVA o no lleva recargo | Diseño (§5.3) |
| Cambio de régimen del cliente | Se propaga a sus albaranes editables y a sus borradores | Diseño (§5.3) |
| Cambio de `TaxConfig` | No afecta a nada existente | CONCEPTO §4 |
| FKs | Se declaran en la migración inicial y se activan (`foreign_keys=true`) | Diseño (§5.1) |
| CHECK de enums | No se declaran | Diseño (§5.1) |

---

## 4. Hoja de ruta

| # | Fase | Estado de diseño |
|---|---|---|
| 0 | Flyway y esquema real | Diseñada y planificada |
| 1 | Borrado libre de catálogo | Diseñada y planificada |
| 2 | IVA del albarán, factura como proyección | Diseñada y planificada |
| 3 | API pequeña: filtro, códigos sugeridos | Diseñada y planificada |
| 4 | PDF real | Spec propio; decisiones abiertas en §7.1 |
| 5 | Verifactu | Spec propio tras consultar a la gestoría (§7.2) |
| 6 | Exportación CSV, backup y estadísticas | Diseñada (§6.1); plan al llegar |
| 7 | Seguimiento de cobro | Diseñada (§6.2); plan al llegar |
| — | Trazabilidad | Bloqueada: falta definir el dominio (§7.3) |

Las fases 0–3 se planifican ya en detalle porque no dependen de nada externo y todo lo
demás se construye encima. Las siguientes se planifican al llegar: su plan lleva código
exacto que depende de cómo queden estas.

Si se confirma el 1-jul-2027, las fases 6 y 7 pueden adelantarse a la 4. Del bloque 6, el
CSV para la gestoría es lo primero que hace falta en producción.

---

## 5. Diseño de las fases planificadas

### 5.1. Fase 0: Flyway y esquema real

- Dependencia `org.flywaydb:flyway-core` (versión gestionada por Boot 3.5.7: 11.7.2, que
  incluye SQLite).
- `V1__init.sql`: el esquema actual escrito a mano a partir del DDL de Hibernate, con:
  - FKs en todas las relaciones (`references`).
  - Índices únicos **parciales** en `code` de `articles`, `clients` y `delivery_notes`:
    `where deleted_at is null`. Un código borrado queda libre.
  - Índice único `(series, code)` en `invoices`, sin condición: los números de factura
    borrados siguen ocupados. Los borradores tienen `code` nulo y SQLite permite varios.
  - Columnas enum sin `CHECK`.
- `ddl-auto=validate`; URL con `?foreign_keys=true`.
- Tests: base de datos nueva en cada ejecución (`${random.uuid}` en la URL), migrada desde
  cero. Se descarta `flyway.clean()`: en SQLite borra las tablas sin desactivar las FKs y
  falla si quedan datos.
- La `selfbill.db` local se renombra a `selfbill.db.bak` y se regenera.

Comprobado antes de escribir este spec: Hibernate valida ese esquema sin errores, los tests
del ciclo de factura pasan contra él con las FKs activas, y SQLite rechaza una FK rota.

Con el índice parcial queda resuelto el bug "el código de un borrado da un 500": los
`existsByCode` actuales ya ignoran las filas borradas.

### 5.2. Fase 1: borrado libre de catálogo

- `Client` y `Article` dejan de llevar `@SQLRestriction` (conservan `@SQLDelete`). Así los
  documentos cargan la fila aunque esté borrada.
- El catálogo filtra explícitamente: los métodos del repositorio pasan a
  `...AndDeletedAtIsNull` y las `Specification` añaden `deletedAt is null`.
- Toda búsqueda de cliente o artículo **para operar** (crear o editar albarán, crear o
  editar factura, endpoints de catálogo) usa la versión filtrada: no se puede crear un
  documento nuevo con un cliente o artículo borrado.
- Leer un documento existente nunca filtra: sigue la relación.
- `DeliveryNote`, `Invoice` y sus líneas conservan `@SQLRestriction`. Ninguna de esas
  relaciones queda colgando: un albarán facturado no se puede borrar, y borrar una factura
  desvincula antes sus albaranes.

Comportamientos que se aceptan:

- Los albaranes pendientes de un cliente borrado no se pueden facturar hasta restaurarlo.
- Un borrador de un cliente borrado se puede emitir: emitir no busca el cliente en el
  catálogo, lo lee de la factura.
- Restaurar a mano una fila cuyo código ya está vivo choca con el índice parcial: hay que
  cambiarle el código (decisión del usuario).

### 5.3. Fase 2: IVA del albarán y factura como proyección

**Albarán.** `applyTaxRegime(albarán, cliente)` fija:

- `vatPercentage` = IVA de `TaxConfig` si el cliente es `WITH_VAT`, si no 0.
- `surchargePercentage` = recargo de `TaxConfig` si el cliente es `WITH_SURCHARGE`, si no 0.

Se aplica al crear el albarán y al cambiarlo de cliente (igual que hoy el formato). Editar
un albarán sin cambiar de cliente no refresca los tipos.

**Cambio de régimen del cliente.** Si `updateClient` cambia `vatType` o `surchargeType`, se
reaplica `applyTaxRegime` a los albaranes **editables** de ese cliente (sin factura o con
factura en borrador) y se recalculan sus borradores. Los albaranes de facturas emitidas no
se tocan. El caso habitual es corregir un cliente mal dado de alta, y así la corrección
llega a lo pendiente.

**Factura.** `applyFinancials` deja de leer `TaxConfig` y el régimen del cliente:

- Tipos: los de sus albaranes. Si difieren entre sí → 409 `INVOICE_MIXED_TAX_RATES`.
- Importes: base = suma de totales de albarán; cuotas = base × tipo, redondeo a 2
  decimales `HALF_UP`, como hoy.
- La serie sigue saliendo del `vatType` actual del cliente. Con la propagación, siempre
  coincide con los tipos de sus albaranes editables.

**Emitir** recalcula la factura antes de numerar, como red de seguridad.

**Límite aceptado (se anota en PENDIENTES).** `TaxConfig` no tiene histórico. Un albarán
fechado antes de un cambio de tipo pero creado después recibe el tipo nuevo. Reaplicar el
régimen usa el tipo vigente ese día.

**Enmienda (revisión final).** La premisa "con la propagación, siempre coincide" no se
sostenía en cuanto se borra una factura emitida: sus albaranes quedaban libres con los
tipos del régimen anterior del cliente, y refacturarlos daba una factura `S-` con IVA o
una general al 0 %. Regla definitiva: el albarán guarda también el régimen (`vatType`,
`surchargeType`) con el que se calcularon sus tipos y, mientras no esté en una factura
emitida, sigue el régimen actual de su cliente: al cambiarlo de cliente, al cambiar el
régimen del cliente, al borrarse su factura emitida y al recalcular un borrador. Se
reaplica componente a componente: el que no cambia de régimen conserva el tipo de la fecha
de la operación y el que cambia toma el vigente, o 0 (migración `V2`).

### 5.4. Fase 3: API pequeña

- `DeliveryNoteFilter.invoiced` (`Boolean`): `false` devuelve albaranes sin factura y
  `true` los que tienen factura (borrador o emitida). Junto con `clientId` da los
  pendientes de un cliente.
- `GET /api/clients/next-code` y `GET /api/articles/next-code` → `{"code": n}`, con
  `n = max(code de filas vivas) + 1` (1 si no hay ninguna). Es coherente con los códigos
  liberados: si se borra el último, se sugiere de nuevo.
- Albarán: `code` deja de ser obligatorio en el request. Si viene vacío al crear, el
  servidor asigna `max(code vivo) + 1`; si viene vacío al editar, conserva el que tenía.
- Tests de controlador de albaranes (`DeliveryNoteControllerTest`), que hoy no existen.

### 5.5. Documentación

Cada fase actualiza CONCEPTO.md y PENDIENTES.md en el mismo commit:

- §3 y §4: el albarán guarda tipos aplicados; la factura usa los de sus albaranes; límite
  del `TaxConfig` sin histórico.
- §7: `Client` y `Article` sin `@SQLRestriction` y por qué; códigos liberados salvo
  números de factura; FKs.
- PENDIENTES: quitar lo resuelto; añadir los huecos de §2.5 al PDF; añadir el límite de
  §5.3.

---

## 6. Diseño de las fases sin plan todavía

### 6.1. Exportación, backup y estadísticas

- **CSV para la gestoría.** `GET /api/exports/invoices.csv?from=&to=` por fecha de
  emisión, ambas inclusive. Solo facturas emitidas no borradas, ordenadas por serie y
  número. Columnas: Serie; Número; Fecha; NIF; Cliente; Base imponible; % IVA; Cuota IVA;
  % Recargo; Cuota recargo; Total (más Vencimiento y Cobro si la fase 7 está hecha). El
  formato es el de Excel en español: UTF-8 con BOM, separador `;`, coma decimal y fechas
  `dd/MM/yyyy`. Solo se usan datos congelados.
- **Backup.** `GET /api/backup` hace `VACUUM INTO` a un fichero temporal, lo devuelve como
  `selfbill-backup-YYYYMMDD-HHmmss.db` y lo borra. La restauración es manual y
  documentada: parar la app y sustituir el fichero.
- **Estadísticas.** `GET /api/stats/revenue?from=&to=&groupBy=MONTH|CLIENT` (número de
  facturas, base, cuotas, total) y `GET /api/stats/receivables` (pendiente y vencido, que
  requiere la fase 7). Se calculan en Java sobre las facturas emitidas del rango: son
  cientos al año, y así se evita depender de cómo guarda SQLite las fechas.

### 6.2. Seguimiento de cobro

- `Client.paymentTermDays` (entero, no nulo, por defecto 0 = al contado). Migración `V2`.
- `Invoice.dueDate` (fecha), fijada al emitir como fecha de emisión + `paymentTermDays`
  del cliente. Se imprime, así que queda congelada (§6).
- `Invoice.paidAt` (fecha, nulable). El cobro no es contenido de la factura: registrarlo
  en una emitida no la edita.
- `PUT /api/invoices/{id}/payment {"paidAt": "YYYY-MM-DD"}` y
  `DELETE /api/invoices/{id}/payment`. Solo sobre facturas emitidas (409 en borrador);
  `paidAt` no puede ser futura.
- `paymentStatus` derivado, no guardado (mismo criterio que "rectificada", CONCEPTO §5):
  `PAID` si hay `paidAt`; `OVERDUE` si `dueDate < hoy`; si no, `PENDING`. Nulo en borradores.
  Usable también como filtro de `InvoiceFilter`.
- Fuera, a PENDIENTES: cobros parciales, forma de pago, remesas.

---

## 7. Sub-proyectos con spec propio

### 7.1. PDF real

Decisiones abiertas:

- Motor: plantillas HTML con Thymeleaf (gestionado por Boot) y un renderizador
  HTML→PDF, o generación programática con OpenPDF. Recomendación previa: HTML, porque una
  plantilla de factura se mantiene mejor como maquetación que como código.
- Snapshot del cliente en el albarán y del titular en la factura (§2.5).
- Tres plantillas: factura; albarán simple; albarán valorado con cabecera **ALBARÁN** y la
  leyenda de CONCEPTO §3.
- Hueco para el QR y la leyenda de Verifactu.

### 7.2. Verifactu

Preguntas para la gestoría antes del spec:

- Fecha que aplica al titular (sociedad o persona física).
- Modalidad: VERI\*FACTU (envío a la AEAT) o registro local firmado. Cambia la arquitectura.
- Serie propia para rectificativas y tipo (R1–R5) habitual (CONCEPTO §9.1).
- Obligaciones como productor de un software desarrollado para uso propio.

El modelo ya está preparado (CONCEPTO §9). La fase 0 deja además las migraciones
necesarias para añadir la tabla de registros sin regenerar la base.

### 7.3. Trazabilidad

Bloqueada hasta definir qué representa cada uno de los tres códigos y qué consultas deben
responderse (CONCEPTO §10).

---

## 8. Estrategia de pruebas

- Pruebas de integración de servicio (`@SpringBootTest`, perfil `test`, SQLite en
  `target/`) para todo lo que toca dominio. Los helpers de `InvoiceLifecycleTest` se
  extraen a una clase base común.
- `@WebMvcTest` para los endpoints nuevos o modificados.
- TDD: cada comportamiento de este spec entra con su test en rojo antes del código.
- Línea base del 2026-10-10: 43 tests en verde.
