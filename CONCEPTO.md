# SelfBill — modelo de dominio

Define el modelo, sus reglas y los motivos que las sostienen. La lista de trabajo está
en [PENDIENTES.md](PENDIENTES.md).

Antes de modificar una entidad, consultar la sección correspondiente. Varias
simplificaciones aparentes responden a un motivo concreto y romperlas tiene efectos
reales.

---

## 1. Ámbito

Software de facturación autohospedado y monousuario para un autónomo con empresa propia.
Se ejecuta íntegramente en la máquina del usuario, con base de datos SQLite en fichero y
sin necesidad de conexión a internet. No es SaaS ni multi-tenant, y no tiene login: existe
un único `UserProfile` con los datos fiscales y las credenciales de correo del titular.

El negocio de referencia es un mayorista de frutas y verduras. Ese sector condiciona el
modelo de precios, el de IVA y el de trazabilidad.

Sustituye a un software anterior en uso durante veinte años, añadiendo mantenimiento,
envío de facturas por correo y, más adelante, Verifactu.

El listón para cualquier decisión sobre seguridad, exposición de datos, autenticación,
escalabilidad o concurrencia es el de una aplicación de escritorio de un solo usuario. No
se introducen patrones multi-tenant ni de escala sin una necesidad concreta.

---

## 2. Flujo: albarán → factura

1. Se crea un albarán por cada entrega a un cliente, con líneas de artículos que llevan
   cantidad, precio y códigos de trazabilidad.
2. A fin de mes se agrupan los albaranes pendientes de un cliente y se factura.
3. La factura agrupa N albaranes de un mismo cliente y se presenta seccionada: una
   sección por albarán, con sus líneas dentro.

Corresponde a la figura de **factura recapitulativa**: una factura que recoge varias
operaciones del mismo mes natural para el mismo cliente.

### Reglas

- Una factura pertenece a un solo cliente. No se mezclan clientes.
- Un albarán pertenece a una factura vigente o a ninguna. Nunca a dos a la vez.
- La jerarquía tiene dos niveles: factura → sección por albarán → líneas. No existen
  facturas dentro de facturas; ese objeto carece de sentido fiscal.
- Una factura no tiene datos propios editables. Es la proyección de los albaranes que la
  componen. Lo único editable es qué albaranes la forman.

---

## 3. Formatos de albarán

Existen dos formatos de impresión:

- **Simple**: líneas y totales, sin impuestos.
- **Valorado**: incluye el cálculo de impuestos y tiene el mismo aspecto que una factura.

El formato valorado sirve a los clientes que pagan al día contra el albarán en lugar de
esperar a la factura de fin de mes.

**Un albarán valorado no es una factura.** No lleva número de serie fiscal, no se
registra, no genera asiento y no es deducible. Es un documento comercial. La plantilla
lleva la cabecera **ALBARÁN**, nunca "Factura Nº", y la leyenda *"Documento sin valor
fiscal. No válido como factura."*

El cliente en formato valorado se factura igualmente a fin de mes, agrupado, como el
resto. El formato solo evita imprimirle veinte facturas al mes; no altera cómo se
factura. Es un formato de impresión, no un modo de facturación.

### Dónde reside

- `Client` guarda la **preferencia por defecto** del cliente.
- `DeliveryNote` guarda el **valor efectivo** con el que ese albarán se emitió, junto con
  los tipos de IVA y de recargo aplicados.
  Los tipos son los **aplicados**, con el régimen del cliente ya resuelto: el IVA es 0 si el
  cliente no lleva IVA, y el recargo es 0 si no lleva recargo. Se fijan al crear el albarán y
  al cambiarlo de cliente.

Reimprimir un albarán antiguo reproduce así el documento exacto que recibió el cliente,
aunque después haya cambiado su preferencia o el tipo de IVA. El generador de PDF lee lo
que el albarán congeló, sin consultar al cliente.

---

