# Pendientes

Lista de trabajo. Las reglas del modelo y sus motivos están en [CONCEPTO.md](CONCEPTO.md).

---

## 1. Lógica

Nada pendiente.

---

## 2. Bugs conocidos

### La base de datos depende del directorio de arranque
`spring.datasource.url` apunta a `./selfbill.db`. Arrancar la app desde otra carpeta crea
una base vacía sin avisar, y parece que se han perdido los datos. Ponerla junto a la clave:
`${user.home}/.selfbill/selfbill.db`.

### Sin la clave de cifrado, el perfil da 500
`UserProfileService` descifra la contraseña SMTP al responder, y con otra clave el
descifrado lanza excepción. Es lo que pasa al restaurar la base en otro equipo sin
`secret.key`: `GET /api/user-profile` falla siempre y el front no puede ni cargar el
formulario para reintroducirla. Tratar el fallo de descifrado como "sin contraseña".

### Expuesto a la red, con la contraseña en claro
La API ya escucha solo en `127.0.0.1`. Queda que `GET /api/user-profile` devuelve la
contraseña SMTP descifrada: devolver `hasMailPassword` en vez de la contraseña (bloque de
ajustes).

### Errores 500 que deberían ser 4xx
`GlobalExceptionHandler` convierte en `INTERNAL_ERROR`:
- `GET /api/clients/by-name` y `by-nif` cuando dos clientes comparten nombre o NIF (una
  empresa con dos tiendas). Para el NIF, mejor devolver una lista.
- Un `sort` con un campo que no existe.
- Dos peticiones que chocan contra un índice único (`DataIntegrityViolationException`).

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

La base va en modo WAL: con la app en marcha, lo último escrito puede estar todavía en
`selfbill.db-wal`. Copiar solo `selfbill.db` en caliente no es una copia completa; hacerlo
con la app parada, o desde la API con `VACUUM INTO`.

### Restaurar clientes y artículos borrados
Un cliente borrado deja sus albaranes pendientes y sus borradores sin poder facturarse ni
editarse, y un artículo borrado impide volver a guardar los albaranes pendientes que lo
usan. Hoy solo se restaura por SQL (`deleted_at = NULL`). Opciones: un endpoint de
restaurar, o que el front avise antes de borrar un cliente con pendientes
(`GET /api/delivery-notes?clientId=&invoiced=false`).

---

## 4. Deuda técnica

### Huecos en los tests
Hay tests de servicio del ciclo de la factura y del perfil, y de controlador de artículos,
clientes y parte de albaranes. Faltan los de controlador de facturas, IVA y perfil, los del
resto de endpoints de albaranes y los de `*Specification` que no se ejercitan en los tests
de servicio. Tampoco hay tests del envío de email (`DocumentSender`/`EmailService`: asunto,
cuerpo, adjunto y causa del error), que requieren un servidor SMTP falso.

---

## 5. Front

### Pendiente del bloque de catálogo
- `CodeField`: un código inicial que no existe muestra el error antes de tocar el campo y deja
  pasar Enter; al no encontrarlo, `value = null`.
- El "+" del pad pulsado justo después de Enter en un `CodeField` con la consulta en curso se
  ignora (decidir con el editor de albaranes).
- Vacíos mezclados: al montar el payload del albarán, los lotes opcionales
  (`traceabilityCode2/3`, `@Pattern("\\d{8}")`) deben ir como `null`, no como `''`.
- En artículos, el aviso de borrar no cuenta los albaranes que están en facturas en borrador (`invoiced=false`
  solo mira si el albarán no tiene factura).
- Regla: `decimals` de `NumberField` igual a la escala de la API (2).
- `SpaFallbackResolver` no llama a la comprobación de ubicación de Spring (`checkResource`):
  defensa en profundidad.
- Iconos de Lucide importados uno a uno: el import del paquete entero hace lento `svelte-check`.
