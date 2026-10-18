# Botones de las acciones principales: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every main action of the catalog list and record screens gets a big on-screen button next to its key, records can be deleted from the list, and `npm run dev` without the API shows "Sin conexión".

**Architecture:** A shared `ActionButton` renders a `KeyAction` (same `run` as the key) and never takes the focus; `ScreenHeader` puts the title and the buttons on one line. The delete question moves out of `RecordForm` into `DeleteFlow` + `DeleteDialog`, shared by `RecordScreen` and `ListScreen`. The page row (text, keys, buttons) moves from `DataTable` to `ListScreen`.

**Tech Stack:** SvelteKit 3 (no `svelte.config.js`; config in `vite.config.ts`), Svelte 5 runes, Tailwind 4 `@theme` tokens (`src/routes/layout.css`), Vitest 4 browser mode (`vitest/browser`, `vitest-browser-svelte`), Playwright e2e against the packaged jar (port 18080, temp DB).

**Spec:** `docs/superpowers/specs/2026-10-18-front-botones-design.md`

## Global Constraints

- Paths below are relative to `front/` unless they start with `docs/` or `PENDIENTES.md`.
- Inside `src/lib`, imports are relative (`'../keyboard/keymap.svelte'`); outside it, `#lib/<path>.js` and `#lib/<path>.svelte`.
- Every internal `href`/`goto` goes through `resolve()` from `$app/paths`.
- One `useKeys`/`keyLayer` per component.
- Button texts, exactly: "Nuevo", "Abrir", "Borrar", "Página anterior", "Página siguiente", "Guardar", "Volver". The key chip uses `keyLabel()` from `src/lib/keyboard/keys.ts` ("+", "Esc", "AvPág", "RePág", "F2", "F8", "Enter").
- Buttons at least 48 px tall (`h-12`), base text size, never icon-only.
- Colours only from the theme tokens: `accent`/`accent-ink` (primary), `danger` (delete), `line`, `surface`, `canvas`, `muted`, `ink`.
- Implementers never commit or stage; the controller commits each task after review.
- Never touch port 8080 (the user's API) nor the user's `selfbill.db`/`~/.selfbill/secret.key`. `npm run test:e2e` builds the jar and runs it on 18080 with a temp DB. No python on this machine.
- Code and comments in English; UI texts and `PENDIENTES.md` in Spanish.

## Review Focus

1. Clicking a button while a text field has the cursor: the cursor stays in that field and Enter keeps moving to the next one (Task 2 test; Task 4 test on the record form).
2. A fast double click on "Guardar" or on "Borrar" in the question: one save, one delete (Task 4 test; Task 3 `DeleteFlow` test).
3. Deleting the only row of the last page: the list goes to the previous page instead of showing an empty page (Task 5 test).
4. Deleting a record that is already gone (another tab): the error dialog shows and the list still reloads without it (Task 5 test).
5. After cancelling the question opened with the "Borrar" button, the search box has the cursor again and Enter opens the marked row (Task 5 test).

---

### Task 1: The dev proxy closes the connection when the API is down

**Files:**
- Modify: `vite.config.ts:9` (`server.proxy`)

**Interfaces:** none.

- [ ] **Step 1: See the current behaviour.** With nothing listening on 8080 (check with `netstat -ano | grep ":8080 .*LISTENING"`; if something is there, it is the user's API: do not touch it, and run this task's checks on another port as below), start `npx vite dev --port 5180 --strictPort` in the background and run `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5180/api/articles`. Expected: `502`.

If 8080 is busy, do the whole task against a free port: temporarily point the proxy target at `http://127.0.0.1:18089` (nothing listens there) for the checks, and put `8080` back before finishing.

- [ ] **Step 2: Configure the proxy.** Replace the string target with an object: `target: 'http://127.0.0.1:8080'`, `changeOrigin: true` (what Vite sets for a string target), and `configure: (proxy) => proxy.on('error', (_err, _req, res) => res.destroy())`, with a one-line comment: without the API the browser's fetch must fail as it does with the jar (`NETWORK` → "Sin conexión"), not get a 502. Vite registers its own 502 handler after `configure`; `res.destroy()` first means that handler writes to a destroyed response, which Node ignores.

- [ ] **Step 3: Verify.** Restart the dev server on 5180 and repeat the curl. Expected: curl prints `000` and exits with 52 (empty reply) or 56 (connection reset), and the Vite console shows no unhandled error and keeps serving (`curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5180/` → `200`). Stop the dev server.

- [ ] **Step 4: Check.** `npm run check && npm run lint`. Expected: 0 errors, lint clean.

---

### Task 2: `ActionButton` and `ScreenHeader`

**Files:**
- Create: `src/lib/components/ActionButton.svelte`
- Create: `src/lib/components/ScreenHeader.svelte`
- Create: `src/lib/test/ButtonHarness.svelte`
- Test: `src/lib/components/ActionButton.svelte.spec.ts`

**Interfaces:**
- Produces:
  - `ActionButton` props: `{ action: Pick<KeyAction, 'key' | 'shift' | 'keyLabel' | 'label' | 'run' | 'when'>; enabled?: () => boolean; variant?: 'normal' | 'primary' | 'danger' }`. `enabled` defaults to `() => action.when?.() !== false`. Renders `<button type="button" tabindex="-1">` with `action.label` and a `<kbd>` with `keyLabel(action)`; `disabled` when not enabled; `onmousedown` calls `preventDefault()` so the focus stays where it was; `onclick` runs `void action.run()`.
  - `ScreenHeader` props: `{ title: string; children?: Snippet }`. One line: the `<h1 class="text-2xl font-semibold">` on the left, the children (buttons, `flex gap-3`) on the right, `mb-4`, items centred; wraps on narrow screens.

- [ ] **Step 1: Write the harness and the failing tests.** `ButtonHarness.svelte` renders a plain `<input aria-label="Campo">`, then an `ActionButton` with the harness props passed through. Tests in `ActionButton.svelte.spec.ts`:
  - `shows the label and the key`: action `{ key: 'NumpadAdd', label: 'Guardar', run }` → `getByRole('button', { name: /Guardar/ })` visible and its text contains `+`.
  - `a click runs the action`: click → `run` called once.
  - `disabled when the action is not available, and a click does nothing`: `enabled: () => false` → `toBeDisabled()`; `when: () => false` with no `enabled` → also disabled; clicking (with `{ force: true }`) → `run` not called.
  - `the focus stays in the field`: focus the input, click the button → input `toHaveFocus()`, `run` called.
  - `Tab skips it`: button `toHaveAttribute('tabindex', '-1')`.
  - `variants`: `variant: 'primary'` → the class list contains `bg-accent`; `variant: 'danger'` → contains `text-danger`.

- [ ] **Step 2: Run them to see them fail.** `npx vitest run src/lib/components/ActionButton.svelte.spec.ts`. Expected: FAIL (component missing).

- [ ] **Step 3: Implement `ActionButton.svelte` and `ScreenHeader.svelte`.** Look: `h-12 rounded border-2 px-4 text-lg`; normal `border-line bg-surface text-ink hover:bg-canvas`; primary `border-accent bg-accent text-accent-ink`; danger `border-line bg-surface text-danger hover:bg-canvas`; disabled `disabled:opacity-50 disabled:cursor-default` (no hover change). The `<kbd>` after the label, smaller (`text-sm`), in a light chip (`rounded bg-canvas px-1.5 text-muted`; on primary use `bg-white/20 text-accent-ink`).

- [ ] **Step 4: Run the tests.** Same command. Expected: PASS. Then `npm run check && npm run lint`.

- [ ] **Step 5: Controller commits** `add ActionButton and ScreenHeader`.

---

### Task 3: Shared delete question (`DeleteFlow`, `DeleteDialog`) and per-entity delete modules

**Files:**
- Create: `src/lib/records/delete-flow.svelte.ts`
- Create: `src/lib/components/DeleteDialog.svelte`
- Create: `src/lib/catalog/articles.ts`, `src/lib/catalog/clients.ts`
- Modify: `src/lib/records/record-form.svelte.ts` (delete part: `deleteDialog`, `#deleting`, `askDelete`, `confirmDelete`, `cancelDelete`)
- Modify: `src/lib/components/RecordScreen.svelte` (the delete `<Dialog>` → `<DeleteDialog flow={form.deletion} />`)
- Modify: `src/lib/catalog/ArticleRecord.svelte`, `src/lib/catalog/ClientRecord.svelte` (use the modules)
- Test: `src/lib/records/delete-flow.svelte.spec.ts`; existing `RecordScreen.svelte.spec.ts` must pass unchanged

**Interfaces:**
- Produces (`records/delete-flow.svelte.ts`):
  ```ts
  export interface DeleteTarget { id: number; code: number | null; name: string }
  /** What an entity knows about deleting its records; shared by its record and its list. */
  export interface DeleteOptions {
    entity: { one: string; Label: string };
    remove(id: number): Promise<void>;
    /** The body of the question, or null when nothing depends on the record */
    pendingForDelete(id: number): Promise<string | null>;
  }
  export interface DeleteFlowOptions extends DeleteOptions {
    toast(message: string): void;
    onDeleted(target: DeleteTarget): void | Promise<void>;
    /** After a failed remove, once the error is shown */
    onFailed?(target: DeleteTarget): void | Promise<void>;
  }
  export class DeleteFlow {
    dialog: { title: string; name: string; body: string | null } | null; // $state
    constructor(options: DeleteFlowOptions);
    ask(target: DeleteTarget): Promise<void>;
    confirm(): Promise<void>;
    cancel(): void;
  }
  ```
  Texts, exactly as today: title `¿Borrar el ${one} ${code}?`, toast `${Label} ${code} borrado`.
- Produces: `DeleteDialog` props `{ flow: DeleteFlow }`: the `Dialog` RecordScreen has today (Enter and hidden Esc "No borrar", F8 "Borrar", name line then body line).
- Produces: `catalog/articles.ts` exports `articleDeletion: DeleteOptions`; `catalog/clients.ts` exports `clientDeletion: DeleteOptions` and `clientPending(clientId: number): Promise<[number, number]>` (unbilled notes, draft invoices; today's `pending()` in `ClientRecord`, also used by its regime confirmation).
- Produces: `RecordForm.deletion: DeleteFlow` (public, readonly). `RecordForm.askDelete()` stays (asks for the saved record: id, `#code`, `describe(#original)`; nothing for a new record). `deleteDialog`, `confirmDelete`, `cancelDelete` and `#deleting` go away. `RecordFormOptions` keeps `remove` and `pendingForDelete`, so a record passes `...articleDeletion` minus `entity`.

- [ ] **Step 1: Write the failing `DeleteFlow` tests** (`delete-flow.svelte.spec.ts`, plain unit tests; `showError` from `../components/errors.svelte` is observable through its exported state or a `vi.mock`, as other specs do):
  - `asks with the name and the pending text`: `ask({ id: 5, code: 101, name: 'Tomate' })` with `pendingForDelete` resolving `'Está en 3…'` → `dialog` equals `{ title: '¿Borrar el artículo 101?', name: 'Tomate', body: 'Está en 3…' }`; with null → `body: null`.
  - `a failed count shows the error and asks nothing`: `pendingForDelete` rejects → `dialog` null, error shown.
  - `confirm removes, toasts and reports`: → `remove(5)` once, toast `'Artículo 101 borrado'`, `onDeleted` called with the target, `dialog` null.
  - `a failed remove closes the question, shows the error and reports it`: → `dialog` null, error shown, `onFailed` called with the target, `onDeleted` not called.
  - `a second confirm while removing does nothing`: `remove` pending; `confirm()` twice → `remove` called once.
  - `a second ask while counting does nothing`.
  - `cancel closes without removing`.

- [ ] **Step 2: Run them to see them fail.** `npx vitest run src/lib/records/delete-flow.svelte.spec.ts`. Expected: FAIL (module missing).

- [ ] **Step 3: Implement `DeleteFlow`, `DeleteDialog`, the two catalog modules, and switch `RecordForm`, `RecordScreen`, `ArticleRecord`, `ClientRecord` to them.** `RecordForm` builds its `DeleteFlow` in the constructor with `onDeleted: async () => { this.#original = this.#snapshot(); await this.#options.goToList(); }` (keep the existing comment about the unsaved-changes question). If any existing spec or harness touches `form.deleteDialog`/`confirmDelete`/`cancelDelete`, switch it to `form.deletion`; the assertions stay the same.

- [ ] **Step 4: Run.** `npx vitest run src/lib/records src/lib/components/RecordScreen.svelte.spec.ts`, then `npm test`, `npm run check`, `npm run lint`. Expected: all PASS, with `RecordScreen` delete tests unchanged.

- [ ] **Step 5: Controller commits** `move the delete question to a shared DeleteFlow`.

---

### Task 4: Buttons on the record screen

**Files:**
- Modify: `src/lib/components/RecordScreen.svelte`
- Test: `src/lib/components/RecordScreen.svelte.spec.ts`

**Interfaces:**
- Consumes: `ActionButton`, `ScreenHeader` (Task 2); `RecordForm.askDelete()` (Task 3).

- [ ] **Step 1: Write the failing tests** in `RecordScreen.svelte.spec.ts` (`button = (name) => page.getByRole('button', { name: new RegExp(name) })`):
  - `the buttons do what their keys do`: editing article 101 → click "Guardar" → `update` called once and `goToList(5)`; new render, click "Borrar" → dialog "¿Borrar el artículo 101?"; new render, click "Volver" → `goToList` called (clean form, no question).
  - `Borrar is greyed out in a new record`: new record → "Borrar" `toBeDisabled()`; "Guardar" and "Volver" enabled.
  - `the buttons sit on the title line`: the `h1`'s parent element contains the three buttons.
  - `a button keeps the cursor in the field`: editing, cursor in "Nombre", click "Borrar" → the question opens; Enter ("No borrar") → the cursor is back in "Nombre"; Enter → it moves to the next field as before ("Código": the form wraps).
  - `a double click on Guardar saves once`: `update` deferred; `button('Guardar').dblClick()` → `update` called once.
  - `Esc is called Volver`: the key bar shows `Esc Volver` (it said `Esc Salir`).

- [ ] **Step 2: Run them to see them fail.** `npx vitest run src/lib/components/RecordScreen.svelte.spec.ts`. Expected: the new tests FAIL.

- [ ] **Step 3: Implement.** Hold the three actions in constants (`save`, `remove` with `when: () => form.id !== null`, `back` with `label: 'Volver'`) and use the same objects in `useKeys` and in `<ScreenHeader {title}>` with `ActionButton`s: Guardar `variant="primary"`, Borrar `variant="danger"`, Volver normal. Update any e2e that waits for "Salir" on a record screen to "Volver" (`grep -n "Salir" e2e/*.ts`: today only list/home screens use it; check).

- [ ] **Step 4: Run.** `npm test`, `npm run check`, `npm run lint`. Expected: PASS.

- [ ] **Step 5: Controller commits** `add buttons to the record screen`.

---

### Task 5: Buttons, page row and delete on the list screen

**Files:**
- Modify: `src/lib/components/ListScreen.svelte`, `src/lib/components/DataTable.svelte` (drop `page`, `onPage`, the page keys and the "Página" line)
- Modify: `src/lib/test/ListHarness.svelte`, `src/lib/test/TableHarness.svelte`
- Test: `src/lib/components/ListScreen.svelte.spec.ts`, `src/lib/components/DataTable.svelte.spec.ts`

**Interfaces:**
- Consumes: `ActionButton`, `ScreenHeader` (Task 2); `DeleteFlow`, `DeleteOptions`, `DeleteDialog` (Task 3).
- Produces: new optional `ListScreen` prop
  ```ts
  deletion?: {
    options: DeleteOptions;
    /** The row's name in the question, and its code in the title and the toast */
    describe(row: T): string;
    code(row: T): number;
    toast(message: string): void;
    /** After a delete, done or failed: the page loads its rows again (same search, same page) */
    reload(): void | Promise<void>;
    /** After a done delete, before reloading: e.g. forget the remembered row */
    onDeleted?(id: number): void;
  };
  ```
  Without `deletion` there is no "Borrar" button and no F8.

- [ ] **Step 1: Move the page tests.** In `DataTable.svelte.spec.ts`, drop `page keys ask for the next and previous page` and the page-key half of `arrows and page keys are left to a focused text input` (keep its arrows half); `TableHarness` loses `page`/`onPage`. In `ListScreen.svelte.spec.ts` add (harness gains `deletion` pass-through with spies: `remove`, `pendingForDelete`, `toast`, `reload`, `onDeleted`, `describe: (r) => r.name`, `code: (r) => r.code`, entity cliente):
  - `the page row shows the page and its buttons`: `page: { number: 1, totalPages: 3 }` → text `Página 2 de 3`; click "Página siguiente" → `onPage(2)`; click "Página anterior" → `onPage(0)`. With `{ number: 0, totalPages: 3 }` "Página anterior" disabled; with `{ number: 2, totalPages: 3 }` "Página siguiente" disabled; with one page, no page row.
  - `page keys work with the focus outside the search box`: click row 2 (focus leaves the box), PageDown → `onPage` called.
  - `Nuevo and Abrir do what F2 and Enter do`: click "Nuevo" → `onNew`; click "Abrir" → `onOpen(rows[0])`; with `rows: []` and no search pending, "Abrir" disabled.
  - `Borrar asks about the marked row`: ArrowDown, click "Borrar" → dialog "¿Borrar el cliente 2?" with name "Bar Pepe" and the pending text; `pendingForDelete(12)`.
  - `F8 deletes the marked row and reloads`: F8, F8 → `remove(12)`, toast `Cliente 2 borrado`, `onDeleted(12)`, `reload` called.
  - `Enter or Esc keeps the row`: F8 then Enter → closed, `remove` not called; F8 then Esc → same.
  - `the cancelled question gives the cursor back to the search box`: focus in the box, click "Borrar", Enter → the box `toHaveFocus()`; Enter → `onOpen` with the marked row.
  - `after a delete the mark stays in place`: mark row 3 (index 2), F8 F8, rerender with the two remaining rows → row 2 (new last) is selected (`aria-selected="true"`); mark row 1, delete, rerender without it → row 1 selected.
  - `deleting the only row of the last page goes to the previous page`: `page: { number: 2, totalPages: 3 }`, one row; F8 F8; rerender with `rows: []` → `onPage(1)`.
  - `a failed delete still reloads`: `remove` rejects with `ApiError(404, 'NOT_FOUND', …)` → error dialog shown, `reload` called, `onDeleted` not called, toast not called.
  - `without deletion there is no Borrar`: no `deletion` prop → no "Borrar" button, F8 not in the key bar.
  - `F8 is in the key bar`: with `deletion` and rows → `F8 Borrar`; with `rows: []` → absent.

- [ ] **Step 2: Run them to see them fail.** `npx vitest run src/lib/components/ListScreen.svelte.spec.ts src/lib/components/DataTable.svelte.spec.ts`. Expected: the new `ListScreen` tests FAIL.

- [ ] **Step 3: Implement.**
  - `DataTable`: remove `page`, `onPage`, `hasNext`, `hasPrevious`, the two page actions and the "Página" paragraph.
  - `ListScreen`: page actions without `inSearch()` (keep `turnPage`); a page row under the table only when `page.totalPages > 1`: `ActionButton` "Página anterior" · `Página {n} de {total}` · "Página siguiente", enabled by the same page conditions.
  - Header: `<ScreenHeader {title}>` with "Nuevo" (the F2 action), "Abrir" (`run: open`, `enabled: () => rows.length > 0 || waiting()`), "Borrar" (only with `deletion`; `variant="danger"`, `enabled: () => rows.length > 0`).
  - F8 action `{ key: 'F8', label: 'Borrar', when: () => deletion !== undefined && rows.length > 0, run: askDelete }`, in the same `useKeys` list, using the same object as the button.
  - One `DeleteFlow` built from `deletion` (`onDeleted`: remember the index to restore, call `deletion.onDeleted?.(id)`, then `deletion.reload()`; `onFailed`: `deletion.reload()`), and `<DeleteDialog flow={…} />`.
  - When the rows arrive after a delete: if empty and `page.number > 0` → `turnPage(page.number - 1)`; otherwise `selected = Math.min(index, rows.length - 1)`. This takes precedence over `selectId` for that one change.

- [ ] **Step 4: Run.** `npm test`, `npm run check`, `npm run lint`. Expected: PASS.

- [ ] **Step 5: Controller commits** `add buttons, page row and delete to the list screen`.

---

### Task 6: Wire the catalog lists, e2e and docs

**Files:**
- Modify: `src/routes/articles/+page.svelte`, `src/routes/clients/+page.svelte`
- Modify: `src/lib/records/list-memory.ts` (+ `list-memory.spec.ts`)
- Modify: `e2e/articles.e2e.ts`, `e2e/clients.e2e.ts`
- Modify: `front/README.md` (conventions), `PENDIENTES.md` §5

**Interfaces:**
- Consumes: `ListScreen.deletion` (Task 5); `articleDeletion`, `clientDeletion` (Task 3).
- Produces: `forgetOpened(section: string, id: number): void` in `list-memory.ts` (like `rememberList`): if the section remembers `openedId === id`, keep its `href` and drop `openedId`; otherwise nothing.

- [ ] **Step 1: Failing unit test** in `list-memory.spec.ts`: `forgetOpened drops only that record` (remember `{ href: '/articles?q=x', openedId: 7 }`; `forgetOpened('articles', 8)` → unchanged; `forgetOpened('articles', 7)` → `{ href: '/articles?q=x', openedId: undefined }`).

- [ ] **Step 2: Failing e2e tests.**
  - `articles.e2e.ts` → `with the mouse only: new, save, back, delete`: open the list; click "Nuevo"; fill "Nombre" with `Ratón e2e`; click "Guardar" → toast `Artículo {code} guardado`; click "Volver" → `/articles`; `searchFor(page, 'Ratón e2e')`; click the row; click "Borrar" → dialog `¿Borrar el artículo {code}?` with `Ratón e2e`; click the dialog's "Borrar" button (`page.getByRole('dialog').getByRole('button', { name: /Borrar/ })`) → toast `Artículo {code} borrado`, the row gone, the empty-search text visible.
  - `articles.e2e.ts` → `the page buttons turn the page`: create 21 articles through the API named `Página e2e 01`…`21` (codes 9300–9320); `searchFor(page, 'Página e2e')`; `Página 1 de 2` visible; click "Página siguiente" → URL has `page=1`, `Página 2 de 2`, one row; click "Página anterior" → `Página 1 de 2`.
  - `clients.e2e.ts` → `deleting from the list warns with the number`: `clientWithNote(request, 9205, 'Lista borrar e2e')`; open `/clients`; `searchFor(page, 'Lista borrar e2e')`; F8 → `Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.`; F8 → toast `Cliente 9205 borrado` and the row gone.

- [ ] **Step 3: Run to see them fail.** `npx vitest run src/lib/records/list-memory.spec.ts`; `npm run test:e2e`. Expected: the new tests FAIL.

- [ ] **Step 4: Implement.** Both list pages pass `deletion={{ options: articleDeletion /* clientDeletion */, describe: (r) => r.name, code: (r) => r.code, toast, reload: () => invalidateAll(), onDeleted: (id) => forgetOpened('articles' /* 'clients' */, id) }}` (`toast` from `#lib/components/toast.svelte.js`, `invalidateAll` from `$app/navigation`). Clients describe by trade name, as their record does.

- [ ] **Step 5: Docs.**
  - `PENDIENTES.md` §5: remove the line "Botones grandes en pantalla para las acciones principales…".
  - `front/README.md` conventions, one bullet: every main action has its key and its `ActionButton`, built from the same `KeyAction`; the button's `enabled` ignores the focus; buttons go in `ScreenHeader`.

- [ ] **Step 6: Run everything.** `npm run check`, `npm run lint`, `npm test`, `npm run test:e2e` twice. Expected: all PASS both times.

- [ ] **Step 7: Controller commits** `wire list delete and buttons into the catalog, with e2e`.