## 4. IVA

`TaxConfig` es un singleton global con un único tipo de IVA y un único recargo de
equivalencia.

El negocio opera en un solo sector y todos sus productos llevan el mismo IVA. Lo que
cambia es ese tipo a lo largo del tiempo: el IVA de la alimentación pasó por 0%, 5%, 2% y
4% en pocos años. Un tipo por artículo obligaría a modificar el catálogo entero en cada
cambio normativo; un tipo global se cambia en un sitio.

**`Article` no tiene tipo de IVA.** Si en el futuro se necesita IVA por producto, se añade
al artículo manteniendo el global como valor por defecto.

El tipo aplicable a una operación es el vigente en la **fecha de la operación**, no el del
día en que se factura. El albarán congela el tipo con el que se valoró, y la factura usa
el de sus albaranes. Todos sus albaranes deben llevar el mismo tipo; si no, la factura se
rechaza (409). El negocio no mezcla tipos en una factura, así que no hay desglose (§8).

### Precio

`Article` tiene solo código y nombre: **no tiene precio**. En frutas y verduras el precio
es de mercado y varía a diario, así que reside en la línea del albarán, que es donde queda
fijado en el momento de la entrega.

---

## 5. Ciclo de vida de la factura

| Estado | Significado |
|---|---|
| `DRAFT` | Borrador. Mutable. Sin número asignado. No enviable como factura. |
| `ISSUED` | Emitida. Número asignado, fecha fijada, contenido congelado. |

Que una factura haya sido rectificada no es un estado propio: es consecuencia de que otra
factura le apunte, y se modela con el FK `rectifies` (§9.1). Guardarlo como estado
exigiría mantenerlo a mano y acabaría desincronizándose.

Una factura se crea en `DRAFT` y pasa a `ISSUED` al emitirla. Mientras esté en borrador
admite cambios sin restricción. Con Verifactu, emitir implica además registrar en la AEAT,
lo que hace el paso irreversible. Emitir recalcula la factura desde sus albaranes antes de
numerarla.

### Numeración

**El número se asigna al emitir, nunca al crear.** La serie debe quedar correlativa y sin
huecos, y un borrador descartado no puede haber consumido un número.

Hay dos series, cada una con su propio contador: la general y la de clientes sin IVA, que
se imprime con el prefijo `S-` (S-1, S-2...). Una factura de una serie no consume número de
la otra. La serie se deduce del tipo de IVA del cliente, queda guardada en la factura
(`InvoiceSeries`) y el número es único dentro de su serie.

Queda grabada en cada fila emitida: cambiarla más tarde obliga a revisar la asunción bajo
la que se emitieron todas las facturas anteriores.

Borrar una factura emitida, posible hasta Verifactu, no libera su número: la fila borrada
lo sigue ocupando y su serie queda con un hueco. Reutilizarlo obligaría a renumerar o a
romper la unicidad, y ninguna de las dos cosas es aceptable en un documento ya entregado.

### Reglas

- Editar una factura significa cambiar qué albaranes la componen, y solo en `DRAFT`.
- Una factura emitida no se edita.
- Un albarán es editable mientras su factura esté en `DRAFT`. Se bloquea cuando pasa a
  `ISSUED`.
- Borrar una factura desvincula sus albaranes, que vuelven a quedar disponibles.
- Hasta Verifactu, una factura emitida puede borrarse; es la vía de corrección
  disponible. Con Verifactu deja de estarlo y la sustituye la rectificativa (§9.1).

---

## 6. Inmutabilidad y snapshots

Un documento emitido debe poder reimprimirse años después idéntico a como se entregó. Una
factura no puede depender de datos que sigan cambiando en otras tablas.

**Regla general: si un dato sale impreso en un documento emitido, ese documento guarda su
propia copia.**

`InvoiceLine` congela `articleName`, `articleCode`, `deliveryNoteCode` y
`deliveryNoteDate`, y mantiene los FK como enlace opcional.

