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

### Lo que el programa antiguo hacía y SelfBill todavía no
Salió al migrar sus datos (`tools/legacy-import`, CONCEPTO §12). Los históricos se leen bien; lo que falta es
poder hacerlo en documentos nuevos. Son decisiones de producto, no errores:

- **Retención de IRPF.** El antiguo la aplicaba factura a factura (`Ret. IRPF 1 %`, `Total a Pagar`) y la mayoría de
  las facturas del último periodo la llevan. SelfBill no la modela: las migradas guardan el total de factura y el
  detalle queda en un CSV, así que su reimpresión no muestra la retención. Si el negocio sigue reteniendo, es
  imprescindible antes de usarlo de verdad.
- **Devolver envases y abonar.** Se hacía con una línea de cantidad negativa (son miles en el histórico). La API la
  rechaza (`@Positive`), así que hoy no se puede devolver un envase.
- **Líneas sin lote.** El envase no tiene lote, pero la API exige uno de 8 cifras en cada línea.
- **Serie S sin datos fiscales.** Casi todos los clientes de la serie S no tienen NIF ni dirección, y emitir los
  exige (CONCEPTO §5). Habría que completarlos o decidir que esa serie no los pide.
- **Facturar al momento.** Los clientes con `fra` tenían una factura por albarán. Se migran como valorados (§3), que
  se facturan agrupados a fin de mes.
- **Empezar la numeración en otro número.** La serie continúa en el máximo + 1. Si el negocio siguió facturando después
  de la última copia, ese número ya se usó y no hay forma de indicar otro.
- **Compras y gastos.** Apuntes, proveedores y categorías del antiguo quedan en CSV: SelfBill no registra compras.

### Rectificativas, anulación y Verifactu
No antes. Hasta entonces, corregir una factura consiste en borrarla, corregir el albarán y
refacturar. Verifactu cierra esa vía: una factura registrada no se borra ni se renumera.
Entonces hacen falta el estado `ANNULLED`, el bloqueo del borrado, `invoiceType`,
`rectifies`, el hash encadenado y el envío a la AEAT.

En el Libro de facturas, F8 Borrar pasa a Rectificar en la serie con IVA; la serie S conserva el borrado (CONCEPTO §9.2).

El flujo completo y los cambios concretos están en CONCEPTO.md §9.

Obligatorio desde el 1-ene-2027 (sociedades) o el 1-jul-2027 (resto), según el
RDL 15/2025. Falta confirmar con la gestoría cuál aplica.

### Seguimiento de cobro
`Invoice` no tiene vencimiento ni estado de pago. No se puede saber qué está cobrado,
pendiente o vencido. Es de lo más útil para el uso diario y no depende de nada más.
La pantalla Resumen por cliente es donde lo mostraba el programa antiguo (columna
Pendiente, fecha de cobro por factura, "Conforme").

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
- Imprimir albarán ya funciona desde el editor; falta enviarlo por correo.
- Imprimir sin diálogo: arrancar Edge con `--kiosk-printing` en el acceso directo de la app.
- En pantallas pequeñas (1366×768 o menos) la rejilla del albarán deja ver pocas líneas: la cabecera con totales y casillas ocupa mucho.
- Los avisos (toasts) pueden quedar debajo de los títulos fijos de la rejilla; y al bajar a una línea de abajo, su línea de error puede quedar cortada (falta margen inferior al desplazar).
- Teclear el número del albarán.
- Duplicar un albarán.
- Si el cliente original de un albarán se borró, la vista previa de los tipos recalcula IVA y recargo al cambiar de cliente, pero la API compara con el régimen guardado en el albarán (que la respuesta no trae): la vista previa puede no coincidir.
- Un albarán en una factura en borrador deja cambiar el cliente, pero la API lo rechaza al guardar (`INVOICE_MIXED_CLIENTS`): avisar antes.
- Un error propio de un campo ("Tienen que ser 8 cifras", "Este dato es obligatorio") se queda visible después de que la rejilla rellene la celda (precio propuesto, lotes) hasta el siguiente Enter o "+".
- En la rejilla, el importe de la línea sale sin "€" ni separador de miles; en la lista y los totales, con `formatMoney`.
- Cambiar de cliente y pulsar "+" sin Enter, con precios propuestos sin tocar, los manda vacíos: la API
  lo rechaza bajo cada precio. Vaciarlos al teclear el cliente, como ya se hace con el código del artículo.
- Si se pierde la conexión con el diálogo de error abierto, al volver el cursor no queda en Cliente.

