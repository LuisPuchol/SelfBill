# Resumen por cliente Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Facturas → "3 · Resumen por cliente": between two dates, the issued invoices summed per client; for the marked client its invoices; for the marked invoice its delivery notes; F9 prints the marked invoice.

**Architecture:** One new read-only API query (`GET /api/invoices/summary`) aggregates issued invoices per client; the invoices of a client and the notes of an invoice come from the existing `GET /api/invoices` and `GET /api/invoices/{id}`. In the front, the "load the detail of the marked row" logic (cache per key, latest request wins, answers from before a reload dropped) — today written three times in `Traceability.svelte` and `Ledger.svelte` — becomes one `DetailLoader`, used by those two screens and the new one, which follows `Traceability.svelte`'s layout and keys.

**Tech Stack:** Spring Boot 3.5 + Java 25 (`JAVA_HOME=C:\Program Files\Java\jdk-25.0.3`), JPA; SvelteKit, Svelte 5 runes, Tailwind 4 tokens, Vitest 4 browser mode, Playwright e2e on the packaged jar (port 18080).

**Spec:** `docs/superpowers/specs/2026-10-27-resumen-cliente-design.md`

## Global Constraints

- Work on `main` (user's decision); implementers commit their own task on `main` with the Step-5 message plus a blank line and `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; never push.
- API paths relative to `selfbill-api/src/` (`.../` = `java/com/luispuchol/selfbill/selfbill_api/`); front paths relative to `front/`. Inside `src/lib` relative imports; outside `#lib/<path>.js`/`.svelte`; internal href/goto via `resolve()`; one `useKeys`/`keyLayer` per component. API texts only through `classifyError`/`showError`.
- Spanish copy, verbatim: menu tile `Resumen por cliente` (digit 3); screen title `Resumen por cliente`; client columns `Cód.`, `Cliente`, `Fact.`, `Base`, `IVA`, `Recargo`, `Total`; invoice columns `Fecha`, `Nº`, `Base`, `IVA`, `Recargo`, `Total`; note columns as the Ledger's (`Fecha`, `Alb.`, `Importe`); footers `38 clientes · 214 facturas · 160.512,40` / `1 cliente · 1 factura · …`, `5 facturas · 1.068,31` / `1 factura · …`, `4 albaranes · 158,93` / `1 albarán · …`; empty `No hay facturas entre el 27/09/2026 y el 27/10/2026.`; key `F9` label `Imprimir factura`; print failure `No se ha podido imprimir la factura.`
- Money with `formatMoney`, dates with `formatDate`; footer sums in cents (floats drift). Default period `defaultRange(today)` (today − 30 days).
- Never start, stop or bind anything on port 8080 and never touch the user's `selfbill.db`/`~/.selfbill/secret.key`; temp APIs on another port with temp DB + key (see `front/e2e/serve-jar.mjs`). No python. Use the `codegraph_explore` MCP tool before grep/Read.
- e2e: `npm run test:e2e` only while nothing listens on 8080; otherwise `npm run build`, `./mvnw -q package -DskipTests` (no clean), `npx playwright test`.

## Review Focus

1. Moving fast through clients or invoices never shows the invoices or notes of a row already left — Task 2 `drops the answer of a key already left`, Task 3 `a late answer is not shown`.
2. After loading other dates, nothing of the previous period is shown or printed — Task 2 `drops an answer that started before clear`, Task 3 `new dates drop the old panels`.
3. Trazabilidad and the Libro behave exactly as before the refactor — Task 2 runs their specs unchanged plus the e2e.
4. A client's "Fact." count and amounts match the invoices its panel lists (same filter: issued, alive, in the period) — Task 1 `sumsIssuedInvoicesPerClient`, Task 3 e2e.
5. Footer totals do not drift (0,10 + 0,20 → 0,30) — Task 3 `footers sum in cents`.

---

### Task 1: API — `GET /api/invoices/summary` + regenerated types

**Files:**
- Create: `main/java/.../billing/invoice/ClientSummaryResponse.java`
- Modify: `main/java/.../billing/invoice/InvoiceController.java`, `InvoiceService.java`, `InvoiceRepository.java`
- Test: `test/java/.../billing/invoice/InvoiceSummaryTest.java` (`DomainIntegrationTest` + `@AutoConfigureMockMvc`), `test/java/.../OpenApiDocsTest.java`
- Regenerate: `front/src/lib/api/schema.d.ts`

**Interfaces:**
- Produces: `GET /api/invoices/summary?dateFrom&dateTo` (both required `LocalDate`, ISO) → `List<ClientSummaryResponse>`; `record ClientSummaryResponse(Integer clientId, Integer clientCode, String clientName, Integer invoiceCount, BigDecimal subtotal, BigDecimal vatAmount, BigDecimal surchargeAmount, BigDecimal total)`, ordered by `clientCode`. `clientName` is the client's trade name (`Client.name`). Only `ISSUED`, live invoices whose `date` is in `[dateFrom 00:00, dateTo + 1 day 00:00)`; a deleted client still appears.

- [ ] **Step 1: Failing tests** in `InvoiceSummaryTest` (set each invoice's issue date with `jdbc` after issuing; period 2026-04-01..2026-04-30):
  - `sumsIssuedInvoicesPerClient`: client 2 with two issued invoices (2026-04-03, 2026-04-30T18:00) and client 1 with one → `[{clientCode 1, invoiceCount 1, …}, {clientCode 2, invoiceCount 2, subtotal/vatAmount/surchargeAmount/total = sums}]`, with `clientId` and the trade name.
  - `leavesOutDraftsDeletedAndOtherDates`: a draft, a deleted issued invoice, one issued 2026-03-31T23:59 and one 2026-05-01T00:00 → none counted (a client with only those has no row).
  - `bothSeries`: a WITHOUT_VAT client's S invoice counts, with `vatAmount` 0.
  - `deletedClientStillShows`: delete the client after issuing → its row is there.
  - `requiresBothDates`: without `dateFrom` or `dateTo` → 400 with `$.errorCode` `VALIDATION_ERROR`.
  - `OpenApiDocsTest`: lists `/api/invoices/summary`.
- [ ] **Step 2: Run, see them fail.** `cd selfbill-api && ./mvnw -q test -Dtest='InvoiceSummaryTest,OpenApiDocsTest'`
- [ ] **Step 3: Implement.** One grouped JPQL query in `InvoiceRepository` (a constructor expression or a projection; `Invoice` already filters deleted rows, `Client` does not), the `@GetMapping("/summary")` with an `@Operation` that says only issued invoices count and the dates are inclusive. Then `./mvnw -q test`.
- [ ] **Step 4: Regenerate the front types** on port **18081** with a temp DB + key (as `e2e/serve-jar.mjs` does), `npx -y openapi-typescript@7.13.0 http://127.0.0.1:18081/v3/api-docs -o src/lib/api/schema.d.ts` from `front/`; stop that PID; delete its temp files; `npm run check` passes.
- [ ] **Step 5: Commit** `add the client summary query`.

---

### Task 2: `DetailLoader` and the shared notes table; Trazabilidad and the Libro use them

**Files:**
- Create: `src/lib/components/detail-loader.svelte.ts` (+ `detail-loader.svelte.spec.ts`), `src/lib/invoices/sections.ts`
- Modify: `src/lib/traceability/Traceability.svelte`, `src/lib/invoices/Ledger.svelte`; `front/README.md` (one bullet next to the other shared pieces)

**Interfaces:**
- Produces:
  ```ts
  export class DetailLoader<K extends string | number, T> {
      constructor(load: (key: K) => Promise<T>, onError: (err: unknown) => void);
      /** Points at a key, or none: its cached detail at once, else asks for it. */
      select(key: K | undefined): void;
      /** The detail on screen if it is `key`'s, else undefined (loading, or another key). */
      get(key: K | undefined): T | undefined;
      /** Forgets every answer: one to a request made before is neither cached nor shown. */
      clear(): void;
  }
  ```
  Only the latest `select` shows its answer or reports its error (`onError` once, for the latest request only); answers are cached by key until `clear()`. A component drives it with `$effect(() => { const k = markedKey; untrack(() => loader.select(k)); })` and reads `loader.get(markedKey)` in a `$derived`.
- Produces: `src/lib/invoices/sections.ts` — `export type Section = NonNullable<components['schemas']['InvoiceResponse']['sections']>[number]` and `export const sectionColumns: Column<Section>[]` (the Ledger's `noteColumns`, moved unchanged).

- [ ] **Step 1: Failing tests** in `detail-loader.svelte.spec.ts` (loads are controllable deferred promises):
  - `shows the answer of the selected key`: `select(1)` → `get(1)` undefined until it resolves, then the value; `get(2)` undefined.
  - `serves a cached key without asking again`: `select(1)`, resolve, `select(2)`, `select(1)` → `get(1)` at once; `load` called once for 1.
  - `drops the answer of a key already left`: `select(1)`, `select(2)`, resolve 1 → `get(1)` and `get(2)` undefined; resolve 2 → `get(2)` value; then `select(1)` uses the cache (the late answer was still cached).
  - `drops an answer that started before clear`: `select(1)`, `clear()`, resolve 1 → not shown and not cached (`select(1)` asks again).
  - `reports only the latest error`: `select(1)` rejects after `select(2)` → `onError` not called; `select(2)` rejects → called once.
  - `no key, no detail`: `select(undefined)` → `get(undefined)` undefined, no load.
- [ ] **Step 2: Run, see them fail.** `cd front && npx vitest run src/lib/components/detail-loader.svelte.spec.ts`
- [ ] **Step 3: Implement** the class (`$state.raw` for what is shown; a plain `Map` cache; a sequence number for "latest" and a generation number for `clear`). Then move Traceability's lots and deliveries and the Ledger's sections onto it and the Ledger's `noteColumns` to `sectionColumns`, without changing what either screen does (Traceability still puts the lot mark back on the first lot when the article changes).
- [ ] **Step 4: Run** the loader spec, then `npx vitest run src/lib/traceability src/lib/invoices` with **no change to those specs**, then `npm test`, `npm run check`, `npm run lint`, and the e2e (`traceability.e2e.ts`, `invoices.e2e.ts` at least) per Global Constraints.
- [ ] **Step 5: Commit** `share the detail loading of Trazabilidad and the Libro`.

---

### Task 3: The "Resumen por cliente" screen, route, menu tile and e2e

**Files:**
- Create: `src/lib/invoices/ClientSummary.svelte` (+ `ClientSummary.svelte.spec.ts`, harness `src/lib/test/ClientSummaryHarness.svelte`), `src/routes/invoices/summary/{+page.svelte,+page.ts}`, `e2e/summary.e2e.ts`
- Modify: `src/lib/invoices/InvoicesMenu.svelte` (+ spec)

**Interfaces:**
- Consumes: Task 1's `ClientSummaryResponse` type; Task 2's `DetailLoader` and `sectionColumns`; `printDocument`, `defaultRange`, `DataTable` (`active`), `StaticTable`.
- Produces: `ClientSummary` props (with `type ClientTotals = components['schemas']['ClientSummaryResponse']`, `type Invoice = components['schemas']['InvoiceResponse']`) `{ today: string; loadSummary: (from: string, to: string) => Promise<ClientTotals[]>; loadInvoices: (clientId: number, from: string, to: string) => Promise<Invoice[]>; loadInvoice: (id: number) => Promise<Invoice>; fetchPdf: (id: number) => Promise<Blob>; print: (pdf: Blob) => Promise<void>; onExit: () => void }`. The page wires `loadInvoices` to `GET /api/invoices` with `{ clientId, status: 'ISSUED', dateFrom, dateTo, size: LEDGER_LIMIT, sort: ['date,asc', 'id,asc'] }` and the rest as the Ledger's page does; `onExit` goes to `/invoices`; `+page.ts` returns `{ today: todayIso() }`.
- Behaviour: spec §4.2, modelled on `Traceability.svelte` (columns `'clients' | 'invoices'`; the notes panel is a `StaticTable` that only reads; the invoices' `DetailLoader` keyed by client id and cleared on every load; the notes' keyed by invoice id). F9 is enabled while an invoice is marked, in any column.

- [ ] **Step 1: Failing tests** in `ClientSummary.svelte.spec.ts` (copy the Traceability spec's setup and helpers):
  - `loads the default period on entry` (from `defaultRange(today)`; cursor in "Desde", selected) and `Enter, Enter loads and goes to the clients`.
  - `the panels follow the marked rows`: ArrowDown in clients → the second client's invoices; in invoices, ArrowDown → that invoice's notes.
  - `a late answer is not shown`: the first client's invoices resolve after moving to the second → not shown.
  - `Enter and Esc move between columns`: clients Enter → invoices active; Esc → clients; Esc → "Desde"; Esc in the dates → `onExit`.
  - `new dates drop the old panels`: editing a date empties the three panels; loading other dates and a late invoices answer from the old period is not shown.
  - `footers` with the exact texts (`2 clientes · 3 facturas · …`, `1 cliente · 1 factura · …`, `1 factura · …`, `1 albarán · …`) and `footers sum in cents` (0,10 + 0,20 → `0,30`).
  - `empty period` → `No hay facturas entre el … y el ….`, cursor stays in "Hasta".
  - `F9 prints the marked invoice` (`fetchPdf(id)` then `print(blob)`), disabled with no invoice marked; `F9 failure` → `No se ha podido imprimir la factura.`
  - `InvoicesMenu`: three tiles; `3` opens `/invoices/summary`; the key bar label `1–3`.
  - `e2e/summary.e2e.ts` (codes 9601–9649 for clients/articles/notes so it does not collide with other e2e data; invoices issued through the API today, so they fall in the default period): two clients with 2 and 1 invoices → their rows (found by code) with `Fact.` 2 and 1 and their totals; marking the first shows its two invoices; marking an invoice shows its notes; F9 calls print (stub `window.print` as `invoices.e2e.ts` does).
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** focused specs, `npm test`, `npm run check`, `npm run lint`, the full e2e per Global Constraints.
- [ ] **Step 5: Commit** `add the client summary screen`.

---

### Task 4: Docs, verification, push

Runs after the final review and its fix wave.

- [ ] **Step 1: Docs.** PENDIENTES: a "Pendiente del bloque de resumen por cliente" section with what the final review parked; update the Facturas note "Facturar y el Libro repiten la misma lógica de lista… extraerla si llega una tercera pantalla así" — the detail loading is shared now, the list/date logic is repeated in four screens (Facturar, Libro, Trazabilidad, Resumen). Seguimiento de cobro stays in PENDIENTES, now pointing at this screen as where cobros would show.
- [ ] **Step 2: Verify.** `./mvnw -q test`; `npm test`, `npm run check`, `npm run lint`; e2e twice.
- [ ] **Step 3: Boot check.** API from `target/classes` with the Maven classpath on a free port ≠ 8080, temp DB and key; create profile, tax config, client, article, a note, issue an invoice; `GET /api/invoices/summary?dateFrom=<today−1>&dateTo=<today>` → one row with `invoiceCount` 1; stop it; delete the temp files.
- [ ] **Step 4: Commit** `update docs for the client summary` and push `main`.
