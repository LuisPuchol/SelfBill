# Albaranes: totales arriba, imprimir y emitir factura al guardar

Fecha: 2026-10-22, acordado con el usuario en el chat. Base:
[spec de albaranes](2026-10-18-front-albaranes-design.md), [CONCEPTO.md](../../../CONCEPTO.md) §2 y §5.
Plan: [2026-10-22-albaranes-imprimir-facturar.md](../plans/2026-10-22-albaranes-imprimir-facturar.md).
Se trabaja directamente en `main`, sin rama ni PR (decisión del usuario).

---

## 1. Objetivo

Tres cambios en el editor de albaranes pedidos tras usarlo:

1. Los totales, abajo, se mueven con cada línea nueva y no se ven claros: van arriba, fijos.
2. Casilla **"Imprimir albarán"**: al guardar, se manda a imprimir el albarán.
3. Casilla **"Emitir factura"**: al guardar, se crea y se emite una factura solo con ese albarán,
   para los clientes que quieren una factura por albarán.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Estado inicial de las casillas | Siempre sin marcar, en cada albarán nuevo y al abrir uno guardado | Usuario |
| Qué se imprime con las dos marcadas | Solo el albarán | Usuario |
| Cómo se imprime | El diálogo de impresión del navegador (Enter imprime). Sin diálogo, con `--kiosk-printing` en el acceso directo de la app, en otro bloque | Usuario |
| Crear y emitir | Un endpoint nuevo que crea y emite en una sola transacción: si falla, no queda nada a medias | Usuario (enfoque recomendado) |
| Teclas | `F9 · Imprimir albarán`, `F3 · Emitir factura` (libres en el editor y sin uso en el navegador) | Claude, propuesto en el chat |

---

## 3. Estado actual

- Los totales (`Totals`) están debajo de la rejilla.
- `GET /api/delivery-notes/{id}/pdf` devuelve un PDF de prueba ("Mock delivery note #1045"): el
  contenido real llega con el bloque del PDF (PENDIENTES §3).
- Emitir (`POST /api/invoices/{id}/issue`) exige un cliente vivo con NIF, dirección, código postal y
  población, y el perfil del titular; asigna número y fecha (CONCEPTO §5). Crear y emitir son hoy
  dos llamadas.
- `RecordForm` guarda, avisa ("Albarán 1045 guardado") y encadena un alta nueva o vuelve a la lista;
  no tiene un paso propio después de guardar.

---

## 4. Diseño

### 4.1. API

- `POST /api/invoices/issue` con `InvoiceRequest` (`clientId`, `deliveryNoteIds`): crea la factura y
  la emite en la misma transacción. Responde la factura emitida (`InvoiceResponse`, con `number`).
  Con cualquier error de crear o de emitir (`CLIENT_FISCAL_DATA_MISSING`, `USER_PROFILE_NOT_FOUND`,
  `INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED`…) no queda ninguna factura ni el albarán facturado.

### 4.2. Totales arriba

En la cabecera, a la derecha de Cliente y Fecha, un recuadro fijo con Base, IVA (x %), Recargo (y %) y
Total (lo mismo que hoy, con sus reglas y su aviso "No hay IVA configurado para esa fecha"). No se
mueve al añadir o quitar líneas.

### 4.3. Casillas

Debajo de Cliente y Fecha: "Al guardar:" y dos casillas grandes, cada una con su tecla, que se marcan
con un clic o con la tecla y no entran en el recorrido de Enter:

- `☐ Imprimir albarán · F9`
- `☐ Emitir factura · F3`

Siempre sin marcar al empezar un albarán (también el siguiente de una cadena) y al abrir uno
guardado. En un albarán en solo lectura (factura emitida) las dos salen grises.

"Emitir factura" sale gris, con el motivo debajo, cuando:

| Caso | Motivo |
|---|---|
| Al cliente le faltan datos | "Faltan datos del cliente para facturar: NIF y código postal" (los que falten: NIF, dirección, código postal, población) |
| Falta el perfil del titular | "Faltan los datos del titular (Ajustes)" |
| El albarán ya está en una factura | "Ya está en una factura" |

Sin cliente elegido todavía, sale gris sin motivo.

### 4.4. Al guardar ("+")

1. Se guarda el albarán, como hoy (validación, errores bajo los campos…).
2. Si "Emitir factura" está marcada: `POST /api/invoices/issue` con el cliente y ese albarán.
   - Bien: el aviso pasa a ser "Albarán 1045 guardado · Factura 23 emitida".
   - Mal: diálogo de error "El albarán se ha guardado, pero no se ha podido emitir la factura: …"
     (el texto del error de la API en español) y el aviso queda en "Albarán 1045 guardado".
3. Si "Imprimir albarán" está marcada: se pide el PDF del albarán y se abre el diálogo de impresión
   del navegador con él. Si el PDF no llega: diálogo "No se ha podido imprimir el albarán".
4. Después, lo de siempre: un alta abre otro albarán vacío con la misma fecha (con las casillas sin
   marcar); una edición vuelve a la lista.

El paso "después de guardar" es un enganche nuevo de `RecordForm` (`afterSave`), que corre tras guardar
y antes del aviso y del encadenado, y puede añadir texto al aviso.

### 4.5. Imprimir

Una función `printPdf(blob)` (`lib/print.ts`): carga el PDF en un `iframe` oculto y llama a su
`print()`; quita el `iframe` al terminar. Se prueba sustituyendo `print`.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | `POST /api/invoices/issue` emite con número y deja el albarán facturado; sin NIF del cliente, sin perfil o con el albarán ya en otra factura, no crea nada (ni borrador ni albarán facturado) |
| Unidad | Motivos de "Emitir factura" (datos que faltan, sin titular, ya facturado); `printPdf` crea y quita el `iframe` y llama a `print` |
| Componente | Totales arriba; F9 y F3 marcan y desmarcan, igual que el clic; casillas grises con su motivo; al guardar con las dos: guardar → emitir → imprimir, en ese orden, y el aviso con la factura; error al emitir: diálogo y el albarán guardado; error al imprimir: diálogo; el siguiente albarán de la cadena empieza sin marcar; `RecordForm.afterSave` en orden con el aviso y el encadenado |
| e2e | Guardar con "Emitir factura" deja el albarán como "Facturado nº N" en la lista; con "Imprimir albarán" se llama a imprimir (con `print` sustituido) |
| Arranque | La API arranca desde `target/classes` y responde, antes de subir a `main` |

---

## 6. Fuera de alcance

- Imprimir sin diálogo (`--kiosk-printing`, con el acceso directo de la app).
- El contenido real del PDF (bloque del PDF).
- Imprimir o enviar la factura emitida desde aquí (se hará desde Facturas).
- Una opción por cliente que marque "Emitir factura" (descartada: siempre sin marcar).
