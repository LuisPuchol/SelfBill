# Ajustes (IVA y titular): plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** From an empty database the user can set the VAT/surcharge validity periods and the owner's fiscal and mail data, sees what's missing on the home screen, and the API stops exposing the mail password.

**Architecture:** Two small API changes (tax-config history + percentage cap; profile `hasMailPassword`, optional password, undecryptable password = not configured), then a shared `FormState` + `EditScreen` pulled out of `RecordForm`, and the Ajustes screens built on the catalog's pieces (`Tile`, `DataTable`, `ScreenHeader`, `ActionButton`, `UnsavedGuard`).

**Tech Stack:** Spring Boot 3.5 + Java 25 (`JAVA_HOME`), SQLite, MapStruct; SvelteKit 3, Svelte 5 runes, Tailwind 4 tokens, Vitest 4 browser mode, Playwright e2e on the packaged jar (port 18080, temp DB).

**Spec:** `docs/superpowers/specs/2026-10-18-front-ajustes-design.md`

## Global Constraints

- Front paths relative to `front/`; API paths relative to `selfbill-api/src/` unless written in full.
- Inside `src/lib`, relative imports; outside, `#lib/<path>.js` / `#lib/<path>.svelte`. Every internal `href`/`goto` through `resolve()`. One `useKeys`/`keyLayer` per component. "+" is `NumpadAdd`.
- Spanish UI texts exactly as the spec writes them; English code and comments. New `ErrorCode` values need their text in `src/lib/api/messages.ts`; removed ones lose it.
- Buttons are `ActionButton`s in `ScreenHeader` ("Guardar", "Volver", "Nuevo", "Corregir"), each built from the same `KeyAction` as its key.
- Lucide icons imported the way the existing code does (`import { Percent } from '@lucide/svelte'`, as in `src/lib/sections.ts`); switching to per-icon imports is a separate pending item.
- Never start, stop or bind anything on port 8080 (the user's API) and never touch the user's `selfbill.db` or `~/.selfbill/secret.key`. Temporary APIs use another port and temp DB + key, like `e2e/serve-jar.mjs`. No python on this machine.
- Implementers never commit or stage; the controller commits each task after review.
- API tests: `./mvnw test` from `selfbill-api/` (Java 25 via `JAVA_HOME`). Front: `npm run check`, `npm run lint`, `npm test`, `npm run test:e2e`.

## Review Focus

1. Saving the owner's data with the password field left empty keeps the stored password (`mailPassword: null` is sent) — Task 6 test.
2. A percentage typed with a comma ("0,5") is stored as 0.5 and listed as "0,5 %" — Task 5 test.
3. Only a future validity period exists: the home and Ajustes say "No hay IVA vigente hoy", not "Sin configurar" — Task 4 test.
4. Correcting a period sends its own `validFrom`, never today's date — Task 5 test.
5. The `RecordForm` refactor keeps the catalog identical: `RecordScreen.svelte.spec.ts` passes unchanged — Task 3.

---

### Task 1: Tax-config history and percentage cap (API)

**Files:**
- Modify: `main/java/com/luispuchol/selfbill/selfbill_api/settings/taxconfig/{TaxConfigController,TaxConfigService,TaxConfigRepository,TaxConfigRequest}.java`
- Create: `test/java/com/luispuchol/selfbill/selfbill_api/settings/taxconfig/TaxConfigHistoryTest.java`
- Modify: `test/java/com/luispuchol/selfbill/selfbill_api/OpenApiDocsTest.java`

**Interfaces:**
- Produces: `GET /api/tax-config/history` → `List<TaxConfigResponse>`, newest `validFrom` first; `[]` when none. `TaxConfigService.getHistory(): List<TaxConfigResponse>`; `TaxConfigRepository.findAllByOrderByValidFromDesc()`. `vatPercentage`/`surchargePercentage` get `@DecimalMax(value = "100.0", inclusive = true)` (message like the existing `@DecimalMin` ones).

- [ ] **Step 1: Failing tests** (`@SpringBootTest` + `@ActiveProfiles("test")` like `UserProfileServiceTest`, or MockMvc like `catalog/client/ClientControllerTest`; clean `tax_config` in `@BeforeEach`):
  - `historyIsEmptyWithoutConfigs` → `[]`.
  - `historyListsNewestFirst`: create 2023-01-01, 2027-01-01, 2026-01-01 → order 2027, 2026, 2023, with their rates.
  - `percentagesAbove100AreRejected`: `POST` with `vatPercentage` 100.01 → 400 `VALIDATION_ERROR` on `vatPercentage`; same for `surchargePercentage`; 100 exactly is accepted.
  - `OpenApiDocsTest`: the docs list `/api/tax-config/history`.
- [ ] **Step 2: Run, see them fail.** `./mvnw -q test -Dtest='TaxConfigHistoryTest,OpenApiDocsTest'`.
- [ ] **Step 3: Implement.** `@Operation(summary = "List every tax configuration, newest first")`; the history mapping must not collide with the existing `GET /api/tax-config`.
- [ ] **Step 4: Run** the same tests, then `./mvnw -q test`. Expected: all pass.
- [ ] **Step 5: Controller commits** `list tax configurations and cap percentages at 100`.

---

### Task 2: Owner profile without exposed password (API) and regenerated front types

**Files:**
- Modify: `main/java/.../settings/profile/{UserProfileService,UserProfileResponse,UserProfileMapper,UserProfileController}.java`, `main/java/.../shared/ErrorCode.java`
- Modify: `test/java/.../settings/profile/UserProfileServiceTest.java`; add a mail-side test next to `test/java/.../mail/EmailServiceTest.java` (or in `billing/DocumentEmailTest.java`, whichever already builds an `SmtpAccount`)
- Modify: `front/src/lib/api/schema.d.ts` (regenerated), `front/src/lib/api/messages.ts`
- Modify: `PENDIENTES.md` §2

**Interfaces:**
- Produces: `UserProfileResponse.hasMailPassword: boolean` replaces `mailPassword` (no decryption when responding). `POST /api/user-profile` accepts a missing or blank `mailPassword` (stored as none). `USER_PROFILE_MAIL_PASSWORD_REQUIRED` removed from `ErrorCode`, the controller's `@ApiErrors` and `messages.ts`. `UserProfileService.getSmtpAccount()` throws `MAIL_NOT_CONFIGURED` when the stored password can't be decrypted (log a warning without the secret).

- [ ] **Step 1: Failing tests** in `UserProfileServiceTest` (adapt the existing ones that read `.mailPassword()`):
  - `createsWithoutPassword` (null) and `createsWithBlankPassword` → profile saved, `hasMailPassword` false.
  - `hasMailPasswordFollowsTheStoredOne`: create with "secret" → true; update with null → still true; update with "" → false.
  - `readsAProfileWhosePasswordCannotBeDecrypted`: overwrite `mail_password_encrypted` through `JdbcTemplate` with a well-formed but foreign value (e.g. the encryption of "x" under a different `CredentialEncryptionService` key, or random Base64 of 12 + 32 bytes) → `getUserProfile()` works and `hasMailPassword` is true.
  - `anUndecryptablePasswordIsNotConfigured`: same setup, with SMTP host and port set → `getSmtpAccount()` throws `BusinessException` with `MAIL_NOT_CONFIGURED`.
- [ ] **Step 2: Run, see them fail.** `./mvnw -q test -Dtest=UserProfileServiceTest` (plus the mail test).
- [ ] **Step 3: Implement**, then `./mvnw -q test`. Expected: all pass.
- [ ] **Step 4: Regenerate the front types without touching 8080.** `./mvnw -q package -DskipTests` in `selfbill-api/`; start the jar on **18081** with a temp DB and a temp key file the way `front/e2e/serve-jar.mjs` does (`--server.port=18081`, `--spring.datasource.url=…tmp…`, `--selfbill.credentials.key-file=…tmp…`); from `front/`: `npx -y openapi-typescript@7.13.0 http://127.0.0.1:18081/v3/api-docs -o src/lib/api/schema.d.ts`; stop that jar (only its PID); delete the temp files. Remove the `USER_PROFILE_MAIL_PASSWORD_REQUIRED` entry from `messages.ts`. `npm run check` and `npm run lint` clean.
- [ ] **Step 5: PENDIENTES §2:** remove "Sin la clave de cifrado, el perfil da 500" and "Expuesto a la red, con la contraseña en claro" (both fixed).
- [ ] **Step 6: Controller commits** `stop exposing the mail password and allow a profile without it`.

---

### Task 3: `FormState`, `EditScreen` and `NumberField.validate` (front)

**Files:**
- Create: `src/lib/records/form-state.svelte.ts`, `src/lib/components/EditScreen.svelte`, `src/lib/test/EditHarness.svelte`
- Modify: `src/lib/records/record-form.svelte.ts` (extends `FormState`), `src/lib/components/NumberField.svelte`, `front/README.md` (conventions)
- Test: `src/lib/components/EditScreen.svelte.spec.ts`, `src/lib/components/Fields.svelte.spec.ts`; `src/lib/components/RecordScreen.svelte.spec.ts` must pass **unchanged**

**Interfaces:**
- Produces (`records/form-state.svelte.ts`):
  ```ts
  export class FormState<V extends object> {
    values: V;                        // $state, its own copy of `initial`
    nav: FormNav | undefined;         // $state.raw, set through bind:nav
    constructor(initial: V);
    get isDirty(): boolean;
    /** The values as last saved (or as opened) */
    get saved(): V;
    /** The API's error for a field, while the field keeps the value it had when the error came */
    errorFor(name: string): string | undefined;
    setFieldErrors(messages: Record<string, string>): void;   // stores them and puts the cursor on the first
    clearFieldErrors(): void;
    /** A VALIDATION_ERROR with field errors goes under its fields, in Spanish (fieldErrorMessages + `messages`); false for anything else */
    showFieldErrors(err: unknown, messages?: Record<string, string>): boolean;
    /** Nothing left to save: the current values become the saved ones */
    markSaved(): void;
    /** Another record to edit: values and saved copy both become `values` */
    reset(values: V): void;
    snapshot(): V;
    /** Back to the first field the API refused; with none, to where the form had the cursor */
    restoreFocus(): void;
    /** validateAll, then `persist` — one at a time; false when invalid, busy, or `persist` returned false */
    save(persist: () => Promise<boolean>): Promise<boolean>;
  }
  ```
- Produces: `RecordForm<V extends { code: number | null }> extends FormState<V>`, same public API and behaviour as today (its `code` default message and duplicate-code branch stay in `RecordForm`).
- Produces: `EditScreen` props `{ form: FormState<V>; title: string; save: () => Promise<boolean>; exit: () => void; children: Snippet }` — `ScreenHeader` with `Guardar · +` (primary; `run: save`) and `Volver · Esc` (`run: exit`), the same objects in its `useKeys`; `<Form bind:nav={form.nav} onLast={() => form.nav?.focusFirstField()}>`; cursor in the first field on mount.
- Produces: `NumberField` prop `validate?: (value: number) => string | undefined`, applied like `TextField`'s (when leaving the field and in `validateAll`, only on a parsed number).

- [ ] **Step 1: Failing tests.**
  - `EditScreen.svelte.spec.ts` with `EditHarness` (a `FormState` over `{ name: string; amount: number | null }`, a required `TextField` and a `NumberField` with `validate: v => v > 100 ? 'Tiene que estar entre 0 y 100' : undefined`, `save` = `form.save(persist)` with a spy `persist`, `exit` spy, `KeyBar`):
    - `the cursor starts in the first field`;
    - `"+" checks every field first`: empty name → "Este dato es obligatorio", `persist` not called;
    - `Guardar and Volver do what their keys do`: valid form, click "Guardar" → `persist` once; click "Volver" → `exit`;
    - `a second "+" while saving does nothing` (`persist` deferred);
    - `API field errors show under the field and clear when it changes` (`persist` calls `form.showFieldErrors(new ApiError(400, 'VALIDATION_ERROR', '', { name: 'Name cannot be blank' }), { name: 'Nombre no válido' })` and returns false);
    - `isDirty follows the values and markSaved clears it`.
  - `Fields.svelte.spec.ts`: `NumberField validate shows its message on leaving and in validateAll` (150 → message; 50 → none).
- [ ] **Step 2: Run, see them fail.** `npx vitest run src/lib/components/EditScreen.svelte.spec.ts src/lib/components/Fields.svelte.spec.ts`.
- [ ] **Step 3: Implement** `FormState`, move the shared parts of `RecordForm` into it (values, saved copy, field errors with value snapshot, `errorFor`, `restoreFocus`, VALIDATION_ERROR translation) and make `RecordForm` extend it; `EditScreen`; `NumberField.validate`. README conventions, one bullet: records with code use `RecordForm` + `RecordScreen`; other forms use `FormState` + `EditScreen`, each with its `UnsavedGuard`.
- [ ] **Step 4: Run.** `npx vitest run src/lib/components/RecordScreen.svelte.spec.ts` (unchanged file, all pass), then `npm test`, `npm run check`, `npm run lint`.
- [ ] **Step 5: Controller commits** `pull FormState and EditScreen out of RecordForm`.

---

### Task 4: Setup status, Ajustes menu and home notice (front)

**Files:**
- Create: `src/lib/settings/status.ts` (+ `status.spec.ts`), `src/routes/settings/+page.ts`, `src/lib/settings/SettingsMenu.svelte`, `src/lib/components/SetupNotice.svelte` (+ spec), `src/routes/+page.ts`
- Modify: `src/routes/settings/+page.svelte` (drop the placeholder), `src/routes/+page.svelte`, `src/lib/components/Tile.svelte` (+ `Tile.svelte.spec.ts`), `src/lib/format/number.ts` (+ `number.spec.ts`)
- Test: `e2e/settings.e2e.ts` (new)

**Interfaces:**
- Produces (`settings/status.ts`):
  ```ts
  export interface SetupStatus {
    /** The period in force today, or null */
    tax: TaxConfigResponse | null;
    /** Whether any period exists (a future one only: tax null, anyTax true) */
    anyTax: boolean;
    profile: UserProfileResponse | null;
  }
  export function loadSetupStatus(): Promise<SetupStatus>;      // 404s become null/false; other errors propagate
  export function setupNotice(status: SetupStatus): string | null; // the spec §4.2 texts
  export function taxSummary(status: SetupStatus): string;      // "IVA 4 % · Recargo 0,5 %" | "No hay IVA vigente hoy" | "Sin configurar"
  ```
- Produces: `formatPercent(value: number): string` in `format/number.ts`: up to 2 decimals, comma, trailing zeros trimmed, non-breaking space before "%" ("4 %", "0,5 %", "5,25 %", "21 %").
- Produces: `Tile` prop `detail?: string` (a smaller line under the title).

- [ ] **Step 1: Failing tests.**
  - `number.spec.ts`: `formatPercent` for 4, 0.5, 5.25, 21, 0.
  - `status.spec.ts` (pure functions): `setupNotice` for nothing missing (null), IVA and titular, only IVA, only titular, future-only IVA alone and with titular missing — exact spec §4.2 texts; `taxSummary` for the three cases.
  - `SetupNotice.svelte.spec.ts`: renders the text for a status, renders nothing when complete.
  - `Tile.svelte.spec.ts`: `detail` shows under the title.
  - A settings-menu component test (render `SettingsMenu` with a status prop): tiles "IVA y recargo" with `IVA 4 % · Recargo 0,5 %` and "Datos del titular" with the fiscal name or "Sin configurar"; key bar `1–2 Abrir` and `Esc Volver`.
  - `e2e/settings.e2e.ts` → `Ajustes opens with 6 and says what is configured`: read `GET /api/tax-config` and `/api/user-profile` through `request` first, then press 6 on the home and assert the tile texts accordingly.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** Home `+page.ts`: `loaded(loadSetupStatus())`; home shows `SetupNotice` above the tiles. `/settings`: `+page.ts` loads the status; the page renders `SettingsMenu` (two `Tile`s: '1' "IVA y recargo" `/settings/tax`, '2' "Datos del titular" `/settings/profile`, with `detail`); keys 1 and 2 (`keyLabel: '1–2'`, label "Abrir") and Esc "Volver" to `/`. The tiles link to routes that Tasks 5 and 6 create; until then `check` may need placeholder `+page.svelte` files there — create minimal ones (Tasks 5/6 replace them).
- [ ] **Step 4: Run.** Focused specs, `npm test`, `npm run check`, `npm run lint`, `npm run test:e2e`.
- [ ] **Step 5: Controller commits** `add the settings menu and the home setup notice`.

---

### Task 5: VAT periods list and form (front)

**Files:**
- Create: `src/routes/settings/tax/{+page.ts,+page.svelte}`, `src/routes/settings/tax/new/{+page.ts,+page.svelte}`, `src/routes/settings/tax/[validFrom]/{+page.ts,+page.svelte}`, `src/lib/settings/TaxRecord.svelte`, `src/lib/settings/TaxList.svelte`
- Modify: `src/lib/components/DataTable.svelte` (prop `openLabel`)
- Test: `src/lib/settings/TaxList.svelte.spec.ts`, `src/lib/settings/TaxRecord.svelte.spec.ts` (harnesses in `src/lib/test/` as needed, without `$app`), `e2e/settings.e2e.ts`

**Interfaces:**
- Consumes: `FormState`, `EditScreen` (Task 3); `formatPercent`, `todayIso`, `formatDate`; `GET /api/tax-config/history` (Task 1).
- Produces: `DataTable` prop `openLabel?: string` (default "Abrir"), the label of its Enter action.
- Produces: `TaxList` props `{ periods: TaxConfigResponse[]; today: string; selectId?: number; onNew(): void; onOpen(period): void; onExit(): void }`; `TaxRecord` props `{ mode: 'new' | 'edit'; initial: { validFrom: string; vatPercentage: number | null; surchargePercentage: number | null }; create(body): Promise<TaxConfigResponse>; update(body): Promise<TaxConfigResponse>; goToList(savedId?: number): void | Promise<void>; toast(message: string): void }` (the pages wire `api`, `goto`, `toast`, list memory).

- [ ] **Step 1: Failing tests.**
  - `TaxList`: rows newest first with "4 %" / "0,5 %"; "Vigente" on the period in force on `today`; "A partir del 01/01/2027" on a future one; the in-force row selected on entry (or `selectId`'s); empty text "Todavía no hay ningún tipo. Pulsa F2 para añadir el primero."; F2 and "Nuevo" → `onNew`; Enter and "Corregir" → `onOpen(selected)`; "Corregir" greyed out without rows; key bar `Enter Corregir`, `Esc Volver`.
  - `TaxRecord` new: proposed date passed in `initial` is shown; IVA 150 → "Tiene que estar entre 0 y 100"; "0,5" in Recargo → `create` gets `surchargePercentage: 0.5`; `create` rejecting `ApiError(409, 'TAX_CONFIG_ALREADY_EXISTS', …)` → under Desde "Ya hay un tipo que empieza ese día: corrígelo desde la lista." and the cursor there; success → toast `Tipo desde el 01/01/2026 guardado` and `goToList(id)`.
  - `TaxRecord` edit: title "Tipo desde el 01/01/2026", no Desde field, the fixed notice "Los albaranes y facturas ya hechos conservan el tipo con el que se valoraron."; `update` gets the original `validFrom`.
  - e2e → `adds a period and corrects it`: from `/settings/tax`, F2, type the date 01/01/2099 (digits through the date field), IVA 21, Recargo 5,2, "+" → back on the list with a row "01/01/2099 · 21 % · 5,2 % · A partir del 01/01/2099" selected; Enter → change IVA to 22, "+" → the row shows 22 %.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** `/settings/tax/+page.ts` loads the history; the page wires `TaxList` (`selectId` from `recallList('settings-tax')?.openedId`). `/new/+page.ts` loads the history to propose the date (`${year}-01-01` when empty, else `todayIso()`). `/[validFrom]/+page.ts` finds the period in the history, `error(404)` when missing. `TaxRecord` uses `FormState` + `EditScreen` + `UnsavedGuard` (in the page-level component, as the catalog records do); field messages `{ vatPercentage, surchargePercentage: 'Tiene que estar entre 0 y 100' }`.
- [ ] **Step 4: Run.** Focused specs, `npm test`, `npm run check`, `npm run lint`, `npm run test:e2e`.
- [ ] **Step 5: Controller commits** `add the VAT periods list and form`.

---

### Task 6: Owner data form, e2e and docs (front)

**Files:**
- Create: `src/routes/settings/profile/{+page.ts,+page.svelte}`, `src/lib/settings/ProfileRecord.svelte` (+ spec, harness without `$app`)
- Modify: `e2e/settings.e2e.ts`, `PENDIENTES.md` §5

**Interfaces:**
- Consumes: `FormState`, `EditScreen` (Task 3); `UserProfileResponse.hasMailPassword` (Task 2); `EMAIL_PATTERN` from `src/lib/catalog/client-form.ts`.
- Produces: `ProfileRecord` props `{ initial: UserProfileResponse | null; save(body: UserProfileRequest, exists: boolean): Promise<void>; goBack(): void | Promise<void>; toast(message: string): void }`.

- [ ] **Step 1: Failing tests.**
  - `ProfileRecord`: blocks "Datos fiscales" and "Correo" with the spec §4.6 fields in Enter order; required and length/format checks (email pattern; port 1–65535 via `NumberField validate`); with no profile, "+" calls `save(body, false)`; with one, `save(body, true)`; password field empty on entry, with "Guardada. Déjalo vacío para conservarla." when `hasMailPassword`; empty password → `mailPassword: null` in the body; typed → sent; optional empties as `null` (`trimOrNull`); success → toast "Datos del titular guardados" and `goBack`.
  - e2e → `saves the owner's data and the home notice goes away`: ensure a period in force (`POST /api/tax-config` 2026-01-01, 4, 0.5, ignore 409); from Ajustes press 2, fill the required fields (Email `facturas@ejemplo.es`, no password), "+" → back on Ajustes with the fiscal name on tile 2; on the home, no "Falta configurar" text.
- [ ] **Step 2: Run, see them fail.**
- [ ] **Step 3: Implement.** `/settings/profile/+page.ts`: `GET /api/user-profile`, 404 → `null` (not the error page; other errors through `loaded`). `ProfileRecord` with `FormState` + `EditScreen` + `UnsavedGuard`; `POST` or `PUT`.
- [ ] **Step 4: PENDIENTES §5**, one line each: borrar una vigencia (no hay endpoint); logo del titular en Ajustes (con el bloque del PDF); probar la cuenta de correo desde Ajustes.
- [ ] **Step 5: Run everything.** `npm run check`, `npm run lint`, `npm test`, `npm run test:e2e` twice; `./mvnw -q test` once.
- [ ] **Step 6: Controller commits** `add the owner data form`.
