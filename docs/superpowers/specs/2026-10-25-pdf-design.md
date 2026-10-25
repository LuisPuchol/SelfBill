# PDF de albaranes y facturas

Fecha: 2026-10-25, acordado con el usuario en el chat. Base: [CONCEPTO.md](../../../CONCEPTO.md) §3
(formatos de albarán) y §6 (snapshots), [PENDIENTES.md](../../../PENDIENTES.md) "Generación real de
PDF" y "Logo del titular en Ajustes". Modelo visual: la factura impresa del programa antiguo (captura
en el chat). Se trabaja directamente en `main`, como en el bloque de trazabilidad.

---

## 1. Objetivo

Sustituir el PDF de prueba por los documentos reales: la factura, el albarán simple y el albarán
valorado, con el aspecto del programa antiguo y la misma maqueta en la serie general y en la S. Son
los PDF que ya imprimen F9 en el editor del albarán y F9 en el Libro de facturas, y los que adjunta el
correo. Además, subir el logo desde Ajustes, que hoy solo se puede por la API.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Generación | Plantilla HTML (Thymeleaf) convertida a PDF con OpenHTMLtoPDF | Claude, aprobado |
| Número y fecha fuera del recuadro | Se quitan: ya están en el recuadro | Usuario |
| Logo | No se copia en el documento: cambiarlo cambia la reimpresión de los antiguos. Excepción deliberada a CONCEPTO §6 | Usuario |
| Serie S | Misma maqueta, con el IVA a 0,00 | Usuario |
| Copia en la factura | Al emitir guarda además el nombre fiscal, el NIF y la dirección del titular, y el código del cliente. Las ya emitidas se rellenan con el perfil y el código actuales | Claude, aprobado |
| Datos en el albarán | Sin copia propia: datos actuales mientras no está en una factura emitida; después, los de esa factura | Claude, aprobado |
| Varias hojas | Cada hoja lleva la cabecera completa, "Página n/m" y los títulos de la tabla; los totales, solo al final | Claude, aprobado |
| Fuente | Helvetica de PDF, sin incrustar: cubre ñ, tildes, ç, ·, € | Claude |
| Albarán sin datos del titular | Se imprime sin el bloque del titular | Claude |
| Logo en Ajustes | Se sube al elegir el archivo, sin pasar por Guardar; hasta 2 MB | Claude |

---

## 3. Estado actual

- `PdfService` dibuja a mano un PDF de una línea ("Mock invoice #…"). Lo llaman la exportación
  (`GET /api/invoices/{id}/pdf`, solo emitidas; `GET /api/delivery-notes/{id}/pdf`) y el envío por
  correo; el correo ya carga el documento dentro de una transacción de lectura.
- La factura congela al emitir el nombre fiscal, NIF, dirección, población, provincia y código postal
  del cliente (`freezeClientData`), pero no su código ni los datos del titular.
- `InvoiceLine` congela código y nombre del artículo, número y fecha del albarán, lotes, cantidad,
  precio e importe, ordenadas por fecha y número del albarán. Las líneas del albarán no tienen orden
  declarado y toman el nombre del artículo vivo.
- `UserProfile` tiene `fiscalName` ("Legal name, printed on documents"), `nif`, `address` (una sola
  línea), `phone` y el logo (`logoData`, JPEG o PNG). La respuesta trae `hasLogo`. El front no tiene
  forma de subirlo.
- Spring limita la subida a 1 MB; pasarse acaba en un 500 (`INTERNAL_ERROR`).

---

## 4. Diseño

### 4.1. Los documentos

A4 vertical. Cantidades, precios e importes con dos decimales y separador de miles ("1.234,56"), sin
"€" salvo en el total final. Fechas dd/mm/aaaa.

**Cabecera, en todas las hojas:**