### Pendiente del bloque de Ajustes
- Borrar una vigencia de IVA: la API no tiene endpoint.
- Probar la cuenta de correo desde Ajustes.
- Con la base restaurada en otro equipo sin `secret.key`, la contraseña del correo no se puede descifrar pero Ajustes dice "Guardada": comprobar al leer si se puede descifrar.
- Si se cambia el email de proveedor (Gmail → Outlook) sin vaciar servidor y puerto, se quedan los del anterior.

### Pendiente del bloque de facturas
- Imprimir o enviar de una vez todas las facturas de un periodo (el PDF real ya existe).
- Si se pierde la conexión justo al recargar tras emitir en lote, el resumen puede abrirse encima de la pantalla de sin conexión.
- Al volver la conexión, Enter no recarga Facturar ni el Libro hasta pulsar Esc: el cursor no vuelve a la fecha.
- El diálogo "Emitiendo facturas…" se cierra antes de recargar: un instante se ven las filas ya emitidas.
- Un error inesperado al emitir en lote sale como "Error inesperado" sin dejar rastro en la consola.
- Imprimir: una conexión perdida al pedir el PDF sale como "No se ha podido imprimir…" en vez de la pantalla de sin conexión (también en el albarán).
- Con más de 2000 facturas en el periodo, los totales del libro solo suman las que se muestran.
- La carga del detalle ya es común (`DetailLoader`), pero la lógica de lista y fechas (cargar, recordar la posición, Esc, el pie) se repite en cuatro pantallas: Facturar, Libro, Trazabilidad y Resumen por cliente. `DataTable` y `StaticTable` también repiten el marcado de la tabla.
- Tests que faltan: tipos 4 y 4,00 en una misma fila y el orden por tipos en `pending`; una fecha válida por HTTP; "+" gris con todas las filas con aviso; una fila de cliente borrado en la pantalla; Desde posterior a Hasta; F9/F4 con el cursor en una fecha; el panel derecho de Facturar con ~30 albaranes a 1280×800.

### Pendiente del bloque de trazabilidad
- Buscar directamente por lote, sin elegir artículo.
- Abrir el albarán desde el listado de un lote.
- Exportar el listado de trazabilidad (CSV).
- Sin índice por fecha en `delivery_notes` (ni por artículo en las líneas). Medido con el histórico migrado (138.000 líneas, diez años): menos de 70 ms por consulta, no hace falta todavía.
- Los albaranes de un lote se ordenan por fecha y hora antes que por número: uno creado más tarde con la fecha de otro día sale fuera de orden dentro de ese día.
- Al editar una fecha o si falla la carga, la columna activa puede seguir siendo la de lotes: un Esc no hace nada visible.
- La lista de lotes y la de albaranes repiten el mismo cargador con caché; `notTyping` está copiado en `DataTable`, `ListScreen` y Trazabilidad.
- Tests que faltan: fallo al cargar lotes o albaranes; clic en un artículo con los lotes activos; el escape del código y de comillas en el listado; suma 0,1 + 0,2 en el total; OpenAPI no documenta el 400 de `/articles`.

### Pendiente del bloque de resumen por cliente
- En ventanas bajas (1366×625, un navegador maximizado en un portátil de 768 px) solo caben 2 filas por panel a la derecha.
- Un error de fecha hace la cabecera de esta pantalla una línea más alta (las fechas van en la línea del título y no reservan hueco para el error).
- Un cliente borrado que comparta código con uno vivo sale como dos filas con el mismo código, sin marca de borrado.
- Imprimir o exportar el resumen.

### Pendiente del bloque de PDF
- La cabecera del PDF tiene un alto fijo (68 mm): una dirección del titular de tres líneas con logo, o un recuadro del cliente de nueve líneas o más, quedaría cortada sin aviso.
- El PDF se genera dentro de la transacción de lectura (con SQLite eso retiene el bloqueo de escritura unos 30–90 ms); la primera impresión tras arrancar tarda alrededor de un segundo (carga de fuentes): calentarlo al arrancar.
- Caracteres fuera del juego de la Helvetica estándar (ł, ş, cirílico, emojis) salen como "#": incrustar una fuente si hiciera falta.
- El logo se incrusta a tamaño original: un logo de 2 MB hace que cada PDF y cada adjunto pese unos 2 MB. Reducirlo al subirlo.
- Si un albarán sigue en la hoja siguiente, su título "Albarán … de …" no se repite arriba.
- El error de tamaño de subida (413) da siempre el texto del logo: revisarlo si se añade otra subida.
