# Pendientes

Lista de trabajo. Las reglas del modelo y sus motivos están en [CONCEPTO.md](CONCEPTO.md). La lógica del dominio está
completa: lo que queda son bugs, pulido y funciones nuevas.

Las fases van en el orden en que se van a hacer. Dentro de cada una, lo más sencillo primero:

- **Sencillo**: un rato; el arreglo se ve en la propia descripción.
- **Medio**: unas horas, o toca API y front a la vez.
- **Grande**: días; necesita un diseño antes.

Y cada bug, su prioridad:

- **Alta**: impide el uso diario o puede hacer creer que se han perdido datos.
- **Media**: se nota en el uso normal o da un dato confuso, pero tiene salida.
- **Baja**: caso raro, o solo estético.

---

## Fase 1. Que funcione lo básico

Lo que impide usar el programa o puede dar un dato mal. De momento basta con que funcione lo básico.

### Sencillo
- **Alta** · La base depende del directorio de arranque. `spring.datasource.url` apunta a `./selfbill.db`: arrancar la app desde otra carpeta crea una base vacía sin avisar, y parece que se han perdido los datos. Ponerla junto a la clave: `${user.home}/.selfbill/selfbill.db`.
- **Media** · Albaranes: cambiar de cliente y pulsar "+" sin Enter, con precios propuestos sin tocar, los manda vacíos y la API lo rechaza bajo cada precio. Vaciarlos al teclear el cliente, como ya se hace con el código del artículo.
- **Media** · Albaranes: un albarán en una factura en borrador deja cambiar el cliente, pero la API lo rechaza al guardar (`INVOICE_MIXED_CLIENTS`): avisar antes.
- **Media** · Un `VALIDATION_ERROR` cuyo campo no está en pantalla no muestra nada: caer a `showError`.
- **Media** · Resumen por cliente: un cliente borrado que comparta código con uno vivo sale como dos filas con el mismo código, sin marca de borrado. Con los datos migrados pasará a menudo: hay 37 clientes anteriores con el código de uno vivo.
- **Media** · Facturar: un error inesperado al emitir en lote sale como "Error inesperado" sin dejar rastro en la consola.
- **Baja** · Artículos: el aviso de borrar no cuenta los albaranes que están en facturas en borrador (`invoiced=false` solo mira si el albarán no tiene factura).
- **Baja** · Buscar un código de 10 cifras o más puede desbordar el `int` de la API (más de 2.147.483.647): la lista se cambia por la página de error.
- **Baja** · API: un `sort` con un campo que no existe da 500 (`INTERNAL_ERROR`) en vez de 400.
- **Baja** · API: dos peticiones que chocan contra un índice único (`DataIntegrityViolationException`) dan 500 en vez de 409.
- **Baja** · Ajustes: si se cambia el email de proveedor (Gmail → Outlook) sin vaciar servidor y puerto, se quedan los del anterior.
- **Baja** · Trazabilidad: los albaranes de un lote se ordenan por fecha y hora antes que por número: uno creado más tarde con la fecha de otro día sale fuera de orden dentro de ese día.
- **Baja** · Imprimir: una conexión perdida al pedir el PDF sale como "No se ha podido imprimir…" en vez de la pantalla de sin conexión (también en el albarán).
- **Baja** · Al volver la conexión, Enter no recarga Facturar ni el Libro hasta pulsar Esc: el cursor no vuelve a la fecha.
- **Baja** · Albaranes: si se pierde la conexión con el diálogo de error abierto, al volver el cursor no queda en Cliente.
- **Baja** · `CodeField`: un código inicial que no existe muestra el error antes de tocar el campo y deja pasar Enter; al no encontrarlo, `value = null`.