```
 [ LOGO ]                                  ╭──────────────────────────────────────╮
                                           │ Factura 3852       Fecha 31/05/2026  │
 ADELINA DELGADO TORRES · NIF X1422821H    │ Cliente: 41 SON TERMENS GRAN CHEF SL │
 Mercado Pedro Garau Puestos 41,42 ...     │ PREDIO SON TERMENS S/N               │
                                           │ 07193 BUNYOLA (ILLES BALEARS)        │
                                           │ CIF/NIF: B07688310                   │
                                           ╰──────────────────────────────────────╯
                                                                       Página 1/5
```

- Izquierda: el logo, hasta 80 × 30 mm y sin deformarlo; sin logo, el nombre fiscal en grande. Debajo,
  "NOMBRE FISCAL · NIF X" y la dirección.
- Recuadro: "Factura 3852" (serie S: "Factura S-12") o "Albarán 31397", y la fecha (la de emisión en la
  factura, la del albarán en el albarán). Debajo, "Cliente: código nombre fiscal", la dirección,
  "CP población (provincia)" y "CIF/NIF: …". Una línea sin datos no sale; de la línea de población sale
  lo que haya, y la provincia entre paréntesis solo si la hay.
- "Página n/m" a la derecha, encima de la tabla.

**Tabla:** Código (del artículo), Concepto (su nombre, en negrita), Trazabilidad, Cantidad, Precio,
Importe. La trazabilidad
lleva los lotes que tenga la línea separados por un espacio; si no caben, siguen en otra línea dentro
de la misma fila. Los títulos se repiten en cada hoja, una fila no se parte entre dos hojas y un título
"Albarán … de …" no se queda solo al pie de una hoja.

**Factura.** Filas "Albarán 31397 de 02/05/2026" seguidas de sus líneas, en el orden de las líneas de
la factura. Al final, el cuadro de totales:

```
 Base imponible   % IVA   Cuota IVA   % Recargo   Cuota recargo        Total
       1.234,56    4,00       49,38        0,50            6,17   1.290,11 €
```

Las columnas de IVA salen siempre (0,00 en la serie S); las de recargo, solo si el documento lleva
recargo. Los importes son los guardados en la factura.

**Albarán simple.** Una tabla de un nivel, sin filas "Albarán … de …", con las líneas en el orden en
que se escribieron. Al final, "Total 1.234,56 €": el total del albarán, sin impuestos.

**Albarán valorado.** Como el simple, con el cuadro de totales de la factura en lugar del total suelto
y debajo la leyenda **"Documento sin valor fiscal. No válido como factura."** (CONCEPTO §3). Los totales
salen de los tipos que congeló el albarán, con el mismo cálculo que la factura (porcentaje redondeado
a dos decimales, mitad hacia arriba). La factura del mes calcula sobre la suma de sus albaranes, así que
puede diferir en algún céntimo de la suma de los albaranes valorados.

### 4.2. De dónde sale cada dato

| Dato | Factura | Albarán fuera de factura emitida | Albarán en factura emitida |
|---|---|---|---|
| Titular (nombre fiscal, NIF, dirección) | Copia de la factura | Perfil actual | Copia de la factura |
| Cliente (código, nombre fiscal, dirección, CP, población, provincia, NIF) | Copia de la factura | Cliente actual, también si está borrado | Copia de la factura |
| Líneas | Líneas de la factura | Líneas del albarán | Líneas de la factura de ese albarán |
| Tipos e importes | Factura | Albarán | Albarán |
| Logo | Perfil actual | Perfil actual | Perfil actual |

Sin perfil (instalación recién hecha) el albarán se imprime sin el bloque del titular. Una factura
emitida siempre tiene su copia: emitir exige el perfil.

### 4.3. La copia en la factura

- Migración nueva: columnas `issuer_fiscal_name`, `issuer_nif`, `issuer_address` y `client_code` en
  `invoices`, que pueden quedar vacías (los borradores no las tienen).
- Emitir las rellena junto con los datos del cliente que ya congela.
- La migración rellena las facturas emitidas que ya existen (también las borradas): el titular con el
  único perfil, si existe, y el código con el del cliente.
