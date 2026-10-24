# Trazabilidad Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Section "5 · Trazabilidad": between two dates, the articles sold; for the marked article its lots with quantities; for the marked lot the delivery notes (date, client, note, quantity), printable as a listing.

**Architecture:** Three read-only API queries under `/api/traceability` aggregate live delivery-note lines (all three lot slots count). The front screen follows `src/lib/invoices/Ledger.svelte` (dates, load-on-entry, latest-wins panels) with two interactive `DataTable`s — only the active one answers the keys — and a read-only `StaticTable`; F9 prints an HTML listing through the same hidden print frame as the albarán.

**Tech Stack:** Spring Boot 3.5 + Java 25 (`JAVA_HOME=C:\Program Files\Java\jdk-25.0.3`), JPA; SvelteKit 3, Svelte 5 runes, Tailwind 4 tokens, Vitest 4 browser mode, Playwright e2e on the packaged jar (port 18080).

**Spec:** `docs/superpowers/specs/2026-10-24-trazabilidad-design.md`

## Global Constraints

- Work on `main` (user's decision); implementers commit their own task on `main` with the Step-5 message plus a blank line and `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; never push.
- Front paths relative to `front/`; API paths relative to `selfbill-api/src/`. Inside `src/lib` relative imports; outside `#lib/<path>.js`/`.svelte`; internal href/goto via `resolve()`; one `useKeys`/`keyLayer` per component; "+" is `NumpadAdd`. Spanish UI texts exactly as the spec; English code and comments matching the surrounding comment density.
- Quantities with `formatDecimal(value, 2)` ("151,00"), dates with `formatDate`; API error texts only through `classifyError`/`showError`.
- Never start, stop or bind anything on port 8080 and never touch the user's `selfbill.db`/`~/.selfbill/secret.key`; temp APIs on another port with temp DB + key (see `front/e2e/serve-jar.mjs`). No python. Use the `codegraph_explore` MCP tool before grep/Read.
- e2e: `npm run test:e2e` only while nothing listens on 8080; otherwise `npm run build`, `./mvnw -q package -DskipTests` (no clean), `npx playwright test`.

## Review Focus

1. A line whose lot is in slot 2 or 3 must be found, or a recall misses deliveries — Task 1 `lotsCountEverySlot`, Task 3 e2e.
2. Moving fast through articles or lots must never show the lots/notes of a row already left — Task 3 `a late answer is not shown`.
3. Only the active column answers ↑↓ and Enter; the other keeps its mark — Task 2 `an inactive table ignores the keys`, Task 3 `Enter and Esc move between columns`.
4. Printing must never show a client name as markup — Task 2 `escapes names`.
5. Dates edited but not loaded must not be printed — Task 3 `editing a date empties the columns`.

---

### Task 1: API — `/api/traceability/{articles,lots,deliveries}` + regenerated types

**Files:**
- Create: `main/java/.../billing/traceability/{TraceabilityController,TraceabilityService,TraceabilityArticleResponse,TraceabilityLotResponse,TraceabilityDeliveryResponse}.java` (a repository or JPQL queries as the package needs)
- Test: `test/java/.../billing/traceability/TraceabilityTest.java` (set up like `InvoicePendingTest`), `OpenApiDocsTest.java`
- Regenerate: `front/src/lib/api/schema.d.ts`

**Interfaces:**
- Produces: `GET /api/traceability/articles?dateFrom&dateTo` → `TraceabilityArticleResponse[]` (`articleId: Integer, articleCode: Integer, articleName: String`), by `articleCode`.
- Produces: `GET /api/traceability/lots?articleId&dateFrom&dateTo` → `TraceabilityLotResponse[]` (`code: String, quantity: BigDecimal`), by `code` (text order).
- Produces: `GET /api/traceability/deliveries?articleId&code&dateFrom&dateTo` → `TraceabilityDeliveryResponse[]` (`deliveryNoteId: Integer, deliveryNoteCode: Integer, date: LocalDateTime, clientId: Integer, clientCode: Integer, clientName: String` (trade name), `quantity: BigDecimal`), by `date` then `deliveryNoteCode`.
- All params required; dates `LocalDate`, inclusive whole days; `code` validated with `DeliveryNoteLineRequest.TRACEABILITY_CODE_PATTERN`/`_MESSAGE`.

- [ ] **Step 1: Failing tests** in `TraceabilityTest` (period 2026-04-01..2026-04-30 unless stated):
  - `articlesSoldInThePeriod`: articles with codes 20 and 10 sold in the period, one sold only on 2026-05-01, one only in a deleted note → `[10, 20]` with names; a note at 2026-04-30T18:00 counts.
  - `lotsCountEverySlot`: article A lines — L1 `code1=02010426` qty 5; L2 `code1=02010426, code2=03020426` qty 3; L3 `code1=03020426, code3=03020426` qty 2 (same lot twice) → `[{02010426, 8}, {03020426, 5}]`; a line of another article with `02010426` is not counted.
  - `deliveriesOfALot`: two notes of different clients with lot `02010426` for A (one note with two such lines, 4 + 6) and a third note with another lot → two rows, by date then note code, the first `quantity` 10, with `clientCode` and trade name; a line with the lot in slot 2 also brings its note.
  - `leavesOutDeletedLinesAndNotes`: a deleted note and a line removed by editing a note do not count in any of the three.
  - `requiresItsParameters`: each endpoint without one parameter → 400; `code=123` → 400.
  - `OpenApiDocsTest`: lists the three paths.
- [ ] **Step 2: Run, see them fail.** `cd selfbill-api && ./mvnw -q test -Dtest='TraceabilityTest,OpenApiDocsTest'`
- [ ] **Step 3: Implement.** One query per endpoint over live lines (`DeliveryNoteLine` has `@SQLRestriction`; check that deleted notes are excluded too — the test pins it), loading what the rows print without one query per row; aggregate the three slots in Java (distinct codes per line). `@Operation` texts say all three slots count and the dates are inclusive. Then `./mvnw -q test`.
- [ ] **Step 4: Regenerate the front types** on port **18081** with a temp DB + key (as `e2e/serve-jar.mjs` does), `npx -y openapi-typescript@7.13.0 http://127.0.0.1:18081/v3/api-docs -o src/lib/api/schema.d.ts` from `front/`; stop that PID; delete its temp files; `npm run check` passes.
- [ ] **Step 5: Commit** `add traceability queries`.

---

### Task 2: Front pieces — `DataTable.active`, `printDocument`, shared default period, the printed listing

**Files:**
- Modify: `src/lib/components/DataTable.svelte` (+ spec)
- Modify: `src/lib/print.ts` (+ `print.svelte.spec.ts`), its callers (`DeliveryNoteRecordPage.svelte`, the ledger page, any spec/harness that names it)
- Modify: `src/lib/format/date.ts` (+ spec), `src/lib/invoices/ledger.ts` (+ spec) and the Ledger's imports
- Create: `src/lib/traceability/report.ts` (+ `report.spec.ts`)

**Interfaces:**
- Produces: `DataTable` prop `active?: boolean` (default `true`): when false its ↑↓/Enter actions are absent (`when` false) and the selected row is drawn in a quieter style; clicks still select.
- Produces: `printDocument(doc: Blob): Promise<void>` — today's `printPdf`, renamed because it prints any document the browser renders (PDF or HTML); same behaviour, timeout and frame lifetime. Error text when its load times out mentions the document, not "PDF".
- Produces: `defaultRange(today: string): { from: string; to: string }` moved from `src/lib/invoices/ledger.ts` to `src/lib/format/date.ts` unchanged (today − 30 days); the Ledger imports it from there; its tests move with it.
- Produces: `traceabilityReport(input: { code: string; articleName: string; from: string; to: string; rows: { date: string; clientCode: number; clientName: string; deliveryNoteCode: number; quantity: number }[] }): string` — a complete HTML document (`<!doctype html>`, `<meta charset="utf-8">`, `<title>Trazabilidad 02200524</title>`, inline print CSS: A4 margins, system font) with the heading "Trazabilidad 02200524 ESPINACA BOLSA 250GR" and "Total cantidad 151,00", the line "Del 24/09/2026 al 24/10/2026", and a table Fecha / Cliente (code then name) / Nº albarán / Cantidad (numbers right-aligned, `formatDecimal(…, 2)`). Every interpolated text is HTML-escaped.

- [ ] **Step 1: Failing tests:**
  - `DataTable`: `an inactive table ignores the keys` — `active={false}`: ArrowDown and Enter change nothing and `onOpen` (a spy) is never called; switching to `active={true}` makes ArrowDown move.
  - `print.svelte.spec.ts`: the existing specs run against `printDocument`; one more prints an HTML blob (`new Blob(['<p>x</p>'], { type: 'text/html' })`) with `print` stubbed → called once.
  - `date.spec.ts`: the four `defaultRange` cases from `ledger.spec.ts` (moved).
  - `report.spec.ts`: `builds the listing` (title, total "151,00" as the sum of the rows, "Del 24/09/2026 al 24/10/2026", one `<tr>` per row with "20/05/2026", "6", the name, "31594", "20,00"); `escapes names` (`clientName: '<b>A&B</b>'` → `&lt;b&gt;A&amp;B&lt;/b&gt;`, no `<b>` in the output); `zero rows` → total "0,00".
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** focused specs, `npm test`, `npm run check`, `npm run lint`.
- [ ] **Step 5: Commit** `add DataTable active state, printDocument and the traceability listing`.

---

### Task 3: Trazabilidad screen + e2e

**Files:**
- Create: `src/lib/traceability/Traceability.svelte` (+ `Traceability.svelte.spec.ts`, harness `src/lib/test/TraceabilityHarness.svelte`), `src/routes/traceability/+page.ts`, `e2e/traceability.e2e.ts`
- Modify: `src/routes/traceability/+page.svelte` (the screen instead of `SectionPlaceholder`)

**Interfaces:**
- Consumes: Task 1 schemas (`TraceabilityArticleResponse`, `TraceabilityLotResponse`, `TraceabilityDeliveryResponse`); Task 2 `DataTable.active`, `printDocument`, `defaultRange`, `traceabilityReport`; `StaticTable` (`scroll`).
- Produces (`Traceability.svelte`): props `{ today: string; loadArticles(from: string, to: string): Promise<TraceabilityArticleResponse[]>; loadLots(articleId: number, from: string, to: string): Promise<TraceabilityLotResponse[]>; loadDeliveries(articleId: number, code: string, from: string, to: string): Promise<TraceabilityDeliveryResponse[]>; print(doc: Blob): Promise<void>; onExit(): void }`. The page wires the three GETs through `request`, `printDocument` and `onExit` → `/`; `+page.ts` gives `today` (`todayIso()`).
- Keys: `F9` "Imprimir", `Escape` "Volver" (lots → articles → Desde → `onExit`), `Enter` in articles → lots; `ArrowUp/Down` belong to the active `DataTable`.

- [ ] **Step 1: Failing tests** (`Traceability.svelte.spec.ts`, today `2026-10-24`, viewport 1280×800 like `Ledger.svelte.spec.ts`):
  - `loads the default period on entry`: `loadArticles('2026-09-24','2026-10-24')`; cursor in Desde, selected; rows Cód./Artículo; the first article's lots (`loadLots(id, from, to)`) and the first lot's notes (`loadDeliveries(id, code, from, to)`) shown with "151,00"-style quantities and the footer "2 albaranes · 30,00" (singular "1 albarán · 20,00").
  - `Enter, Enter reloads and moves to the articles` with typed dates; an empty answer shows "No se ha vendido nada entre el 01/04/2026 y el 30/04/2026." and keeps the cursor in Hasta with its text selected.
  - `the columns follow the marked rows`: ArrowDown in articles → lots of the second article; Enter → lots active, ArrowDown → notes of the second lot; the articles' mark stays.
  - `a late answer is not shown`: the first article's `loadLots` resolves after the second's → the second's lots are shown; same for `loadDeliveries`.
  - `Enter and Esc move between columns`: in lots, ArrowDown does not move the articles; Esc → articles active; Esc → Desde focused and selected; Esc → `onExit`. A click on a lot row marks it and activates the lots column.
  - `editing a date empties the columns`: after loading, typing in Desde empties the three columns and greys F9 until Enter.
  - `F9 prints the marked lot`: `print` gets a `text/html` Blob whose text contains "Trazabilidad <code> <article name>", "Total cantidad 30,00" and the period; `print` rejecting → dialog "No se ha podido imprimir el listado."; with no lot F9 is greyed and `print` not called.
  - `e2e/traceability.e2e.ts` (codes 9501–9549; tax config from 2026-01-01 as the other files; notes dated in **2026-04**, a month no other e2e file uses — check with grep): one article, two clients; note 1 with a line lot1 `02010426` qty 5; note 2 with a line `code1=03020426, code2=02010426` qty 3 → "5" from home, type `010426` Enter `300426` Enter; the article is listed; its lots show `02010426` 8,00 and `03020426` 3,00; Enter, ArrowDown to `03020426` → one note of client 2 with 3,00; F9 with `window.print` stubbed in every frame (as `delivery-notes.e2e.ts` does) → printed once.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** Follow `Ledger.svelte`: FormNav dates with Enter via `onLast`, a load counter for the articles, latest-wins counters (and a cache keyed by article / article+lot, cleared when the articles reload) for lots and notes, index restore after `DataTable` resets, a click inside a column activates it. Articles and lots are `DataTable`s with `scroll`, `active` and no `onOpen`; the notes are a `StaticTable` with `scroll`. The articles' Enter is the screen's own key action (`when` the articles column is active and there are lots).
- [ ] **Step 4: Run** specs, `npm test`, check, lint, e2e.
- [ ] **Step 5: Commit** `add the traceability screen`.

---

### Task 4: Docs, verification, push (after the final review's fix wave)

- [ ] **Step 1:** `CONCEPTO.md` §10: one paragraph — the Trazabilidad screen answers the inspection query with `/api/traceability/{articles,lots,deliveries}`; the three slots count, and a line with two lots counts its whole quantity in both. `PENDIENTES.md` §5: group "Pendiente del bloque de trazabilidad" with the out-of-scope items of the spec (buscar por lote sin artículo, abrir el albarán desde el listado, exportar a CSV) and any minors left by the reviews. `front/README.md`: one bullet for `DataTable.active` and `printDocument` if the conventions list covers such pieces.
- [ ] **Step 2: Verify:** check, lint, `npm test`, `npm run test:e2e` twice, `./mvnw -q test`; boot the API from `target/classes` on a temp port with temp DB/key, seed one article/client/note and hit the three endpoints (expected rows), and one without parameters → 400; stop it by PID.
- [ ] **Step 3: Commit** `update docs and pending items for traceability`, then push `main`.
