# Pendientes

Lista de trabajo. Las reglas del modelo y sus motivos están en [CONCEPTO.md](CONCEPTO.md).

---

## 1. Lógica

### Filtro de albaranes sin facturar
El front necesita pedir los albaranes pendientes de un cliente y `DeliveryNoteFilter` no
lo permite. Hoy habría que traerlos todos y filtrar en el cliente por `invoiceId == null`.

### IVA por fecha de operación
El albarán ya congela su tipo de IVA y de recargo al crearse, pero `applyFinancials()`
sigue leyendo el `TaxConfig` global en el momento de facturar. Debe usar el de sus
albaranes. Si una factura junta albaranes con tipos distintos, los campos escalares de
`Invoice` no lo representan (desglose, CONCEPTO.md §8).

### Un borrador no se entera de los cambios en su cliente ni en el IVA
Los importes y la serie de un borrador se calculan al crearlo, al cambiar sus albaranes y
al editar uno de ellos. Si mientras está abierto cambia el tipo de IVA o de recargo del
cliente, o el `TaxConfig`, el borrador conserva los valores viejos y se emite así; puede
incluso salir en la serie equivocada. Opciones: recalcular al emitir, o recalcular los
borradores del cliente al editarlo. Conviene resolverlo junto con el punto anterior.

---

## 2. Bugs conocidos

### Borrar un cliente con albaranes o facturas los deja inservibles
`deleteClient` no comprueba nada. `DeliveryNote.client` e `Invoice.client` son FK no
nulables y `Client` tiene `@SQLRestriction`, así que tras el borrado esos documentos no
pueden cargar su cliente y lanzan `EntityNotFoundException` (500). Comprobado: falla el
detalle de sus albaranes y también el listado completo de albaranes, aunque el resto sean
de otros clientes. Con las facturas pasa lo mismo, porque la respuesta lee `clientName`
del cliente vivo y no del snapshot. Hay que decidir si se impide borrar un cliente con
documentos (409) o si las lecturas toleran su ausencia.

---

## 3. Funcionalidad futura

### Rectificativas, anulación y Verifactu
No antes. Hasta entonces, corregir una factura consiste en borrarla, corregir el albarán y
refacturar. Verifactu cierra esa vía: una factura registrada no se borra ni se renumera.
Entonces hacen falta el estado `ANNULLED`, el bloqueo del borrado, `invoiceType`,
`rectifies`, el hash encadenado y el envío a la AEAT.

El flujo completo y los cambios concretos están en CONCEPTO.md §9.

Obligatorio desde el 1-ene-2027 (sociedades) o el 1-jul-2027 (resto), según el RDL 15/2025. Falta confirmar con la gestoría cuál aplica.

### Generación real de PDF
`PdfService` produce un PDF de prueba con una sola línea de texto. Falta la plantilla real
de factura y las dos de albarán (simple y valorado, CONCEPTO.md §3).

### Seguimiento de cobro
`Invoice` no tiene vencimiento ni estado de pago. No se puede saber qué está cobrado,
pendiente o vencido. Es de lo más útil para el uso diario y no depende de nada más.

### Código sugerido para Cliente y Artículo
Piden el `code` en el request y el front va a ciegas. Un endpoint que sugiera el siguiente
libre, permitiendo sobreescribirlo. Para el albarán no hace falta: que lo asigne el
servidor si viene vacío. Para la factura ya es server-side.

### Estadísticas / dashboard
Ningún endpoint de agregados: facturación por mes o cliente, pendiente de cobro, etc.

### Exportación / backup
No hay forma de exportar datos (CSV para la gestoría) ni de respaldar la BD SQLite desde
la API.

### Trazabilidad
Tres enteros sueltos, indexados y sin entidad detrás. Falta decidir qué representan y
poder consultar por lote. Ver CONCEPTO.md §10.

---

## 4. Deuda técnica

### Huecos en los tests
Hay tests de servicio del ciclo de la factura y del perfil, y de controlador solo de
artículos y clientes. Faltan los de controlador de albaranes, facturas, IVA y perfil, y
los de los filtros (`*Specification`).