- La respuesta de la API no cambia.

### 4.4. Generación

- `PdfService` conserva sus dos métodos públicos: la exportación y el correo no cambian.
- Por dentro, tres pasos separados:
  1. Del albarán o la factura se monta un modelo de lo que se imprime (cabecera, filas, totales,
     leyenda), según la tabla de 4.2. Es una función pura y se prueba sin PDF.
  2. Una plantilla Thymeleaf (`pdf/document.html`) pinta el HTML desde el modelo; escapa los textos.
  3. OpenHTMLtoPDF lo convierte en PDF.
- La cabecera es un elemento que se repite en el margen superior de cada hoja; el contador de páginas
  y la repetición de los títulos de la tabla los da la librería.
- El logo entra en el HTML como `data:` URI con su tipo.
- Las líneas del albarán se ordenan por su `id`.

### 4.5. Logo en Ajustes (`/settings/profile`)

- Con un perfil guardado, debajo de los datos fiscales, una fila "Logo": la vista previa del logo
  actual o "Sin logo", y el botón "Cambiar logo…", que abre el selector de archivos (JPEG o PNG).
- Elegir un archivo lo sube en el acto: toast "Logo guardado" y la vista previa se renueva. No cuenta
  como cambio sin guardar del formulario.
- Sin perfil: "Guarda primero los datos del titular para poder subir el logo.", sin botón.
- El botón no entra en el recorrido de Enter: se llega con Tab o con el ratón.
- Más de 2 MB: no se envía y sale "El logotipo no puede pasar de 2 MB.". Otro tipo: el texto que ya
  existe para `USER_PROFILE_INVALID_LOGO_FORMAT`.
- API: la subida admite hasta 2 MB; pasarse responde 413 con un `ErrorCode` nuevo,
  `USER_PROFILE_LOGO_TOO_LARGE`, en vez de un 500.

### 4.6. Documentación

- CONCEPTO §6: la copia del titular y del código del cliente en la factura, la regla del albarán
  (4.2) y la excepción del logo.
- PENDIENTES: fuera "Generación real de PDF", "Logo del titular en Ajustes" y la nota de que el PDF del
  albarán es provisional.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| Modelo (unidad) | Factura: filas por albarán en orden, totales, columnas de recargo solo con recargo, IVA 0,00 en la serie S, número S-. Albarán simple: total suelto, sin impuestos. Valorado: totales con el cálculo de la factura y la leyenda. Datos según 4.2: albarán fuera de factura con el perfil y el cliente actuales, albarán en factura emitida con la copia y las líneas de la factura, sin perfil sin titular, líneas de población parciales, lotes separados por espacio |
| PDF | Texto extraído del PDF (PDFBox): cabecera, filas, totales y leyenda; "ñ", tildes y "€"; un nombre con `<` y `&` sale tal cual; un documento largo tiene varias hojas, y cada una repite cabecera y títulos con "Página n/m"; con y sin logo |
| API | La copia al emitir (titular y código); la migración rellena las emitidas; exportar los dos documentos devuelve un PDF con su contenido; subir más de 2 MB da 413 `USER_PROFILE_LOGO_TOO_LARGE` |
| Componente | Logo: vista previa o "Sin logo", subir al elegir y toast, más de 2 MB no se envía, el error de formato, sin perfil sin botón |
| e2e | Subir un logo en Ajustes y verlo en la vista previa |
| Visual | Los tres documentos de muestra (uno de varias hojas) convertidos en imagen y revisados; las muestras quedan para que el usuario las abra |
| Arranque | La API arranca desde `target/classes` y devuelve los PDF de un albarán y de una factura emitida |

---

## 6. Fuera de alcance

- Enviar por correo desde el front.
- Imprimir o enviar todas las facturas de un periodo de una vez.
- Imprimir sin diálogo (`--kiosk-printing`).
- Borrar el logo.
- PDF de una factura en borrador.