### Medio
- **Media** · Libro de facturas: con más de 2000 facturas en el periodo, los totales solo suman las que se muestran. Con los años migrados, un periodo de varios años da un total mal sin avisar.
- **Media** · Restaurar clientes y artículos borrados. Un cliente borrado deja sus albaranes pendientes y sus borradores sin poder facturarse ni editarse, y un artículo borrado impide volver a guardar los albaranes pendientes que lo usan. El front ya avisa antes de borrar con pendientes, pero hoy solo se restaura por SQL (`deleted_at = NULL`). Falta un endpoint de restaurar.
- **Baja** · API: `GET /api/clients/by-name` y `by-nif` dan 500 cuando dos clientes comparten nombre o NIF (una empresa con dos tiendas). Para el NIF, mejor devolver una lista. El front no los usa, pero los clientes migrados de la serie S comparten el NIF fantasma.
- **Baja** · Ajustes: con la base restaurada en otro equipo sin `secret.key`, la contraseña del correo no se puede descifrar pero Ajustes dice "Guardada": comprobar al leer si se puede descifrar. Va con las copias de seguridad (fase 3).
- **Baja** · Albaranes: si el cliente original de un albarán se borró, la vista previa de los tipos recalcula IVA y recargo al cambiar de cliente, pero la API compara con el régimen guardado en el albarán (que la respuesta no trae): la vista previa puede no coincidir.
- **Baja** · `RecordForm`: el borrado y el guardado no se excluyen entre sí, y el bloqueo de borrar se suelta antes de volver a la lista.
- **Baja** · Catálogo: con texto, Enter antes de que llegue la búsqueda abre la fila marcada del resultado (la primera, o el último abierto si está) sin verla.
- **Baja** · Facturar: si se pierde la conexión justo al recargar tras emitir en lote, el resumen puede abrirse encima de la pantalla de sin conexión.

### Puesta en marcha y migración
La importación del programa antiguo está hecha (`tools/legacy-import`, CONCEPTO §12). Las funciones del antiguo que no se usan no se adaptan: no son carencias.

- **El día del cambio.** El programa antiguo sigue en uso. Ese día: una copia nueva desde él, repetir la importación con ella y, desde ese momento, solo SelfBill. Lo que se haga después en el antiguo no llega.
- **Imprimir sin diálogo** (sencillo): arrancar Edge con `--kiosk-printing` en el acceso directo de la app.
- **Facturas con contador propio, sin prisa.** Un tipo de factura antiguo, con IVA, tenía numeración aparte (CONCEPTO §12) y hoy sale en negativo. No es la serie S. La última es de 2022: si ese tipo ya no se usa, no hace falta nada; si se usa, haría falta una serie propia.

---

## Fase 2. Pasada de uso y pulido

Una pasada a mano por el programa, usándolo como en el día a día: botones, espacios, CSS, colores, diseño y, sobre todo, que cada acción diga qué ha pasado. Lo que salga de la pasada se apunta aquí. Ya conocido:

### Sencillo
- **Media** · Enviar una factura por correo no deja claro si ha salido: mientras se envía (puede tardar unos segundos) no se ve nada, y al terminar solo sale un aviso breve. Hace falta un "Enviando…" y una confirmación que se vea.
- **Baja** · Albaranes: en la rejilla, el importe de la línea sale sin "€" ni separador de miles; en la lista y los totales, con `formatMoney`.
- **Baja** · Albaranes: un error propio de un campo ("Tienen que ser 8 cifras", "Este dato es obligatorio") se queda visible después de que la rejilla rellene la celda (precio propuesto, lotes) hasta el siguiente Enter o "+".
- **Baja** · Albaranes: los avisos (toasts) pueden quedar debajo de los títulos fijos de la rejilla; y al bajar a una línea de abajo, su línea de error puede quedar cortada (falta margen inferior al desplazar).
- **Baja** · Facturar: el diálogo "Emitiendo facturas…" se cierra antes de recargar: un instante se ven las filas ya emitidas.
- **Baja** · Resumen por cliente: un error de fecha hace la cabecera una línea más alta (las fechas van en la línea del título y no reservan hueco para el error).
- **Baja** · Trazabilidad: al editar una fecha o si falla la carga, la columna activa puede seguir siendo la de lotes: un Esc no hace nada visible.
- **Baja** · Catálogo: "Guardar y salir" desde un alta no marca en la lista lo recién guardado: `openedId` se apunta al pulsar Esc, antes de guardar.
- **Baja** · Catálogo: al volver a la misma lista desde la barra de arriba (de `/clients?q=gar` a Clientes), el buscador no recupera el cursor: el componente se reutiliza y `onMount` no vuelve a correr.
- **Baja** · Los botones ignoran los clics seguidos (para que un doble clic no pulse lo que aparece debajo): tres clics rápidos en "Página siguiente" pasan una sola página. AvPág no tiene ese límite.
- **Baja** · PDF: la primera impresión tras arrancar tarda alrededor de un segundo (carga de fuentes): calentarlo al arrancar.