Los demás snapshots que el modelo exige:

- **Datos fiscales del cliente en la factura**: nombre fiscal, NIF y dirección. Sin ellos,
  editar un cliente reescribe retroactivamente el PDF de todas sus facturas anteriores.
- **Tipo de IVA y formato en el albarán**: §3 y §4.

---

## 7. Borrado

Todas las entidades del dominio usan borrado lógico (`@SQLDelete`). Los documentos añaden `@SQLRestriction`; el catálogo (`Client`, `Article`) no, y filtra él mismo las filas borradas (ver abajo).
Nada se elimina físicamente: el histórico es el producto.

- `Client` y `Article` no llevan `@SQLRestriction`. Un documento sigue leyendo su cliente y sus artículos aunque estén borrados, porque el albarán no guarda copia de ellos, y restaurar una fila es vaciar `deleted_at`. El catálogo filtra las filas borradas en sus consultas, y no se puede crear un documento nuevo con un cliente o artículo borrado.
- Los documentos (albarán, factura y sus líneas) sí llevan `@SQLRestriction`. Ninguna relación viva apunta a un documento borrado: un albarán facturado no se borra, y borrar una factura desvincula antes sus albaranes.
- Una fila borrada libera su código: el índice único de `code` es parcial (`WHERE deleted_at IS NULL`), así que solo puede haber una fila viva por código. Restaurar una fila cuyo código ya está vivo obliga a cambiárselo. Los números de factura son la excepción: no se liberan nunca (§5).
- El esquema lo gestiona Flyway (`db/migration`) y Hibernate solo lo valida. Las FKs están declaradas y activas (`foreign_keys=true`).
- Hasta Verifactu, una factura se puede borrar en cualquier estado (§5).

---

## 8. Alcance

El criterio es cubrir el flujo que el negocio necesita y proteger solo lo que resulta caro
de cambiar después.

Fuera de alcance hasta que exista una necesidad real, en la práctica hasta Verifactu:

- Estado `ANNULLED` y bloqueo del borrado de facturas emitidas.
- Facturas rectificativas, `invoiceType` y FK `rectifies`.
- Hash encadenado y envío a la AEAT.
- Desglose por varios tipos de IVA.
- IVA o precio por artículo.

Ante la duda "¿y si el usuario quisiera…?", la respuesta por defecto es no, y se anota en
PENDIENTES.md.

---

## 9. Verifactu

No implementado. Campos previstos en `Invoice`:

| Campo | Tipo | Uso |
|---|---|---|
| `invoiceType` | enum `InvoiceType` | `F1` normal; `R1`–`R5` rectificativa (§9.1) |
| `rectifies` | FK a `Invoice`, nulable | Factura que esta rectifica (§9.1) |
| `previousInvoice` | FK a `Invoice` | Registro anterior de la cadena, para el hash encadenado |
| `verifactuHash` | texto (SHA-256 hex) | Hash del registro |
| `verifactuTimestamp` | fecha y hora | Momento del registro |
| `verifactuStatus` | enum | `PENDING`, `SENT`, `ACCEPTED`, `REJECTED` |

La estructura del modelo lo contempla: momento de asignación del número, inmutabilidad de
lo emitido y snapshots. El hash encadenado, el tipo de factura y el envío a la AEAT son
aditivos y no obligan a rehacer el modelo.

El modelo tendrá que representar:

