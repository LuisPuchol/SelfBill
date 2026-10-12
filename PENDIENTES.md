# Pendientes

Lista de trabajo. Las reglas del modelo y sus motivos están en [CONCEPTO.md](CONCEPTO.md).

---

## 1. Lógica

### `TaxConfig` sin histórico
El albarán congela el tipo vigente cuando se crea, no el de su fecha: si se da de alta
tarde un albarán anterior a un cambio de tipo, recibe el tipo nuevo. Lo mismo ocurre al
reaplicar el régimen de un cliente, solo en el componente (IVA o recargo) cuyo régimen
cambia. Si llega a importar, `TaxConfig` necesita vigencias (desde/hasta).

### Filtros de fecha exigen fecha y hora
`dateFrom` y `dateTo` en los filtros de albaranes y facturas son `LocalDateTime`: una fecha
sola (`2026-10-02`) da 400. Para el front convendría aceptar también fechas sin hora.

---

## 2. Bugs conocidos

Ninguno conocido.

---

## 3. Funcionalidad futura

### Rectificativas, anulación y Verifactu
No antes. Hasta entonces, corregir una factura consiste en borrarla, corregir el albarán y
refacturar. Verifactu cierra esa vía: una factura registrada no se borra ni se renumera.
Entonces hacen falta el estado `ANNULLED`, el bloqueo del borrado, `invoiceType`,
`rectifies`, el hash encadenado y el envío a la AEAT.

El flujo completo y los cambios concretos están en CONCEPTO.md §9.

Obligatorio desde el 1-ene-2027 (sociedades) o el 1-jul-2027 (resto), según el
RDL 15/2025. Falta confirmar con la gestoría cuál aplica.

### Generación real de PDF
`PdfService` produce un PDF de prueba con una sola línea de texto. Falta la plantilla real
de factura y las dos de albarán (simple y valorado, CONCEPTO.md §3).
Antes hay que decidir dos snapshots que faltan (CONCEPTO §6): los datos del cliente en el
albarán y los del titular (`UserProfile`) en la factura. Sin ellos, editar un cliente o el
perfil reescribe la reimpresión de documentos ya entregados.

### Seguimiento de cobro
`Invoice` no tiene vencimiento ni estado de pago. No se puede saber qué está cobrado,
pendiente o vencido. Es de lo más útil para el uso diario y no depende de nada más.

### Estadísticas / dashboard
Ningún endpoint de agregados: facturación por mes o cliente, pendiente de cobro, etc.

### Exportación / backup
No hay forma de exportar datos (CSV para la gestoría) ni de respaldar la BD SQLite desde
la API.

### Restaurar clientes y artículos borrados
Un cliente borrado deja sus albaranes pendientes y sus borradores sin poder facturarse ni
editarse, y un artículo borrado impide volver a guardar los albaranes pendientes que lo
usan. Hoy solo se restaura por SQL (`deleted_at = NULL`). Opciones: un endpoint de
restaurar, o que el front avise antes de borrar un cliente con pendientes
(`GET /api/delivery-notes?clientId=&invoiced=false`).

Afecta también a la trazabilidad (CONCEPTO §10). `GET /api/delivery-notes` acepta el
`articleId` o el `clientId` de una fila borrada, pero el front no tiene cómo obtenerlo: las
búsquedas del catálogo solo devuelven filas vivas. Y si su código ya se ha reutilizado,
`GET /api/articles/by-code/{code}` devuelve el artículo nuevo, y buscar por ese código deja
fuera, sin avisar, las entregas del producto borrado.

---

## 4. Deuda técnica

### Huecos en los tests
Hay tests de servicio del ciclo de la factura y del perfil, y de controlador de artículos,
clientes y parte de albaranes. Faltan los de controlador de facturas, IVA y perfil, los del
resto de endpoints de albaranes y los de `*Specification` que no se ejercitan en los tests
de servicio. Tampoco hay tests del envío de email (`DocumentSender`/`EmailService`: asunto,
cuerpo, adjunto y causa del error), que requieren un servidor SMTP falso.