### Medio
- **Media** · Ajustes: probar la cuenta de correo. Va con el aviso de si un correo ha salido.
- **Media** · Albaranes: en pantallas pequeñas (1366×768 o menos) la rejilla deja ver pocas líneas: la cabecera con totales y casillas ocupa mucho.
- **Media** · PDF: si un albarán sigue en la hoja siguiente, su título "Albarán … de …" no se repite arriba. Se nota en las facturas largas, como las mensuales de la serie S.
- **Baja** · Resumen por cliente: en ventanas bajas (1366×625, un navegador maximizado en un portátil de 768 px) solo caben 2 filas por panel a la derecha.
- **Baja** · Catálogo: borrar la única fila de la última página enseña un momento "Página 3 de 2" y el texto de catálogo vacío, y deja en el historial una página que ya no existe: corregir cualquier página vacía más allá de la última, reemplazando la entrada del historial.
- **Baja** · Catálogo: buscar solo cifras es siempre un código: las cifras de un NIF no se encuentran como texto.
- **Baja** · PDF: la cabecera tiene un alto fijo (68 mm): una dirección del titular de tres líneas con logo, o un recuadro del cliente de nueve líneas o más, quedaría cortada sin aviso.
- **Baja** · PDF: el logo se incrusta a tamaño original: un logo de 2 MB hace que cada PDF y cada adjunto pese unos 2 MB. Reducirlo al subirlo.
- **Baja** · PDF: caracteres fuera del juego de la Helvetica estándar (ł, ş, cirílico, emojis) salen como "#": incrustar una fuente si hiciera falta.

---

## Fase 3. Copias de seguridad

Decidir si hacen falta y, si hacen falta, cómo. Hoy no hay forma de respaldar la base desde la app.

- El programa antiguo las hacía casi a diario.
- La base va en modo WAL: con la app en marcha, lo último escrito puede estar todavía en `selfbill.db-wal`. Copiar solo `selfbill.db` en caliente no es una copia completa; hacerlo con la app parada, o desde la API con `VACUUM INTO`.
- Restaurar en otro equipo necesita también `secret.key`, o la contraseña del correo no se descifra (ver fase 1).

---

## Fase 4. CI/CD y mantenimiento

Nada pensado todavía: hace falta un análisis de cómo llevar las versiones, el despliegue y el mantenimiento.

- Control de versiones: ramas, etiquetas y numeración de versiones.
- Construir y pasar los tests en cada cambio.
- Despliegue: cómo llega una versión nueva al ordenador del usuario y cómo se actualiza sin tocar la base ni `secret.key`. Hoy el entregable es el jar de `npm run package`, y las migraciones de la base (Flyway) corren al arrancar.

---

## Fase 5. Integraciones fuera de la aplicación

Que los pedidos que el usuario recibe por WhatsApp o por correo se conviertan en un albarán sin teclearlo a mano, y automatizaciones de ese estilo. Por analizar. Lo que ya condiciona:

- SelfBill funciona sin internet: la integración no puede ser requisito para trabajar.
- El precio es de mercado y se fija en la línea del albarán (CONCEPTO §4): un albarán que llega de un pedido tiene que poder revisarse antes de darlo por bueno.

---

## Fase 6. Rectificativas, anulación y Verifactu

Lo último, cuando el entorno de trabajo sea cómodo y completamente estable.

Obligatorio desde el 1-ene-2027 (sociedades) o el 1-jul-2027 (resto), según el RDL 15/2025. Falta confirmar con la
gestoría cuál aplica.

Hasta entonces, corregir una factura consiste en borrarla, corregir el albarán y refacturar. Verifactu cierra esa vía:
una factura registrada no se borra ni se renumera. Entonces hacen falta el estado `ANNULLED`, el bloqueo del borrado,
`invoiceType`, `rectifies`, el hash encadenado y el envío a la AEAT.

