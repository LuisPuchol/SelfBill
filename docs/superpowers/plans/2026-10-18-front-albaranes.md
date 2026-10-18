# Albaranes: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A delivery-note list and a keyboard-first editor (header + editable line grid + live totals) for the daily work, with the API telling each note's invoice state and searching by client.

**Architecture:** Two small API additions (invoice state/number on each note; `q` client search). Shared front pieces get small extensions (`validateAll` waits for running lookups, `RecordForm.firstField`, `RecordScreen.readOnly`, `ListScreen` extra actions and `canDelete`, compact fields). A `deliverynotes/` module holds pure logic (payload, totals, last price, code lookups with cache) and components (`LinesGrid`, `LotsDialog`, `Totals`, `DeliveryNoteRecord`), wired into `/delivery-notes` routes.

**Tech Stack:** Spring Boot 3.5 + Java 25 (`JAVA_HOME`), SQLite, MapStruct, JPA Specifications; SvelteKit 3, Svelte 5 runes, Tailwind 4 tokens, Vitest 4 browser mode, Playwright e2e on the packaged jar (port 18080, temp DB).

**Spec:** `docs/superpowers/specs/2026-10-18-front-albaranes-design.md`

## Global Constraints

- Front paths relative to `front/`; API paths relative to `selfbill-api/src/`.
- Inside `src/lib`, relative imports; outside, `#lib/<path>.js` / `#lib/<path>.svelte`. Every internal `href`/`goto` through `resolve()`. One `useKeys`/`keyLayer` per component. "+" is `NumpadAdd`.
- Spanish UI texts exactly as the spec writes them; English code and comments. Buttons are `ActionButton`s built from the same `KeyAction` as their key.
- Money: `lineTotalCents` / `formatMoney` from `src/lib/format/money.ts`; percentages with `formatPercent`; dates with `formatDate`/`todayIso`. Quantity and price: 2 decimals (`NumberField decimals={2}`).
- Never start, stop or bind anything on port 8080 (the user's API) and never touch the user's `selfbill.db` or `~/.selfbill/secret.key`; temporary APIs use another port with temp DB + key (see `front/e2e/serve-jar.mjs`). The user's API runs from `selfbill-api/target/classes`: prefer `./mvnw -q test` without `clean`. No python.
- Implementers never commit or stage; the controller commits after review.
- Commands: front `npm run check`, `npm run lint`, `npm test`, `npm run test:e2e`; API `./mvnw -q test`.

## Review Focus

1. A trailing empty line never blocks saving and is never sent; a half-filled line always blocks it (Task 4 unit + Task 5 component tests).
2. "+" pressed right after Enter on an article code still saves once the lookup ends (Task 2 test; e2e in Task 7 types fast).
3. Editing a saved note keeps its time of day and its frozen rates in the totals while date and client don't change (Task 4 tests).
4. A note in an issued invoice can't be changed by any key or button: "+", F4, F7, F8 and typing do nothing (Task 6 test).
5. Removing the line with the cursor, or the only line, leaves a consistent grid and Enter order (Task 5 test).

---

### Task 1: Invoice state and client search on delivery notes (API) + regenerated types

**Files:**
- Modify: `main/java/com/luispuchol/selfbill/selfbill_api/billing/deliverynote/{DeliveryNoteResponse,DeliveryNoteMapper,DeliveryNoteFilter,DeliveryNoteSpecification}.java` (and the controller's OpenAPI docs if `q` needs a description)
- Test: `test/java/.../billing/deliverynote/DeliveryNoteQueriesTest.java` (extend), `test/java/.../OpenApiDocsTest.java`
- Modify: `front/src/lib/api/schema.d.ts` (regenerated)

**Interfaces:**
- Produces: `DeliveryNoteResponse.invoiceStatus: InvoiceStatus | null` (mapped from `invoice.status`), `DeliveryNoteResponse.invoiceNumber: String | null` (`invoice.getNumber()`, null for drafts). `DeliveryNoteFilter.q: String` → notes whose client's `searchText` contains `SearchText.normalize(q)` (like `ClientSpecification`).

- [ ] **Step 1: Failing tests** (in `DeliveryNoteQueriesTest`, its existing setup style):
  - `invoiceStateIsNullWithoutInvoice`, `invoiceStateIsDraftWithoutNumber` (note in a draft invoice → `DRAFT`, `invoiceNumber` null), `invoiceStateIsIssuedWithNumber` (issued → `ISSUED`, `"1"`; a WITHOUT_VAT client → `"S-1"`).
  - `qFindsByTradeNameWithoutAccents` ("garcia" finds trade name "Frutería García"), `qFindsByBusinessName`, `qFindsByNif`, `qCombinesWithInvoiced`.
  - `OpenApiDocsTest`: `GET /api/delivery-notes` documents `q`.
- [ ] **Step 2: Run, see them fail.** `./mvnw -q test -Dtest='DeliveryNoteQueriesTest,OpenApiDocsTest'`.
- [ ] **Step 3: Implement**, then `./mvnw -q test`.
- [ ] **Step 4: Regenerate the front types** without touching 8080: `./mvnw -q package -DskipTests`, start the jar on **18081** with temp DB and temp key like `front/e2e/serve-jar.mjs`, run `npx -y openapi-typescript@7.13.0 http://127.0.0.1:18081/v3/api-docs -o src/lib/api/schema.d.ts` from `front/`, stop that PID, delete the temp folder. `npm run check` clean.
- [ ] **Step 5: Controller commits** `add invoice state and client search to delivery notes`.

---

### Task 2: `validateAll` waits for running lookups, `RecordForm.firstField`, `RecordScreen.readOnly`

**Files:**
- Modify: `src/lib/keyboard/form-nav.ts`, `src/lib/records/record-form.svelte.ts`, `src/lib/components/RecordScreen.svelte`
- Test: `src/lib/keyboard/form-nav.svelte.spec.ts`, `src/lib/components/RecordScreen.svelte.spec.ts`, `src/lib/components/CodeField.svelte.spec.ts`

**Interfaces:**
- Produces: `FormNav.validateAll()` — when a commit started by `next()` (or another `validateAll`) is still running, it waits for it to finish and then validates, instead of returning `false`. Two `validateAll` calls at once must not run the commits twice concurrently (the second waits for the first, then runs).
- Produces: `RecordFormOptions.firstField?: string` (default `'code'`): the field focused after a chained new record.
- Produces: `RecordScreen` prop `readOnly?: boolean` (default false): no `Guardar · +` nor `Borrar · F8` (neither button nor key); `Volver · Esc` stays.

- [ ] **Step 1: Failing tests.**
  - `form-nav.svelte.spec.ts` → `validateAll waits for a commit that is still running`: a field whose commit resolves later; call `next()` then `validateAll()` before it resolves → `validateAll` resolves `true` after the commit resolves (not `false` immediately).
  - `CodeField.svelte.spec.ts` (or the `CodeHarness`): type a code, Enter, "+" before the lookup answers → the save callback runs once, after the lookup.
  - `RecordScreen.svelte.spec.ts`: `firstField puts the cursor there after a chained new record` (harness option `firstField: 'name'` → cursor in "Nombre"); `readOnly hides Guardar and Borrar and ignores "+" and F8` (key bar has only `Esc Volver`; `create`/`update`/`remove` not called).
  - Existing `form-nav`, `RecordScreen`, `EditScreen`, `unsaved` specs pass unchanged.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** focused specs, `npm test`, `npm run check`, `npm run lint`.
- [ ] **Step 5: Controller commits** `wait for running lookups before saving, add firstField and readOnly records`.

---

### Task 3: `ListScreen` extra actions and `canDelete`; compact fields

**Files:**
- Modify: `src/lib/components/ListScreen.svelte`, `src/lib/test/ListHarness.svelte`, `src/lib/components/{Field,TextField,NumberField,CodeField,DigitsField}.svelte`
- Test: `src/lib/components/ListScreen.svelte.spec.ts`, `src/lib/components/Fields.svelte.spec.ts`

**Interfaces:**
- Produces: `ListScreen` prop `extraActions?: { action: KeyAction; variant?: 'normal' | 'primary' | 'danger' }[]` — each registered in the list's `useKeys` and shown as an `ActionButton` after "Borrar" in the header (enabled by `action.when`). `deletion.canDelete?(row: T): boolean` — F8 and "Borrar" only act (and are only enabled) when it is true for the marked row.
- Produces: prop `compact?: boolean` on `Field`, `TextField`, `NumberField`, `CodeField`, `DigitsField`: the label is only for screen readers (`sr-only`), the error line is smaller and the field fits a table cell; `CodeField` with `compact` does not render the found name (the grid shows it in its own column) and gains `onFound?(code: number): void` called after a successful lookup.

- [ ] **Step 1: Failing tests.**
  - `ListScreen`: `an extra action has its key and its button` (`{ key: 'F3', label: 'Solo pendientes', run }` → F3 and the button run it; key bar shows `F3 Solo pendientes`); `canDelete false greys out Borrar and ignores F8` for the marked row; true → asks as before.
  - `Fields`: `a compact field keeps its accessible name and its error` (input found by its label; label has `sr-only`; error text visible); `a compact CodeField calls onFound and hides the name`.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** focused specs, `npm test`, `npm run check`, `npm run lint`.
- [ ] **Step 5: Controller commits** `add list extra actions, canDelete and compact fields`.

---

### Task 4: Delivery-note logic (pure)

**Files:**
- Create: `src/lib/deliverynotes/{form.ts,totals.ts,last-price.ts,lookups.ts}` with specs `{form,totals,last-price}.spec.ts`

**Interfaces:**
- Produces (`form.ts`):
  ```ts
  export interface LineValues { articleCode: number | null; lot1: string; lot2: string; lot3: string; quantity: number | null; price: number | null }
  export interface NoteValues { code: number | null; clientCode: number | null; date: string /* yyyy-mm-dd */; lines: LineValues[] }
  export function emptyLine(): LineValues;
  export function isEmptyLine(line: LineValues): boolean;          // articleCode, lot1, quantity, price all empty (lot2/3 ignored)
  export function blankNote(date: string): NoteValues;              // code null, clientCode null, one empty line
  export function noteValues(response: DeliveryNoteResponse): NoteValues; // from the API; date part only
  export function notePayload(values: NoteValues, ids: { clientId: number; articleId(code: number): number }, time: string /* HH:mm:ss */): DeliveryNoteRequest;
  // empty lines dropped; lot2/lot3 '' → null; code only when not null; date `${values.date}T${time}`
  export function timeOf(response: DeliveryNoteResponse): string;   // the saved note's HH:mm:ss
  ```
- Produces (`totals.ts`):
  ```ts
  export interface Rates { vat: number; surcharge: number }
  export function ratesFor(period: { vatPercentage?: number; surchargePercentage?: number } | null, client: { vatType: 'WITH_VAT' | 'WITHOUT_VAT'; surchargeType: 'WITH_SURCHARGE' | 'WITHOUT_SURCHARGE' }): Rates | null; // null when no period
  export function periodOn(date: string, periods: TaxConfigResponse[]): TaxConfigResponse | null; // latest validFrom <= date
  export function totals(lines: { quantity: number; price: number }[], rates: Rates): { baseCents: number; vatCents: number; surchargeCents: number; totalCents: number };
  // base = Σ lineTotalCents; vat/surcharge = round half up of base × rate / 100
  ```
- Produces (`last-price.ts`): `lastPrice(notes: DeliveryNoteResponse[], articleId: number): number | null` — from notes already sorted newest first, the price of the last line with that article in the first note that has one; and `lastPriceQuery(clientId, articleId)` → the `GET /api/delivery-notes` query `{ clientId, articleId, size: 1, sort: ['date,desc', 'code,desc'] }`.
- Produces (`lookups.ts`): `createLookups(api)` → `{ client(code): Promise<ClientResponse | null>; article(code): Promise<ArticleResponse | null>; cachedClient(code); cachedArticle(code) }` using `GET /api/clients/by-code/{code}` and `/api/articles/by-code/{code}`; 404 → null; results cached by code.

- [ ] **Step 1: Failing tests** with exact values:
  - `form.spec`: `blankNote('2026-10-18')`; `isEmptyLine` true for a fresh line and for a line with only lot2; `notePayload` drops the empty trailing line, maps codes to ids, sends `traceabilityCode2: null`, `date: '2026-10-18T09:15:00'`, omits `code` when null and keeps it otherwise; `noteValues` + `timeOf` from a response dated `2026-10-17T08:30:00`.
  - `totals.spec`: lines 25 × 1,20 and 3 × 0,33 → base 3099 cents; rates 4 / 0.5 → vat 124, surcharge 15, total 3238; `ratesFor` WITHOUT_VAT → vat 0; WITHOUT_SURCHARGE → surcharge 0; no period → null; `periodOn` picks 2026-01-01 for 2026-10-18 with periods 2023-01-01, 2026-01-01, 2027-01-01, and null before the first.
  - `last-price.spec`: picks the last matching line of the first note; null when no line has the article; null for `[]`.
- [ ] **Step 2: Run, see them fail.** `npx vitest run src/lib/deliverynotes`.
- [ ] **Step 3: Implement.** Check the totals' rounding against the API's (`DeliveryNote`/`InvoiceService` line totals: half up per line); mirror it.
- [ ] **Step 4: Run** the specs, `npm run check`, `npm run lint`.
- [ ] **Step 5: Controller commits** `add delivery note form, totals and price logic`.

---

### Task 5: `LinesGrid` and `LotsDialog`

**Files:**
- Create: `src/lib/deliverynotes/LinesGrid.svelte`, `src/lib/deliverynotes/LotsDialog.svelte`, `src/lib/test/GridHarness.svelte` (a `Form` + `LinesGrid` + `KeyBar`, no `$app`)
- Test: `src/lib/deliverynotes/LinesGrid.svelte.spec.ts`

**Interfaces:**
- Consumes: compact fields and `CodeField.onFound` (Task 3), `LineValues`/`emptyLine`/`isEmptyLine` (Task 4).
- Produces: `LinesGrid` props `{ lines: LineValues[] (bindable); readOnly?: boolean; articleName(code: number): string | undefined; lookupArticle(code: number): Promise<string | null>; proposePrice(code: number): Promise<number | null>; errorFor(name: string): string | undefined }` and an exported `appendLine()` the page calls from the form's `onLast`. Field names `lines[i].articleCode`, `lines[i].lot1`, `lines[i].quantity`, `lines[i].price` (so API errors land under them). Its keys (one `useKeys`): ↑/↓ (hidden), `F4 · Lotes`, `F7 · Quitar línea`; its buttons `Lotes · F4` and `Quitar línea · F7` above the table. `LotsDialog` props `{ open (bindable); lots: [string, string, string]; onAccept(lots): void }` — a `Dialog` with three compact `DigitsField length={8}` (first required), `Aceptar · +`, `Cancelar · Esc`.

- [ ] **Step 1: Failing tests** (`GridHarness`, spies for lookups):
  - `Enter walks code → lot → quantity → price, then the next line's code`;
  - `Enter on the last price adds a line and puts the cursor in its code` (harness wires `onLast` → `appendLine`);
  - `the found article name shows in its column`;
  - `the last price is proposed when the price is empty` (proposePrice → 1.2 → price shows "1,20"), `not when a price is already typed`;
  - `↑ and ↓ keep the column`;
  - `F7 removes the line with the cursor and keeps the column`, `F7 on the only line leaves it empty`;
  - `F4 opens the lots; + saves them into the line and "+1" shows`; `Esc cancels`;
  - `a half-filled line fails validateAll; an empty trailing line passes` (lot required only when the line has content);
  - `readOnly: no F4/F7, inputs read-only`;
  - `buttons Lotes and Quitar línea act on the line with the cursor`.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** Requiredness of a cell depends on its line not being empty (`isEmptyLine`); quantity validate `> 0` → "Tiene que ser mayor que cero"; the "+N" marker counts non-empty lot2/lot3.
- [ ] **Step 4: Run** the spec, `npm test`, `npm run check`, `npm run lint`.
- [ ] **Step 5: Controller commits** `add the delivery note line grid and lots dialog`.

---

### Task 6: Delivery-note editor (`DeliveryNoteRecord`, routes `new` and `[id]`)

**Files:**
- Create: `src/lib/deliverynotes/{Totals.svelte,DeliveryNoteRecord.svelte,DeliveryNoteRecordPage.svelte,deletion.ts}`, `src/routes/delivery-notes/new/{+page.ts,+page.svelte}`, `src/routes/delivery-notes/[id]/{+page.ts,+page.svelte}`, harness in `src/lib/test/`
- Test: `src/lib/deliverynotes/DeliveryNoteRecord.svelte.spec.ts`, `e2e/delivery-notes.e2e.ts` (new)

**Interfaces:**
- Consumes: Tasks 2–5.
- Produces: `deliveryNoteDeletion: DeleteOptions` (`{ one: 'albarán', Label: 'Albarán' }`, `remove` → `DELETE`, `pendingForDelete` → null). `DeliveryNoteRecord` (no `$app`): props `{ id: number | null; initial: NoteValues; saved?: DeliveryNoteResponse; periods: TaxConfigResponse[]; api functions (create, update, blank date chaining, lookups, proposePrice); goToList; toast }`; `DeliveryNoteRecordPage` wires `$app`, `UnsavedGuard`, list memory (`'delivery-notes'`). `[id]` page wrapped in `{#key data.id}`.

- [ ] **Step 1: Failing tests.**
  - Record spec: header Cliente (code → trade name; "No existe ningún cliente con ese código") and Fecha; format label "Albarán simple"/"Albarán valorado"; totals for a client WITH_VAT/WITH_SURCHARGE with period 4/0,5 and the Task 4 lines (Base 30,99 · IVA 1,24 · Recargo 0,15 · Total 32,38); "No hay IVA configurado para esa fecha" without a period; a saved note shows its frozen rates until date or client change; save of a new note → `create` gets the Task 4 payload shape, toast "Albarán 1045 guardado", blank with the same date, cursor in Cliente; edit → `update` with `code` and the original time, `goToList(id)`; draft banner text; issued banner "Está en la factura nº 23, emitida: no se puede modificar." and "+", F4, F7, F8 do nothing; F8 on a pending saved note asks "¿Borrar el albarán 1045?" with the client name.
  - e2e → `a delivery note by keyboard`: via API ensure a period in force (2026-01-01, 4, 0.5; 409 ok), a client and two articles with fixed codes from the 9401–9449 range (reserved for this file: articles 9300–9320 and clients 9201–9205 are taken by other e2e files), and one earlier note for that client with article A at 1,20; from `/delivery-notes` press F2, type the client code, Enter, today's date stays, Enter, article A code, Enter (price proposal later), lot `01181026`, Enter, quantity `25`, Enter → price shows "1,20", Enter → second line: article B, lot, `3`, price `0,33`, then "+" right away → toast "Albarán N guardado", a blank note with the same date and the cursor in Cliente.
  - e2e → `editing a note keeps it and returns to the list`.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** `new/+page.ts`: tax history (for totals) and `todayIso()`; `[id]/+page.ts`: the note (404 → error page), tax history, its client by id (`GET /api/clients/{id}`) for the regime/format, and seed the lookups cache with the note's client and articles. `RecordForm` with `firstField: 'clientCode'`, `readOnly` when `invoiceStatus === 'ISSUED'`; `blank()` keeps the current date.
- [ ] **Step 4: Run** specs, `npm test`, `npm run check`, `npm run lint`, `npm run test:e2e`.
- [ ] **Step 5: Controller commits** `add the delivery note editor`.

---

### Task 7: Delivery-note list (`/delivery-notes`)

**Files:**
- Create: `src/routes/delivery-notes/{+page.ts,+page.svelte}` (replace the placeholder)
- Modify: `src/lib/records/search.ts` only if needed (it already maps digits to `codeFrom`/`codeTo` and text to `q`)
- Test: `e2e/delivery-notes.e2e.ts`, a small unit test for the row mapping/state text (e.g. `src/lib/deliverynotes/list.ts` + spec)

**Interfaces:**
- Consumes: `ListScreen` `extraActions` + `canDelete` (Task 3), `deliveryNoteDeletion` (Task 6), `invoiceStatus`/`invoiceNumber`/`q` (Task 1).
- Produces: `stateText(note): string` — "Pendiente" / "En factura borrador" / "Facturado nº {invoiceNumber}".

- [ ] **Step 1: Failing tests.**
  - Unit: `stateText` for the three states.
  - e2e → `the list finds by client without accents and filters pending`: notes for clients "Frutería García" (pending) and another client in an issued invoice (create profile — POST, 409 ok —, a client with NIF/address/postal code/city, a note, `POST /api/invoices`, `POST /api/invoices/{id}/issue`); search "garcia" → only García's note, state "Pendiente"; clear; F3 → the issued note disappears, the button reads `Ver todos · F3`; F3 again → back.
  - e2e → `an issued note opens read-only`: open it → the issued banner, no "Guardar" button, "+" does nothing.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** `+page.ts`: `q` / digits via `listQuery(q, 'q')`, `invoiced: pending ? false : undefined`, `sort: ['date,desc', 'code,desc']`, `size: 20`. The page: columns per spec (date with `formatDate` of the date part, importe with `formatMoney`), `extraActions` F3 toggling `?pending=1` (replace history entry), `deletion` with `canDelete: (r) => r.invoiceId == null`, `reload: () => invalidateAll()`, empty text "Todavía no hay albaranes. Pulsa F2 para hacer el primero.", list memory `'delivery-notes'`.
- [ ] **Step 4: Run** `npm test`, `npm run check`, `npm run lint`, `npm run test:e2e`.
- [ ] **Step 5: Controller commits** `add the delivery note list`.

---

### Task 8: Docs and pending items

**Files:**
- Modify: `PENDIENTES.md` §5, `front/README.md` (if a convention changed: compact fields, list extra actions)

- [ ] **Step 1:** PENDIENTES §5: remove the two items this block resolves ("El '+' del pad pulsado justo después de Enter en un `CodeField`…" and "Vacíos mezclados: … lotes opcionales … como `null`"); add a "Pendiente del bloque de albaranes" group with: imprimir y enviar un albarán (bloque del PDF); teclear el número del albarán; duplicar un albarán.
- [ ] **Step 2:** README conventions, one bullet each where they apply: compact fields for grids; `ListScreen.extraActions`.
- [ ] **Step 3: Run** `npm run check`, `npm run lint`, `npm test`, `npm run test:e2e` twice, `./mvnw -q test`.
- [ ] **Step 4: Controller commits** `update pending items for delivery notes`.
