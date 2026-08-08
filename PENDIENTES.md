# Pendientes / a revisar en el futuro

Cosas detectadas en la revisión de la API que no son bugs urgentes, pero conviene tener anotadas.

## Rectificación de facturas
No existe forma de corregir una factura ya emitida, solo borrarla. `InvoiceStatus.RECTIFIED` está definido pero nada lo usa. Relacionado con el TODO de Verifactu que ya hay en `Invoice.java`.

## Seguimiento de cobro
`Invoice` no tiene fecha de vencimiento ni estado de pago. No se puede saber qué facturas están cobradas, pendientes o vencidas.

## Código libre para clientes/artículos/albaranes
Solo la factura autogenera su código (`code`). Cliente, Artículo y Albarán lo piden en el request, así que el frontend tiene que adivinar cuál es el siguiente disponible.

## Estadísticas / dashboard
No hay ningún endpoint de agregados: facturación por mes o cliente, pendiente de cobro, etc.

## Exportación / backup
No hay forma de exportar datos (por ejemplo CSV para la gestoría) ni de hacer backup de la base de datos SQLite desde la API.