En el Libro de facturas, F8 Borrar pasa a Rectificar en la serie con IVA; la serie S conserva el borrado (CONCEPTO §9.2).

El flujo completo y los cambios concretos están en CONCEPTO.md §9.

---

## Más adelante

Funciones nuevas, sin fase. Solo si se van a usar; de la más sencilla a la más trabajosa:

- Trazabilidad: abrir el albarán desde el listado de un lote.
- Albaranes: enviarlo por correo (imprimirlo ya funciona desde el editor).
- Albaranes: teclear el número del albarán.
- Albaranes: duplicar un albarán.
- Ajustes: borrar una vigencia de IVA (la API no tiene endpoint).
- Trazabilidad: buscar directamente por lote, sin elegir artículo.
- Trazabilidad: exportar el listado (CSV).
- Resumen por cliente: imprimirlo o exportarlo.
- Facturas: imprimir o enviar de una vez todas las de un periodo (el PDF real ya existe).
- Exportar datos en CSV para la gestoría.
- **Seguimiento de cobro y estadísticas.** `Invoice` no tiene vencimiento ni estado de pago, y no hay ningún endpoint de agregados (facturación por mes o cliente, pendiente de cobro...). No corre prisa: en una empresa pequeña el cobro se lleva a mano, y el programa antiguo casi no lo usaba (ninguna de sus facturas de 2011 a 2026 tiene fecha de cobro). Si se automatiza, será con un dashboard. El programa antiguo lo mostraba en el Resumen por cliente (columna Pendiente, fecha de cobro por factura, "Conforme").

---

## Deuda técnica

De la más sencilla a la más trabajosa:

- `SpaFallbackResolver` no llama a la comprobación de ubicación de Spring (`checkResource`): defensa en profundidad.
- El error de tamaño de subida (413) da siempre el texto del logo: revisarlo si se añade otra subida.
- El PDF se genera dentro de la transacción de lectura: con SQLite eso retiene el bloqueo de escritura unos 30–90 ms.
- `notTyping` está copiado en `DataTable`, `ListScreen` y Trazabilidad, y las listas de lotes y de albaranes de Trazabilidad repiten el mismo cargador con caché.
- Tests del front que faltan:
  - Catálogo: las ramas de error de `RecordForm` (`blank()`, borrar, avisos) y que `CodeField` no valida al salir.
  - Facturas: tipos 4 y 4,00 en una misma fila y el orden por tipos en `pending`; una fecha válida por HTTP; "+" gris con todas las filas con aviso; una fila de cliente borrado en la pantalla; Desde posterior a Hasta; F9/F4 con el cursor en una fecha; el panel derecho de Facturar con ~30 albaranes a 1280×800.
  - Trazabilidad: fallo al cargar lotes o albaranes; clic en un artículo con los lotes activos; el escape del código y de comillas en el listado; suma 0,1 + 0,2 en el total; OpenAPI no documenta el 400 de `/articles`.
- Tests de la API que faltan. Hay tests de servicio del ciclo de la factura y del perfil, y de controlador de artículos, clientes y parte de albaranes. Faltan los de controlador de facturas, IVA y perfil, los del resto de endpoints de albaranes y los de `*Specification` que no se ejercitan en los tests de servicio. Tampoco hay tests del envío de email (`DocumentSender`/`EmailService`: asunto, cuerpo, adjunto y causa del error), que requieren un servidor SMTP falso.
- La carga del detalle ya es común (`DetailLoader`), pero la lógica de lista y fechas (cargar, recordar la posición, Esc, el pie) se repite en cuatro pantallas: Facturar, Libro, Trazabilidad y Resumen por cliente. `DataTable` y `StaticTable` también repiten el marcado de la tabla.

Convenciones que mantener:

- `decimals` de `NumberField` igual a la escala de la API (2).
- Iconos de Lucide importados uno a uno: el import del paquete entero hace lento `svelte-check`.

Por vigilar:

- Sin índice por fecha en `delivery_notes` (ni por artículo en las líneas). Medido con el histórico migrado (138.000 líneas, diez años): menos de 70 ms por consulta, no hace falta todavía.