- Serie y número de cada factura, y el encadenamiento de hash. La cadena es **una sola
  por sistema de facturación y obligado tributario**, sea cual sea la serie: cada registro
  enlaza con el anterior que generó el sistema, aunque sea de otra serie
  ([FAQ de la AEAT](https://sede.agenciatributaria.gob.es/Sede/iva/sistemas-informaticos-facturacion-verifactu/preguntas-frecuentes/sistemas-verifactu.html)).
  Además, la cadena enlaza registros de facturación (altas y anulaciones), no facturas: es
  probable que pida una tabla propia de registros en lugar de un FK entre facturas.
- Tipo de factura y, en rectificativas, referencia a la original.
- Desglose por tipo impositivo. Los campos escalares de IVA actuales bastan con un tipo
  único, pero no representan una factura con tipos mezclados.
- Motivo en las operaciones sin IVA repercutido.
- Estado de envío a la AEAT.

El calendario de obligatoriedad aplicable al usuario debe verificarse antes de planificar:
las fechas se han movido varias veces.

---

### 9.1. Corrección de una factura emitida

Con Verifactu, una factura emitida está registrada en la AEAT: no se puede borrar, ni
renumerar, ni alterar. La corrección se hace emitiendo una **factura rectificativa**, y la
original permanece en los libros.

Escenario de referencia: la factura 45 agrupa los albaranes 100, 101 y 102, y el 101 lleva
50 kg donde debía llevar 15.

#### Flujo

1. **Facturas → factura 45 → "Rectificar".**
2. Se crea un borrador de rectificativa vinculado a la 45, que arranca con **el mismo
   conjunto de albaranes**. Esos albaranes quedan desbloqueados mientras el borrador
   exista.
3. Se corrigen los albaranes necesarios desde la pantalla de albaranes. El borrador no se
   edita a mano: se recalcula solo, porque una factura es la proyección de sus albaranes
   (§2).
4. Opcionalmente se ajusta el conjunto: quitar un albarán que no debía estar (vuelve a
   quedar libre) o añadir uno que se quedó fuera.
5. **Emitir.** Número nuevo, `rectifies = 45`, contenido congelado, registro enviado a la
   AEAT y albaranes rebloqueados.

Los valores originales no se pierden: siguen congelados en las `InvoiceLine` de la 45, que
documentan lo que realmente se facturó y se cobró.

#### Por sustitución

La rectificativa expresa los importes correctos completos, referenciando lo previamente
facturado, y se genera proyectando los albaranes corregidos igual que cualquier otra
factura. Lleva **todos** los albaranes de la original, no solo el corregido: sustituye a
la factura entera.

La alternativa —rectificativa por diferencias, que solo recoge el delta— produce líneas de
ajuste que no se corresponden con ningún albarán, y rompe el invariante de que una factura
es la proyección de sus albaranes.

#### Desbloqueo de albaranes

Se desbloquean **todos los albaranes de la factura rectificada**, no solo el que se sabe
erróneo: rara vez se conoce de antemano cuál falla, y es habitual encontrar un segundo
error al revisar. El bloqueo vuelve al emitir la rectificativa.

Es seguro porque la factura 45 conserva sus propias copias congeladas. Sin el patrón de
snapshots de §6, desbloquear corrompería el histórico.

Los albaranes quedan **retenidos en el borrador**: editables, pero no libres. No reaparecen
en la pestaña de facturar. Liberarlos al pool de pendientes provocaría que:

- Se mezclasen con los albaranes pendientes del mes en curso.
- Hubiera que reseleccionarlos a mano, con riesgo de omitir alguno, cuando la sustitución
  exige exactamente el mismo conjunto.
- Se perdiera el vínculo `rectifies`.
- Al abandonar a medias, la factura 45 quedase emitida y registrada sin ningún albarán
  detrás.

Al abandonar la rectificación se borra el borrador y los albaranes vuelven a la factura
rectificada, rebloqueándose.

#### Tipo de factura: campo, no herencia

```java
private InvoiceType invoiceType;   // F1 normal; R1–R5 rectificativa
private Invoice rectifies;         // null en facturas normales
```

Un enum y un FK autoreferenciado dentro de `Invoice`. Ni entidad aparte ni herencia JPA:

- Una rectificativa **es** una factura: número de serie, cliente, líneas, totales, PDF,
  correo y registro en la AEAT. Separarla duplicaría repositorio, specification, generador
  de PDF y listado, para luego volver a unirlos.
- Los dos tipos tienen estructura idéntica; lo único que cambia es que uno apunta a otra
  factura. `SINGLE_TABLE` daría una columna discriminadora equivalente al enum pero con
  más restricciones (el tipo no se puede cambiar tras crear la entidad, consultas
  polimórficas más incómodas, mappers más complicados); `JOINED` añadiría una tabla y
  joins sin contrapartida.
- Verifactu modela el tipo como un campo (`TipoFactura`) dentro del mismo registro.
  Convertirlo en jerarquía de clases obligaría a destraducirlo al enviar.

Un único enum con `F1` y `R1`–`R5`, no un booleano más un campo de causa, con
`isRectificative()` para legibilidad. `F2` (factura simplificada) no aplica: aquí siempre
se factura a empresas identificadas.

`InvoiceType` y `rectifies` no deben confundirse con `previousInvoice`, que apunta al
**registro anterior de la cadena** para encadenar el hash y existe en todas las emitidas.

La relación inversa no se guarda: "rectificada por la 46" se consulta con
`findByRectifiesId`. Para etiquetar un listado, una consulta por página, nunca una por
fila.

**Invariantes:**

- `rectifies != null` ⟺ `invoiceType.isRectificative()`.
- Solo se rectifica una factura emitida. Una en borrador se edita.
- Una factura tiene como mucho una rectificativa en borrador a la vez.
- Las rectificativas encadenan: la 47 puede rectificar a la 46, que rectificaba a la 45.
  La vigente es la última de la cadena.

#### Requisito previo

Durante el borrador, el FK del albarán apunta a la rectificativa, que es lo que lo hace
editable bajo la regla "editable si su factura está en borrador". Al moverse ese FK, la
factura 45 deja de tener `deliveryNotes`.

Por eso las secciones de una factura se construyen desde sus `InvoiceLine` (§6) y no desde
la relación viva con los albaranes: en caso contrario la factura original se queda sin
secciones y su PDF sale vacío. Es requisito previo de las rectificativas.

#### Pendiente de definir

- **Serie propia para las rectificativas.** Es práctica habitual numerarlas aparte (serie
  R). Afecta a la numeración, no al encadenamiento, que es único para todas las series.
- **Tipo de rectificativa aplicable (R1–R5).** Para un error de cantidad o precio suele
  ser R1 o R4 según la clasificación de la causa. Debe confirmarse con la gestoría.
- **Tratamiento si la factura errónea ya se cobró.** Es conciliación de cobros, no
  facturación, pero conecta con el seguimiento de cobro pendiente.

> Lo normativo de esta sección debe contrastarse antes de implementarse. Está recogido
> para saber qué tocar y por dónde empezar, no como fuente de verdad fiscal.

---

## 10. Trazabilidad

`DeliveryNoteLine` tiene `trazabilityCode1/2/3`, el primero obligatorio, indexados, y
se copian a `InvoiceLine`. Son tres enteros sueltos: no hay entidad ni catálogo detrás, ni
consulta por lote.

Queda por definir qué representa cada uno y qué debe poder responderse, típicamente "dado
un lote, qué clientes lo recibieron". Los índices están puestos con esa finalidad.

---

## 11. Glosario

El código está en inglés y el dominio en español.

| Dominio | Código |
|---|---|
| Albarán | `DeliveryNote` |
| Línea de albarán | `DeliveryNoteLine` |
| Factura | `Invoice` |
| Línea de factura | `InvoiceLine` |
| Factura recapitulativa | una `Invoice` con varios `DeliveryNote` |
| Serie de facturación | `InvoiceSeries` |
| Albarán valorado | albarán impreso con impuestos |
| Recargo de equivalencia | `surcharge` |
| Datos del titular | `UserProfile` |
| Configuración de impuestos | `TaxConfig` |
