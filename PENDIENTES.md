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
usan. El front ya avisa antes de borrar con pendientes, pero hoy solo se restaura por SQL
(`deleted_at = NULL`). Falta un endpoint de restaurar.

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
- En artículos, el aviso de borrar no cuenta los albaranes que están en facturas en borrador (`invoiced=false`
  solo mira si el albarán no tiene factura).
- Regla: `decimals` de `NumberField` igual a la escala de la API (2).
- `SpaFallbackResolver` no llama a la comprobación de ubicación de Spring (`checkResource`):
  defensa en profundidad.
- Iconos de Lucide importados uno a uno: el import del paquete entero hace lento `svelte-check`.
- Un `VALIDATION_ERROR` cuyo campo no está en pantalla no muestra nada: caer a `showError`.
- Buscar un código de 10 cifras o más puede desbordar el `int` de la API (más de 2.147.483.647):
  la lista se cambia por la página de error.
- El borrado y el guardado de `RecordForm` no se excluyen entre sí, y el bloqueo de borrar se
  suelta antes de volver a la lista.
- Faltan tests de las ramas de error de `RecordForm` (`blank()`, borrar, avisos) y de que
  `CodeField` no valida al salir.
- Buscar solo cifras es siempre un código: las cifras de un NIF no se encuentran como texto.
- Con texto, Enter antes de que llegue la búsqueda abre la fila marcada del resultado (la
  primera, o el último abierto si está) sin verla.
- "Guardar y salir" desde un alta no marca en la lista lo recién guardado: `openedId` se apunta
  al pulsar Esc, antes de guardar.
- Al volver a la misma lista desde la barra de arriba (de `/clients?q=gar` a Clientes), el
  buscador no recupera el cursor: el componente se reutiliza y `onMount` no vuelve a correr.
- Borrar la única fila de la última página enseña un momento "Página 3 de 2" y el texto de
  catálogo vacío, y deja en el historial una página que ya no existe: corregir cualquier página
  vacía más allá de la última, reemplazando la entrada del historial.
- Los botones ignoran los clics seguidos (para que un doble clic no pulse lo que aparece debajo):
  tres clics rápidos en "Página siguiente" pasan una sola página. AvPág no tiene ese límite.

### Pendiente del bloque de albaranes
- Imprimir y enviar un albarán (bloque del PDF).
- Teclear el número del albarán.
- Duplicar un albarán.
- Si el cliente original de un albarán se borró, la vista previa de los tipos recalcula IVA y recargo al cambiar de cliente, pero la API compara con el régimen guardado en el albarán (que la respuesta no trae): la vista previa puede no coincidir.
- Un albarán en una factura en borrador deja cambiar el cliente, pero la API lo rechaza al guardar (`INVOICE_MIXED_CLIENTS`): avisar antes.
- Con "Solo pendientes" activo y todos los albaranes facturados, la lista dice "Todavía no hay albaranes…".
- Un error propio de un campo ("Tienen que ser 8 cifras", "Este dato es obligatorio") se queda visible después de que la rejilla rellene la celda (precio propuesto, lotes) hasta el siguiente Enter o "+".
- En la rejilla, el importe de la línea sale sin "€" ni separador de miles; en la lista y los totales, con `formatMoney`.

### Pendiente del bloque de Ajustes
- Borrar una vigencia de IVA: la API no tiene endpoint.
- Logo del titular en Ajustes (junto con el bloque del PDF).
- Probar la cuenta de correo desde Ajustes.
- Con la base restaurada en otro equipo sin `secret.key`, la contraseña del correo no se puede descifrar pero Ajustes dice "Guardada": comprobar al leer si se puede descifrar.
- Si se cambia el email de proveedor (Gmail → Outlook) sin vaciar servidor y puerto, se quedan los del anterior.
