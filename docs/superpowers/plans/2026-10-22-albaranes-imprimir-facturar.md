# Albaranes: totales arriba, imprimir y emitir factura — plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In the delivery-note editor, totals sit fixed at the top, and two unchecked-by-default boxes ("Imprimir albarán · F9", "Emitir factura · F3") print the note and/or create+issue an invoice for it right after saving.

**Architecture:** One atomic API endpoint (`POST /api/invoices/issue`); a `RecordForm.afterSave` hook; a small `printPdf` helper (hidden iframe + `print()`); a pure `invoiceBlocker` for the "Emitir factura" reasons; a `CheckAction` toggle built from a `KeyAction` like `ActionButton`; the editor wires them.

**Tech Stack:** Spring Boot 3.5 + Java 25 (`JAVA_HOME`), MapStruct; SvelteKit 3, Svelte 5 runes, Vitest 4 browser mode, Playwright e2e on the packaged jar (port 18080).

**Spec:** `docs/superpowers/specs/2026-10-22-albaranes-imprimir-facturar-design.md`

## Global Constraints

- Work happens on `main` (user's decision): implementers never commit or stage; the controller commits each task on `main` after review.
- Front paths relative to `front/`; API paths relative to `selfbill-api/src/`. Inside `src/lib` relative imports; outside `#lib/<path>.js`/`.svelte`; internal href/goto via `resolve()`; one `useKeys`/`keyLayer` per component; "+" is `NumpadAdd`. Spanish UI texts exactly as the spec; English code/comments.
- Keys: `F9` "Imprimir albarán", `F3` "Emitir factura" (both already in `ALWAYS_PREVENTED`; check `keys.ts`).
- Never start, stop or bind anything on port 8080 and never touch the user's `selfbill.db`/`~/.selfbill/secret.key`; temp APIs on another port with temp DB + key (see `front/e2e/serve-jar.mjs`). The IDE no longer compiles into `target/` (autobuild off), so `./mvnw -q test` is reliable; no python.

## Review Focus

1. Issuing fails (client data missing, profile missing, note already invoiced): the note stays saved and pending, no invoice exists, the user sees why (Task 1 + Task 3 tests).
2. The boxes start unchecked on every note, including the next one of a chain (Task 3 test).
3. Print happens after save (and after issuing), never for an unsaved note; a print failure doesn't lose the save (Task 3 test).
4. In an issued (read-only) note the boxes do nothing (F9/F3/click) (Task 3 test).
5. F9/F3 never type into a field or move the cursor (Task 2/3 tests).

---

### Task 1: `POST /api/invoices/issue` (API) + regenerated types

**Files:** `main/java/.../billing/invoice/{InvoiceController,InvoiceService}.java`; test `test/java/.../billing/invoice/InvoiceIssueNowTest.java` (new; setup like `InvoiceLifecycleTest`/`InvoiceRequirementsTest`), `OpenApiDocsTest.java`; `front/src/lib/api/schema.d.ts` (regenerated).

**Interfaces:** `InvoiceService.createAndIssue(InvoiceRequest): InvoiceResponse` (`@Transactional`: `createInvoice` then `issueInvoice` logic in one transaction); `POST /api/invoices/issue` → 201 with the issued `InvoiceResponse`; `@ApiErrors` listing the create and issue errors.

- [ ] **Step 1: Failing tests:** `issuesAnInvoiceWithItsNumber` (note pending → response `status ISSUED`, `number` "1" (or next), note now has `invoiceStatus ISSUED`); `missingClientDataCreatesNothing` (client without NIF → `CLIENT_FISCAL_DATA_MISSING`, invoice count unchanged, note still has no invoice); `missingProfileCreatesNothing` (`USER_PROFILE_NOT_FOUND`, same checks); `alreadyInvoicedNoteCreatesNothing` (`INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED`); OpenAPI lists `/api/invoices/issue`.
- [ ] **Step 2: Run, see them fail.** `./mvnw -q test -Dtest='InvoiceIssueNowTest,OpenApiDocsTest'`.
- [ ] **Step 3: Implement**, then `./mvnw -q test`.
- [ ] **Step 4: Regenerate the front types** on **18081** with temp DB + key (like `e2e/serve-jar.mjs`), `npx -y openapi-typescript@7.13.0 http://127.0.0.1:18081/v3/api-docs -o src/lib/api/schema.d.ts`; stop that PID; `npm run check`.
- [ ] **Step 5: Controller commits** `add create-and-issue invoice endpoint`.

---

### Task 2: `RecordForm.afterSave`, `printPdf`, `invoiceBlocker`, `CheckAction`

**Files:** `src/lib/records/record-form.svelte.ts`; `src/lib/print.ts` (+ `print.spec.ts`); `src/lib/deliverynotes/invoice-blocker.ts` (+ spec); `src/lib/components/CheckAction.svelte` (+ spec, harness in `src/lib/test/`); `RecordScreen.svelte.spec.ts` / `RecordHarness.svelte` for the hook test.

**Interfaces:**
- `RecordFormOptions.afterSave?(saved: Saved): Promise<string | void>` — runs after a successful create/update, before the toast and the chain/return; a returned string is appended to the toast as `"<Label> <code> guardado · <string>"`; if it throws, the error goes to `showError` and the flow continues (toast without suffix, chain/return as usual).
- `printPdf(pdf: Blob): Promise<void>` — hidden `iframe` with an object URL of the blob; on load call `iframe.contentWindow.print()`; remove the iframe and revoke the URL afterwards; rejects if loading fails.
- `invoiceBlocker(input: { client: { nif?: string|null; address?: string|null; postalCode?: string|null; city?: string|null } | null; hasProfile: boolean; inInvoice: boolean }): { enabled: boolean; reason: string | null }` — no client → `{ enabled: false, reason: null }`; missing fields → reason `"Faltan datos del cliente para facturar: "` + the missing names in Spanish joined like "NIF y código postal" / "NIF, dirección y población" (names: NIF, dirección, código postal, población); no profile → `"Faltan los datos del titular (Ajustes)"`; `inInvoice` → `"Ya está en una factura"`; precedence: inInvoice, then client data, then profile.
- `CheckAction` props `{ action: Pick<KeyAction, 'key'|'label'|'run'|'when'>; checked: boolean; enabled?: () => boolean; reason?: string | null }` — a large toggle (`role="checkbox"`, `aria-checked`), label + key chip (`keyLabel`), `tabindex="-1"`, mousedown prevented (never takes focus), greyed via `aria-disabled` with the click ignored, `reason` shown under it in muted text.

- [ ] **Step 1: Failing tests:** `afterSave` runs once after create and after update, before the toast; its string is appended; a throw shows the error and the chain still happens; `printPdf` with `print` stubbed: an iframe appears, `print` called once, iframe removed; `invoiceBlocker` cases incl. the joined Spanish names; `CheckAction`: click and the key toggle via `run`, greyed ignores click, keeps focus in a field, reason shown, `aria-checked` follows `checked`.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** focused specs, `npm test`, check, lint.
- [ ] **Step 5: Controller commits** `add afterSave hook, printPdf, invoice blocker and CheckAction`.

---

### Task 3: Editor wiring (totals at the top, boxes, save → issue → print) + e2e

**Files:** `src/lib/deliverynotes/{DeliveryNoteRecord,DeliveryNoteRecordPage,Totals}.svelte`, `src/routes/delivery-notes/{new,[id]}/+page.ts` (load profile existence: `GET /api/user-profile`, 404 → false), `DeliveryNoteRecord.svelte.spec.ts`, `src/lib/test/DeliveryNoteHarness.svelte`, `e2e/delivery-notes.e2e.ts`.

**Interfaces:** consumes Task 1 (`POST /api/invoices/issue`), Task 2 (`afterSave`, `printPdf`, `invoiceBlocker`, `CheckAction`). `DeliveryNoteRecord` gains props `hasProfile: boolean`, `issueInvoice(clientId: number, noteId: number): Promise<{ number: string }>`, `fetchPdf(noteId: number): Promise<Blob>`, `print(pdf: Blob): Promise<void>` (the page passes `printPdf`; the harness a spy). Keys in the editor's existing `useKeys`: F9 toggles "Imprimir albarán", F3 toggles "Emitir factura" (`when`: not read-only; F3 also only when the blocker allows).

- [ ] **Step 1: Failing tests** (record spec): totals render in the header area above the grid (DOM order) and don't change position when lines are added (assert the totals element precedes the grid); F9 and F3 toggle like a click; both unchecked on open, on a new note and on the next note of a chain; greyed + reason for a client without NIF/postal code ("Faltan datos del cliente para facturar: NIF y código postal"), without profile, and for a note already in an invoice; read-only note: F9/F3/clicks do nothing; save with both checked → `create` then `issueInvoice(clientId, id)` then `fetchPdf(id)` then `print` in that order, toast "Albarán 1045 guardado · Factura 23 emitida"; `issueInvoice` rejecting `ApiError(400,'CLIENT_FISCAL_DATA_MISSING',…)` → error dialog text starts "El albarán se ha guardado, pero no se ha podido emitir la factura:", toast "Albarán 1045 guardado", chain continues; `fetchPdf` rejecting → dialog "No se ha podido imprimir el albarán"; edit with "Imprimir" → print then back to the list.
  - e2e → `issuing on save leaves the note invoiced`: profile (POST, 409 ok) + a client with full fiscal data + articles (codes 9401–9449 free ones), F2 from the list, type a note, F3, "+" → toast contains "Factura", back to the list (Esc) → its row says "Facturado nº"; e2e → `printing on save calls print` with `print` stubbed through `page.addInitScript` in every frame (count calls on `window.top`).
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** The editor's `afterSave(saved)`: if "Emitir" → `issueInvoice` (on error: `showError` with the prefixed message — build it from `messages.ts`' text for the code — and no suffix); if "Imprimir" → `fetchPdf` + `print` (on error: dialog); return "Factura N emitida" when issued. Reset both boxes when the form resets to a blank note and on open. The `[id]` page passes `inInvoice` from `saved.invoiceId != null`. `fetchPdf` in the page: `api.GET('/api/delivery-notes/{id}/pdf', { params: { path: { id } }, parseAs: 'blob' })` through `request`.
- [ ] **Step 4: Run** specs, `npm test`, check, lint, `npm run test:e2e`.
- [ ] **Step 5: Controller commits** `print and issue an invoice when saving a delivery note; totals at the top`.

---

### Task 4: Docs, verification, push

- [ ] **Step 1:** `PENDIENTES.md` §5 albaranes group: add "Imprimir sin diálogo: arrancar Edge con `--kiosk-printing` en el acceso directo de la app." and remove nothing else. README: one bullet for `RecordForm.afterSave` and `CheckAction` if the conventions list covers such pieces.
- [ ] **Step 2: Verify:** check, lint, `npm test`, `npm run test:e2e` twice, `./mvnw -q test`; scan `target/classes` for broken classes; boot the API from `target/classes` on a temp port with temp DB/key and hit `POST /api/invoices/issue` with an empty body (expect 400, not 404/500) and `GET /api/delivery-notes`.
- [ ] **Step 3: Controller commits** `update pending items for printing` and pushes `main`.
