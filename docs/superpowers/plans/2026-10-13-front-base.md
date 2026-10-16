# Front, la base: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir la base del front de SelfBill: proyecto SvelteKit servido desde el jar de la API, cliente tipado de la API, sistema de teclado, componentes de campo, diálogos, errores y el inicio con sus seis secciones vacías.

**Architecture:** SPA estática (`adapter-static`, `ssr = false`) que Spring sirve desde `static/` con un fallback a `index.html`. La lógica sin DOM (formatos, errores, teclas, aviso de cambios) vive en módulos `.ts` puros con tests en node; los componentes se prueban en Chromium con Vitest en modo navegador, y la navegación de punta a punta con Playwright contra el jar.

**Tech Stack:** SvelteKit 3.0 (Svelte 5 con runes, Vite 8, TypeScript 6), Tailwind 4, Vitest 4 (`@vitest/browser-playwright`, `vitest-browser-svelte`), Playwright 1.60, `openapi-fetch` 0.17, `openapi-typescript` 7.13 (vía `npx`), `@lucide/svelte`. API: Java 25, Spring Boot 3.5.7, springdoc 2.8.13.

**Spec:** [docs/superpowers/specs/2026-10-13-front-base-design.md](../specs/2026-10-13-front-base-design.md)

## Requisitos previos

- Rama `feature/front-base`, que ya tiene los commits del spec.
- Node 22.18. `JAVA_HOME` apunta al JDK 25. Ojo: el `java` del PATH es el JDK 24 y no arranca el jar.
- Comandos del front desde `front/`, y de la API desde `selfbill-api/` (Git Bash: `./mvnw`).
- Línea base de la API: `./mvnw test` acaba con 121 tests y 0 fallos.

## Global Constraints

- SvelteKit 3: no hay `svelte.config.js`. La configuración va en `sveltekit({...})` dentro de `vite.config.ts`, y `lib/` se importa como `#lib/…`.
- Si alguna dependencia no funciona con SvelteKit 3, se pasa a 2.70 y se explica en el mensaje del commit. No se mezclan versiones.
- Únicas dependencias de ejecución nuevas: `openapi-fetch` y `@lucide/svelte`. `openapi-typescript@7.13.0` se ejecuta con `npx`, sin instalarlo: declara TypeScript 5 y `npm i` falla.
- Todo `href` interno y todo `goto` pasan por `resolve()` de `$app/paths`: la regla `svelte/no-navigation-without-resolve` de ESLint lo exige. Las rutas fijas se tipan con `RouteId` de `$app/types`.
- Código, rutas y nombres en inglés. Todo texto en pantalla en español. Nunca se muestra un texto de la API (`detail` ni mensajes de validación).
- Teclas: "+" es `event.code === 'NumpadAdd'`; el "+" del teclado principal escribe. Las teclas F que se usan son F2, F3, F4, F8 y F9. F5, F11 y F12 no se usan.
- Estilo: `html { font-size: 112.5% }` (1rem = 18 px), campos de al menos 48 px (`h-12`), tema claro, pantalla de referencia 1920×1080. La barra de teclas es pequeña (`text-sm`) y no es clicable.
- Formatos: `1.234,56 €` (espacio duro antes del €) y `12/10/2026`.
- Tests: node en `*.spec.ts`, navegador en `*.svelte.spec.ts`, punta a punta en `front/e2e/*.e2e.ts`. Primero el test, luego el código.
- Punta a punta, siempre en el puerto 18080, con una SQLite y una clave temporales. Nunca contra el 8080 ni contra `selfbill.db`.
- `src/lib/api/schema.d.ts` se genera y va a git, excluido de Prettier y ESLint. Se genera sin `--properties-required-by-default`: los campos de la API llegan como opcionales y se tratan así.
- Cada tarea del front termina con `npm run check`, `npm run lint` y `npm test` en verde. Cada tarea de la API, con `./mvnw test` en verde (incluido `ModularityTest`).
- Commits: mensaje corto en inglés y en minúsculas, como los del `git log`, terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Pasada la medianoche (00:30), "hoy" es el día local, no el de UTC, que todavía sería ayer (tarea 3, `todayIso`).
2. Un importe de cuatro cifras sale `1.234,56 €`. `Intl` en es-ES no agrupa los miles hasta las cinco cifras salvo con `useGrouping: 'always'` (tarea 3).
3. El "+" del teclado principal en un campo de texto, como en el email `pepe+tienda@…`, se escribe y no guarda (tarea 5, `matches NumpadAdd by code only`).
4. Dos Enter seguidos mientras se consulta un código hacen una sola consulta, y el cursor avanza una sola vez (tarea 9).
5. Al instalar un jar nuevo, Edge no se queda con un `index.html` en caché que apunta a ficheros que ya no existen (tarea 1, `index.html` sin caché).

## Mapa de ficheros

API: `main/` = `selfbill-api/src/main/java/com/luispuchol/selfbill/selfbill_api/`, `test/` = `selfbill-api/src/test/java/com/luispuchol/selfbill/selfbill_api/`. Front: rutas relativas a `front/`.

| Fichero | Responsabilidad | Tarea |
|---|---|---|
| `main/shared/SpaWebConfig.java`, `application.properties`, `main/shared/RequestLoggingFilter.java` | Servir el front, fallback, 127.0.0.1, log solo de `/api` | 1 |
| `selfbill-api/pom.xml` | Copiar `front/build` al jar | 13 |
| `vite.config.ts`, `src/routes/+layout.ts`, `src/routes/layout.css`, `src/app.html` | Proyecto, SPA, proxy, tokens de estilo | 2 |
| `src/lib/format/{number,money,date}.ts` | Formatos es-ES, total en céntimos, fechas | 3 |
| `src/lib/api/{schema.d.ts,errors,messages,client,field-errors,classify,load}.ts` | Tipos, `ApiError`, textos, cliente, clasificación | 4 |
| `src/lib/keyboard/{keys.ts,keymap.svelte.ts,KeyboardRoot.svelte}`, `src/lib/components/KeyBar.svelte` | Capas de teclas y barra | 5 |
| `src/lib/sections.ts`, `src/lib/components/{TopBar,Tile,SectionPlaceholder}.svelte`, `src/routes/**/+page.svelte`, `src/routes/+layout.svelte` | Inicio, barra superior, secciones vacías | 6 |
| `src/lib/keyboard/form-nav.ts`, `src/lib/components/{Form,Field,TextField,NumberField}.svelte`, `field-style.ts` | Enter entre campos, campos de texto y número | 7 |
| `src/lib/components/{DigitsField,DateField,ChoiceField}.svelte` | Lote, fecha, opciones | 8 |
| `src/lib/components/CodeField.svelte` | Código con consulta | 9 |
| `src/lib/components/{Dialog,UnsavedGuard,Toaster}.svelte`, `unsaved.ts`, `toast.svelte.ts` | Diálogos, cambios sin guardar, avisos | 10 |
| `src/lib/components/{ErrorDialog,ConnectionScreen}.svelte`, `errors.svelte.ts`, `src/routes/+error.svelte` | Errores en pantalla | 11 |
| `src/lib/components/{DataTable.svelte,table.ts}`, `src/lib/url.ts` | Listados | 12 |
| `playwright.config.ts`, `e2e/serve-jar.mjs`, `e2e/navigation.e2e.ts` | Punta a punta contra el jar | 13 |
| `CLAUDE.md`, `front/README.md`, `PENDIENTES.md` | Documentación | 1, 14 |

Los componentes de prueba (`*Harness.svelte`) van en `src/lib/test/`.

---

### Task 1: La API sirve el front

**Files:**
- Create: `main/shared/SpaWebConfig.java`
- Modify: `selfbill-api/src/main/resources/application.properties`, `main/shared/RequestLoggingFilter.java`
- Create: `selfbill-api/src/test/resources/static/index.html`, `selfbill-api/src/test/resources/static/_app/immutable/app.js`
- Test: `test/shared/SpaWebConfigTest.java`
- Modify: `PENDIENTES.md` ("Expuesto a la red, con la contraseña en claro")

**Interfaces:**
- Produces: cualquier GET que no sea `/api/**`, `/v3/**` ni `/swagger-ui/**` y cuyo último segmento no tenga punto devuelve `classpath:/static/index.html`. La tarea 13 rellena `static/` en el jar.

- [ ] **Step 1: Crear los ficheros de prueba**

`static/index.html`: `<!doctype html><title>selfbill-test-index</title>`. `static/_app/immutable/app.js`: `console.log('selfbill-test-asset');`.

- [ ] **Step 2: Escribir `SpaWebConfigTest`**

`@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @ExtendWith(OutputCaptureExtension.class)`, como `OpenApiDocsTest`:

```java
@Test void servesIndexAtRoot()            // GET / → 200, text/html, contiene "selfbill-test-index", Cache-Control contiene "no-cache"
@Test void servesIndexForAppRoutes()      // GET /delivery-notes/12 → 200, contiene "selfbill-test-index"
@Test void servesBuildFiles()             // GET /_app/immutable/app.js → 200, contiene "selfbill-test-asset", Cache-Control contiene "max-age=31536000"
@Test void missingFileIsNotIndex()        // GET /_app/immutable/missing.js → 404
@Test void unknownApiRouteIsNotIndex()    // GET /api/nope → 404, el cuerpo no contiene "selfbill-test-index"
@Test void keepsApiDocsAndSwagger()       // GET /v3/api-docs → 200 JSON; GET /swagger-ui/index.html → 200
@Test void logsOnlyApiRequests(CapturedOutput output)
// GET /_app/immutable/app.js y GET /api/articles/next-code →
// output contiene "GET /api/articles/next-code" y no contiene "GET /_app/immutable/app.js"
```

- [ ] **Step 3: Ejecutar y ver que falla**

Run: `./mvnw test -Dtest=SpaWebConfigTest`
Expected: FAIL. `/` y `/delivery-notes/12` dan 404.

- [ ] **Step 4: Implementar**

`application.properties`:

```properties
# Desktop app: only this machine may connect (PENDIENTES, "Expuesto a la red")
server.address=127.0.0.1
# SpaWebConfig owns static files, so the front's routes fall back to index.html
spring.web.resources.add-mappings=false
```

`SpaWebConfig implements WebMvcConfigurer`, en `shared`, con `addResourceHandlers`:
- `/_app/immutable/**` → `classpath:/static/_app/immutable/`, con `CacheControl.maxAge(365, DAYS).cachePublic().immutable()`.
- `/**` → `classpath:/static/`, con `CacheControl.noCache()` y `.resourceChain(true).addResolver(...)`. El resolver es un `PathResourceResolver` cuyo `getResource(path, location)` devuelve el fichero si existe. Si no, devuelve `null` cuando la ruta empieza por `api/`, `v3/` o `swagger-ui` o su último segmento tiene un punto, y `index.html` en cualquier otro caso.

`RequestLoggingFilter`: `shouldNotFilter` devuelve `true` si la URI no empieza por `/api`.

Si `/swagger-ui/index.html` deja de responder con `add-mappings=false`, se quita esa propiedad y el handler `/**` se registra con `registry.setOrder(Ordered.LOWEST_PRECEDENCE)`.

- [ ] **Step 5: Ejecutar y ver que pasa**

Run: `./mvnw test`
Expected: 128 tests, 0 fallos, `ModularityTest` incluido.

- [ ] **Step 6: Actualizar PENDIENTES**

En "Expuesto a la red, con la contraseña en claro": la API ya escucha solo en `127.0.0.1`. Queda devolver `hasMailPassword` en vez de la contraseña (bloque de ajustes).

- [ ] **Step 7: Commit**

```bash
git add selfbill-api PENDIENTES.md
git commit -m "serve the front from the api on localhost only"
```

---

### Task 2: Proyecto del front

**Files:**
- Delete: `front/` (plantilla de 2025)
- Create: `front/` con `sv create`
- Modify: `front/vite.config.ts`, `front/package.json`, `front/src/app.html`, `front/src/routes/layout.css`, `front/src/routes/+layout.svelte`
- Create: `front/src/routes/+layout.ts`
- Delete: `front/src/lib/vitest-examples/`, `front/src/routes/page.svelte.e2e.ts`

**Interfaces:**
- Produces: tokens de Tailwind `bg-canvas`, `bg-surface`, `text-ink`, `text-muted`, `border-line`, `bg-accent`/`text-accent-ink`, `border-focus`, `bg-field-active`, `text-danger`, `text-draft`, `text-issued`, `text-pending`, y la clase `.tabular`. `npm run build` deja `front/build/index.html`.

- [ ] **Step 1: Borrar la plantilla antigua**

Run (raíz del repo): `git rm -r -q front && rm -rf front`

- [ ] **Step 2: Generar el proyecto**

Run (raíz del repo):

```bash
npx -y sv@1.1.1 create front --template minimal --types ts --add prettier eslint "vitest=usages:unit,component" "tailwindcss=plugins:none" "sveltekit-adapter=adapter:static" "playwright=demo:no" --install npm
```

Expected: "You're all set!". `front/package.json` tiene `@sveltejs/kit` ^3.0.0.

- [ ] **Step 3: Quitar los ejemplos**

Run: `rm -r front/src/lib/vitest-examples front/src/routes/page.svelte.e2e.ts`

- [ ] **Step 4: Configurar SPA, proxy y scripts**

- `vite.config.ts`: `adapter: adapter({ fallback: 'index.html' })` y `server: { proxy: { '/api': 'http://127.0.0.1:8080' } }`. El bloque `test` se queda como lo genera `sv`.
- `src/routes/+layout.ts`: `export const ssr = false;`
- `package.json`: `"test": "vitest --run"` y `"test:e2e": "playwright test"`. La tarea 13 completa `test:e2e`.
- `src/app.html`: `lang="es"`.
- `src/routes/+layout.svelte`: `<title>SelfBill</title>` en `<svelte:head>`.

- [ ] **Step 5: Tokens de estilo en `src/routes/layout.css`**

Después del `@import 'tailwindcss'`:

```css
@theme {
	--font-sans: 'Segoe UI', system-ui, sans-serif;
	--color-ink: #1a1a1a;
	--color-muted: #4b5563;
	--color-canvas: #f4f5f7;
	--color-surface: #ffffff;
	--color-line: #c9ccd1;
	--color-accent: #1d4ed8;
	--color-accent-ink: #ffffff;
	--color-focus: #c2410c;
	--color-field-active: #fff4e5;
	--color-danger: #b91c1c;
	--color-draft: #92400e;
	--color-issued: #166534;
	--color-pending: #1e40af;
}
```

Y además: `html { font-size: 112.5%; }`, `body` con `bg-canvas text-ink font-sans`, `:focus-visible { outline: 3px solid var(--color-focus); outline-offset: 2px; }` y `.tabular { font-variant-numeric: tabular-nums; }`.

- [ ] **Step 6: Verificar**

Run: `npm run check && npm run lint && npm run build`
Expected: 0 errores, y existe `build/index.html`.

- [ ] **Step 7: Commit**

```bash
git add -A front
git commit -m "regenerate the front with sveltekit 3"
```

---

### Task 3: Formatos

**Files:**
- Create: `src/lib/format/number.ts`, `src/lib/format/money.ts`, `src/lib/format/date.ts`
- Test: `src/lib/format/number.spec.ts`, `src/lib/format/money.spec.ts`, `src/lib/format/date.spec.ts`

**Interfaces:**
- Produces:
  - `parseDecimal(text: string): number | null`: acepta "," o "." como decimal, nunca separador de miles, y rechaza negativos.
  - `formatDecimal(value: number, decimals = 2): string`: es-ES, sin miles.
  - `formatMoney(euros: number): string`
  - `lineTotalCents(quantity: number, price: number): number`
  - `totalCents(lines: readonly { quantity: number; price: number }[]): number`
  - `formatDate(iso: string): string`: acepta `YYYY-MM-DD` y `YYYY-MM-DDTHH:mm:ss`.
  - `parseDateDigits(text: string, today: string): string | null`: devuelve ISO `YYYY-MM-DD`.
  - `todayIso(now = new Date()): string`

- [ ] **Step 1: Escribir los tests**

```ts
// number.spec.ts
expect(parseDecimal('10,5')).toBe(10.5);
expect(parseDecimal('10.5')).toBe(10.5);
expect(parseDecimal(' 7,25 ')).toBe(7.25);
expect(parseDecimal('10')).toBe(10);
for (const bad of ['', 'abc', '1,2,3', '1.234,56', '-3']) expect(parseDecimal(bad)).toBeNull();
expect(formatDecimal(10)).toBe('10,00');
expect(formatDecimal(1234.5)).toBe('1234,50');
expect(formatDecimal(3, 0)).toBe('3');

// money.spec.ts
expect(formatMoney(1234.56)).toBe('1.234,56 €');
expect(formatMoney(84)).toBe('84,00 €');
expect(lineTotalCents(10, 1.2)).toBe(1200);
expect(lineTotalCents(0.33, 1.5)).toBe(50);      // 0,495 → mitad hacia arriba
expect(lineTotalCents(1.25, 0.99)).toBe(124);    // 1,2375
expect(lineTotalCents(3, 0.1)).toBe(30);         // 0.30000000000000004 en coma flotante
expect(totalCents([{ quantity: 0.33, price: 1.5 }, { quantity: 0.33, price: 1.5 }])).toBe(100); // por línea, no 99

// date.spec.ts
expect(formatDate('2026-10-12')).toBe('12/10/2026');
expect(formatDate('2026-10-12T08:00:00')).toBe('12/10/2026');
expect(parseDateDigits('121026', '2026-10-16')).toBe('2026-10-12');
expect(parseDateDigits('12102026', '2026-10-16')).toBe('2026-10-12');
expect(parseDateDigits('12/10/26', '2026-10-16')).toBe('2026-10-12');
expect(parseDateDigits('1210', '2026-10-16')).toBe('2026-10-12');   // año en curso
expect(parseDateDigits('290228', '2026-10-16')).toBe('2028-02-29');
for (const bad of ['310226', '12345', '']) expect(parseDateDigits(bad, '2026-10-16')).toBeNull();
expect(todayIso(new Date(2026, 9, 13, 0, 30))).toBe('2026-10-13');  // hora local, no UTC
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npx vitest --run --project server src/lib/format`
Expected: FAIL, no se encuentran los módulos.

- [ ] **Step 3: Implementar**

- `formatMoney` usa `Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', useGrouping: 'always' })`.
- `lineTotalCents` trabaja en enteros: `q = Math.round(quantity * 100)`, `p = Math.round(price * 100)` y `Math.floor((q * p + 50) / 100)`.
- `parseDateDigits`: quita lo que no son cifras. Con 6 cifras es `ddmmyy` (20yy), con 8 `ddmmyyyy` y con 4 `ddmm` del año de `today`. Comprueba que la fecha existe.
- `formatDate` y `todayIso` leen y escriben las cifras directamente, sin `new Date(iso)` ni `toISOString()`.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front/src/lib/format
git commit -m "add es-ES formats and cent totals"
```

---

### Task 4: Cliente de la API

**Files:**
- Modify: `front/package.json` (script `api:types`, dependencia `openapi-fetch@^0.17.0`), `front/.prettierignore`, `front/eslint.config.js`
- Create: `src/lib/api/schema.d.ts` (generado), `errors.ts`, `messages.ts`, `client.ts`, `field-errors.ts`, `classify.ts`, `load.ts`
- Test: `src/lib/api/errors.spec.ts`, `client.spec.ts`, `field-errors.spec.ts`, `classify.spec.ts`, `load.spec.ts`

**Interfaces:**
- Produces:
  - `type ErrorCode = NonNullable<components['schemas']['Problem']['errorCode']>`
  - `class ApiError extends Error { readonly status: number; readonly code: ErrorCode | 'NETWORK' | 'UNKNOWN'; readonly detail: string; readonly fieldErrors: Record<string, string> }`
  - `toApiError(status: number, body: unknown): ApiError`, `networkError(cause: unknown): ApiError` (status 0)
  - `errorMessages: Record<ErrorCode, string>`, `isErrorCode(value: unknown): value is ErrorCode`
  - `api` (`createClient<paths>({ baseUrl: '' })`), `request<R extends { data?: unknown; error?: unknown; response: Response }>(call: Promise<R>): Promise<Exclude<R['data'], undefined>>`
  - `fieldErrorMessages(apiErrors: Record<string, string>, known?: Record<string, string>): Record<string, string>`
  - `type ErrorView = { kind: 'connection' } | { kind: 'business'; message: string } | { kind: 'unexpected'; status: number; code: string; detail: string }`, `classifyError(err: unknown): ErrorView`
  - `loaded<T>(call: Promise<T>): Promise<T>`: convierte un `ApiError` en `error(status, …)` de SvelteKit. `NETWORK` pasa a 503.

- [ ] **Step 1: Generar los tipos**

- Añadir el script `"api:types": "npx -y openapi-typescript@7.13.0 http://127.0.0.1:8080/v3/api-docs -o src/lib/api/schema.d.ts"`.
- Arrancar la API (`./mvnw spring-boot:run` en `selfbill-api/`), ejecutar `npm run api:types` y parar la API.
- Añadir `src/lib/api/schema.d.ts` a `.prettierignore` y a los `ignores` de `eslint.config.js`.
- `npm i openapi-fetch@^0.17.0`.

Expected: `schema.d.ts` contiene `errorCode?: "CLIENT_NOT_FOUND" | …` con los 27 códigos.

- [ ] **Step 2: Escribir los tests**

```ts
// errors.spec.ts
const dup = toApiError(409, { status: 409, detail: 'Already exists a client with code: 15', errorCode: 'CLIENT_DUPLICATE_CODE' });
expect(dup).toMatchObject({ status: 409, code: 'CLIENT_DUPLICATE_CODE', fieldErrors: {} });
const invalid = toApiError(400, { errorCode: 'VALIDATION_ERROR', errors: { nif: 'NIF cannot be blank', 'lines[2].quantity': 'Quantity cannot be null' } });
expect(invalid.fieldErrors).toEqual({ nif: 'NIF cannot be blank', 'lines[2].quantity': 'Quantity cannot be null' });
// 404 de Spring para una ruta inexistente: sin errorCode
expect(toApiError(404, { timestamp: 't', status: 404, error: 'Not Found', path: '/api/nope' })).toMatchObject({ code: 'UNKNOWN', status: 404 });
expect(toApiError(409, { errorCode: 'SOMETHING_NEW' }).code).toBe('UNKNOWN');
expect(toApiError(500, 'not json')).toMatchObject({ code: 'UNKNOWN', detail: '' });
expect(networkError(new TypeError('Failed to fetch'))).toMatchObject({ code: 'NETWORK', status: 0 });

// client.spec.ts
await expect(request(Promise.resolve({ data: { id: 1 }, response: new Response(null, { status: 200 }) }))).resolves.toEqual({ id: 1 });
await expect(request(Promise.resolve({ error: { errorCode: 'INVOICE_ALREADY_ISSUED' }, response: new Response(null, { status: 409 }) })))
	.rejects.toMatchObject({ code: 'INVOICE_ALREADY_ISSUED', status: 409 });
await expect(request(Promise.reject(new TypeError('Failed to fetch')))).rejects.toMatchObject({ code: 'NETWORK' });

// field-errors.spec.ts
expect(fieldErrorMessages({ nif: 'NIF cannot be blank' })).toEqual({ nif: 'Revisa este dato' });
expect(fieldErrorMessages({ nif: 'x' }, { nif: 'Falta el NIF' })).toEqual({ nif: 'Falta el NIF' });
expect(fieldErrorMessages({ 'lines[2].quantity': 'x' }, { 'lines[].quantity': 'Pon la cantidad' })).toEqual({ 'lines[2].quantity': 'Pon la cantidad' });

// classify.spec.ts
expect(classifyError(networkError(new TypeError()))).toEqual({ kind: 'connection' });
expect(classifyError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {})))
	.toEqual({ kind: 'business', message: errorMessages.INVOICE_MIXED_MONTHS });
expect(classifyError(new ApiError(500, 'EMAIL_SEND_FAILED', 'x', {})).kind).toBe('business');
expect(classifyError(new ApiError(500, 'INTERNAL_ERROR', 'Internal server error', {})))
	.toEqual({ kind: 'unexpected', status: 500, code: 'INTERNAL_ERROR', detail: 'Internal server error' });
expect(classifyError(new ApiError(404, 'UNKNOWN', '', {})).kind).toBe('unexpected');
expect(classifyError(new Error('boom'))).toEqual({ kind: 'unexpected', status: 0, code: '', detail: 'boom' });

// load.spec.ts (isHttpError de @sveltejs/kit)
await loaded(Promise.reject(new ApiError(404, 'CLIENT_NOT_FOUND', '', {}))).catch((e) => expect(isHttpError(e, 404)).toBe(true));
await loaded(Promise.reject(networkError(new TypeError()))).catch((e) => expect(isHttpError(e, 503)).toBe(true));
await expect(loaded(Promise.resolve(7))).resolves.toBe(7);
```

- [ ] **Step 3: Ejecutar y ver que falla**

Run: `npx vitest --run --project server src/lib/api`
Expected: FAIL, no se encuentran los módulos.

- [ ] **Step 4: Implementar**

`toApiError` toma `code` del `errorCode` del cuerpo si `isErrorCode`, y `'UNKNOWN'` si no. `isErrorCode` es `typeof value === 'string' && value in errorMessages`. `classifyError`: un código conocido distinto de `INTERNAL_ERROR` es `business`; cualquier otro `ApiError` o `Error` es `unexpected`. `fieldErrorMessages` busca el texto por la ruta exacta y después con los índices vaciados (`lines[2].quantity` → `lines[].quantity`).

`errorMessages`, texto exacto:

| Código | Texto |
|---|---|
| `CLIENT_NOT_FOUND` | No existe ese cliente. Puede que se haya borrado. |
| `CLIENT_DUPLICATE_CODE` | Ya existe un cliente con ese código. |
| `CLIENT_EMAIL_NOT_CONFIGURED` | Este cliente no tiene correo electrónico. Añádelo en su ficha para poder enviarle documentos. |
| `CLIENT_FISCAL_DATA_MISSING` | Al cliente le faltan datos para emitir la factura: NIF, dirección, código postal o población. Complétalos en su ficha. |
| `ARTICLE_NOT_FOUND` | No existe ese artículo. Puede que se haya borrado. |
| `ARTICLE_DUPLICATE_CODE` | Ya existe un artículo con ese código. |
| `DELIVERY_NOTE_NOT_FOUND` | No existe ese albarán. Puede que se haya borrado. |
| `DELIVERY_NOTE_DUPLICATE_CODE` | Ya existe un albarán con ese número. |
| `DELIVERY_NOTE_ALREADY_INVOICED` | Este albarán está en una factura emitida y ya no se puede cambiar ni borrar. |
| `INVOICE_NOT_FOUND` | No existe esa factura. Puede que se haya borrado. |
| `INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED` | Uno de los albaranes ya está en otra factura. |
| `INVOICE_MIXED_CLIENTS` | Todos los albaranes de una factura tienen que ser del mismo cliente. |
| `INVOICE_MIXED_TAX_RATES` | Los albaranes llevan tipos de IVA distintos. Una factura solo puede llevar albaranes con el mismo IVA. |
| `INVOICE_MIXED_MONTHS` | Los albaranes son de meses distintos. Una factura solo puede llevar albaranes del mismo mes. |
| `INVOICE_ALREADY_ISSUED` | Esta factura ya está emitida y no se puede cambiar. |
| `INVOICE_NOT_ISSUED` | Esta factura todavía es un borrador. Emítela primero. |
| `TAX_CONFIG_NOT_FOUND` | No hay ningún IVA configurado para esa fecha. Añádelo en Ajustes. |
| `TAX_CONFIG_ALREADY_EXISTS` | Ya hay un IVA que empieza en esa fecha. |
| `USER_PROFILE_NOT_FOUND` | Faltan tus datos de empresa. Rellénalos en Ajustes. |
| `USER_PROFILE_ALREADY_EXISTS` | Tus datos de empresa ya están guardados. |
| `USER_PROFILE_MAIL_PASSWORD_REQUIRED` | Falta la contraseña del correo. |
| `USER_PROFILE_LOGO_NOT_FOUND` | Todavía no has subido ningún logotipo. |
| `USER_PROFILE_INVALID_LOGO_FORMAT` | El logotipo tiene que ser una imagen JPG o PNG. |
| `MAIL_NOT_CONFIGURED` | El correo no está configurado. Rellena sus datos en Ajustes. |
| `EMAIL_SEND_FAILED` | No se ha podido enviar el correo. Comprueba la conexión a internet y los datos del correo en Ajustes. |
| `VALIDATION_ERROR` | Hay datos que revisar. |
| `INTERNAL_ERROR` | Ha ocurrido un error inesperado. La operación no se ha completado. |

- [ ] **Step 5: Ejecutar y ver que pasa**

Run: `npm run check && npm test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add front
git commit -m "add typed api client and spanish error texts"
```

---

### Task 5: Capas de teclas y barra de teclas

**Files:**
- Create: `src/lib/keyboard/keys.ts`, `src/lib/keyboard/keymap.svelte.ts`, `src/lib/keyboard/KeyboardRoot.svelte`, `src/lib/components/KeyBar.svelte`, `src/lib/test/KeysHarness.svelte`
- Test: `src/lib/keyboard/keys.spec.ts`, `src/lib/components/KeyBar.svelte.spec.ts`

**Interfaces:**
- Produces (`keys.ts`, sin runes):

```ts
export interface KeyAction {
	key: string;              // event.code o event.key: 'NumpadAdd', 'Escape', 'Enter', 'F3', '1'
	shift?: boolean;
	label: string;
	keyLabel?: string;        // sustituye a keyLabel(key) en la barra, p. ej. '1–6'
	hidden?: boolean;
	when?: () => boolean;     // false: la acción no existe y la tecla sigue bajando
	run: () => void | Promise<void>;
}
export interface Layer { id: number; actions: () => KeyAction[]; modal: boolean }
export type KeyEventLike = Pick<KeyboardEvent, 'key' | 'code' | 'shiftKey' | 'defaultPrevented' | 'target' | 'preventDefault'>;
export const ALWAYS_PREVENTED: readonly string[];  // ['F1','F2','F3','F4','F7','F8','F9']
export function isEditable(target: unknown): boolean;
export function dispatch(layers: readonly Layer[], event: KeyEventLike): boolean;
export function visibleActions(layers: readonly Layer[]): KeyAction[];
export function keyLabel(action: Pick<KeyAction, 'key' | 'shift' | 'keyLabel'>): string;
```

- Produces (`keymap.svelte.ts`): `keymap` con `layers` reactivo, `push(actions: () => KeyAction[], modal?: boolean): () => void`, `handle(event: KeyboardEvent): void` y `visible: KeyAction[]` derivado; `useKeys(actions: KeyAction[] | (() => KeyAction[]), options?: { modal?: boolean }): void`, que registra en un `$effect` y se borra al desmontar.
- Produces: `<KeyboardRoot />` (escucha `keydown` en `window`) y `<KeyBar />`.

Reglas de `dispatch`:
- Recorre las capas de arriba abajo y se para tras la primera modal.
- Una acción coincide si `key` es igual a `event.code` o a `event.key`, y su `shift` coincide.
- Las teclas de un solo carácter no se despachan si `isEditable(event.target)`.
- Si `event.defaultPrevented`, no se hace nada.
- Al ejecutar una acción llama a `preventDefault` y devuelve `true`.
- Las teclas de `ALWAYS_PREVENTED` siempre llevan `preventDefault`.

`visibleActions` recorre las mismas capas, quita las `hidden` y las de `when()` falso, y deja una por tecla (gana la de arriba). Ordena por `Enter`, `NumpadAdd`, `Escape`, `F2`, `F3`, `F4`, `F8`, `F9` y, detrás, el resto en el orden en que se declararon.

- [ ] **Step 1: Escribir `keys.spec.ts`**

Con `ev(over)` = `{ key: '', code: '', shiftKey: false, defaultPrevented: false, target: null, preventDefault: vi.fn(), ...over }` y `layer(actions, modal = false)`:

```ts
it('runs the action of the top layer and prevents default')  // page y top con Escape → solo la de top, preventDefault, true
it('falls through non-modal layers')                         // top {F3}, page {Escape} → Escape ejecuta la de page
it('stops at a modal layer')                                 // page {NumpadAdd}, modal {Enter} → NumpadAdd no ejecuta nada, false
it('matches NumpadAdd by code only')                         // {key:'+',code:'NumpadAdd'} ejecuta; {key:'+',code:'BracketRight'} no, y sin preventDefault
it('treats both Enter keys alike')                           // {key:'Enter',code:'NumpadEnter'} ejecuta la acción Enter
it('requires the declared shift state')                      // Enter con shiftKey no ejecuta {key:'Enter'}; sí {key:'Enter',shift:true}
it('ignores single-character keys while typing')             // acción '1': target {tagName:'INPUT'} → no; target null → sí
it('skips actions whose when() is false')                    // baja a la capa de debajo
it('ignores events a component already handled')             // defaultPrevented → nada
it('blocks browser F keys even without an action')           // F1, F3, F7 → preventDefault y false; F5 → sin preventDefault
it('lists visible actions in fixed order, upper layers winning')
// page [Escape 'Salir', NumpadAdd 'Guardar'], form [Enter 'Siguiente', Enter+shift hidden] → ['Siguiente','Guardar','Salir']
it('stops visible actions at the modal layer')
// page [NumpadAdd 'Guardar'], modal [Enter 'Seguir editando', F8 'Salir sin guardar'] → ['Seguir editando','Salir sin guardar']
it('names keys in spanish')
// NumpadAdd '+', Escape 'Esc', Enter 'Enter', Enter+shift 'Shift+Enter', PageDown 'AvPág', PageUp 'RePág', ArrowUp '↑', ArrowDown '↓', F3 'F3', keyLabel '1–6' → '1–6'
```

- [ ] **Step 2: Escribir `KeyBar.svelte.spec.ts`**

`KeysHarness.svelte` recibe `actions: KeyAction[]` y `modalActions?: KeyAction[]`. Monta `<KeyboardRoot />`, `<KeyBar />`, `useKeys(actions)` y un botón "Abrir capa" que hace `keymap.push(() => modalActions, true)`.

```ts
it('shows the keys of the current screen')        // [Escape 'Salir', NumpadAdd 'Guardar'] → se ven 'Esc', 'Salir', '+', 'Guardar'
it('runs the action when its key is pressed')     // userEvent.keyboard('{Escape}') → spy de Salir llamado
it('replaces the bar while a modal layer is open') // clic en "Abrir capa" con [Enter 'Seguir editando'] → 'Seguir editando' sí, 'Guardar' no
```

- [ ] **Step 3: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL, no se encuentran los módulos.

- [ ] **Step 4: Implementar**

`KeyBar` es un `<footer>` fino y fijo abajo (`text-sm text-muted`, borde superior `border-line`), con `<kbd>{keyLabel(a)}</kbd> {a.label}` por acción. Sin botones.

- [ ] **Step 5: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add front/src/lib
git commit -m "add key layers and key bar"
```

---

### Task 6: Inicio, barra superior y secciones vacías

**Files:**
- Modify: `front/package.json` (dependencia `@lucide/svelte`), `src/routes/+layout.svelte`, `src/routes/+page.svelte`
- Create: `src/lib/sections.ts`, `src/lib/components/TopBar.svelte`, `src/lib/components/Tile.svelte`, `src/lib/components/SectionPlaceholder.svelte`
- Create: `src/routes/{delivery-notes,invoices,clients,articles,traceability,settings}/+page.svelte`
- Test: `src/lib/components/Tile.svelte.spec.ts`

**Interfaces:**
- Consumes: `useKeys`, `KeyboardRoot`, `KeyBar` (tarea 5).
- Produces: `interface Section { digit: string; title: string; href: RouteId; icon: Component }` y `sections: readonly Section[]`. El layout raíz monta `TopBar`, `<main>`, `KeyBar` y `KeyboardRoot`; las tareas 10 y 11 le añaden más piezas.

| `digit` | `title` | `href` | Icono de Lucide |
|---|---|---|---|
| 1 | Albaranes | `/delivery-notes` | `FileText` |
| 2 | Facturas | `/invoices` | `Receipt` |
| 3 | Clientes | `/clients` | `Users` |
| 4 | Artículos | `/articles` | `Package` |
| 5 | Trazabilidad | `/traceability` | `ScanSearch` |
| 6 | Ajustes | `/settings` | `Settings` |

- [ ] **Step 1: Escribir el test**

```ts
// Tile.svelte.spec.ts: render(Tile, { section: sections[0] })
// → enlace con nombre accesible que contiene 'Albaranes', href '/delivery-notes', y se ve el '1'
```

El inicio, la barra superior y las secciones vacías usan `goto` y `$app/state`, que no existen fuera de la app. Los prueba la tarea 13 de punta a punta.

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar**

- `npm i @lucide/svelte`.
- Inicio (`+page.svelte`): rejilla de 3×2 con un `Tile` por sección (número grande, icono de 48 px y título `text-2xl`). Declara `useKeys(() => sections.map((s, i) => ({ key: s.digit, label: 'Abrir sección', keyLabel: '1–6', hidden: i > 0, run: () => goto(resolve(s.href)) })))`.
- `TopBar`: "SelfBill" enlaza al inicio y lleva las seis secciones; la actual con `aria-current="page"` según `page.url.pathname` de `$app/state`.
- `SectionPlaceholder` busca la sección por `href`, pinta su título y el texto, y declara `Escape` 'Salir' → `goto(resolve('/'))`.
- Cada `+page.svelte` de sección es `<SectionPlaceholder href="…" />`.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test && npm run build`
Expected: PASS. La navegación real la prueba la tarea 13.

- [ ] **Step 5: Commit**

```bash
git add front
git commit -m "add home tiles, top bar and empty sections"
```

---

### Task 7: Enter entre campos, texto y número

**Files:**
- Create: `src/lib/keyboard/form-nav.ts`, `src/lib/components/Form.svelte`, `src/lib/components/Field.svelte`, `src/lib/components/TextField.svelte`, `src/lib/components/NumberField.svelte`, `src/lib/components/field-style.ts`, `src/lib/test/FormHarness.svelte`
- Test: `src/lib/keyboard/form-nav.svelte.spec.ts`, `src/lib/components/Form.svelte.spec.ts`

**Interfaces:**
- Consumes: `useKeys` (tarea 5), `parseDecimal`, `formatDecimal` (tarea 3).
- Produces:

```ts
export interface NavField { name: string; el: HTMLElement; commit?: () => boolean | Promise<boolean> }
export class FormNav {
	constructor(options?: { onLast?: () => void });
	register(field: NavField): () => void;
	next(): Promise<void>;          // valida el campo actual y pasa al siguiente en orden del DOM; en el último, onLast
	prev(): void;
	hasFocus(): boolean;            // el foco está en un campo registrado
	focus(name: string): void;
	focusFirst(names: readonly string[]): void;  // el primero en orden del DOM
	readonly busy: boolean;         // true mientras espera un commit: next() no hace nada
}
export function setFormNav(nav: FormNav): void;
export function getFormNav(): FormNav | undefined;
```

- `<Form onLast?>`: `<form novalidate>` con `onsubmit` que hace `preventDefault`. Crea un `FormNav` y declara Enter 'Siguiente' (`when: () => nav.hasFocus()`) y Shift+Enter 'Anterior' (oculta).
- `<Field id label error?>`: etiqueta encima y error en rojo debajo, enlazado con `aria-describedby`.
- `<TextField label name value=$bindable('') required? error? type?: 'text' | 'email'>`.
- `<NumberField label name value=$bindable<number | null>(null) decimals=2 required? error?>`.
- `field-style.ts`: `inputClass` con `h-12`, borde `border-2 border-line`, foco `border-focus bg-field-active`.

Normas de todos los campos:
- `id` = `name` = ruta de la API (p. ej. `lines[2].quantity`).
- Se registran en el `FormNav` del contexto.
- Al recibir el foco seleccionan su contenido.
- Muestran `error` (externo) o, si no hay, su error propio, que se borra al escribir.
- Texto de campo obligatorio vacío: "Este dato es obligatorio". Número no válido: "Escribe un número".
- `NumberField` al salir (commit o blur) normaliza con `formatDecimal`. Ante `keydown` con `code === 'NumpadDecimal'` hace `preventDefault` e inserta "," en el cursor.

- [ ] **Step 1: Escribir los tests**

```ts
// form-nav.svelte.spec.ts: inputs creados en document.body y registrados a mano
it('waits for a running commit before accepting another next()')
// commit del primero = promesa pendiente; next(); next() → commit llamado 1 vez; al resolver true, el foco en el segundo (no en el tercero)
it('orders fields by their place in the page, not by registration')
it('focusFirst picks the first in page order')

// Form.svelte.spec.ts: FormHarness = KeyboardRoot + Form(onLast) con TextField 'Nombre' (name, required),
// NumberField 'Cantidad' (quantity) y TextField 'Último' (last)
it('Enter moves to the next field')                   // foco en Nombre, escribir 'Pepe', {Enter} → Cantidad toHaveFocus
it('Shift+Enter moves back')
it('Enter on the last field calls onLast')
it('a required empty field keeps the cursor')         // {Enter} en Nombre vacío → 'Este dato es obligatorio', foco sigue
it('an invalid number keeps the cursor')              // 'abc' {Enter} → 'Escribe un número'
it('numbers are normalised on leaving')               // '10' {Enter} → '10,00'; '7.5' → '7,50'
it('the numpad decimal key always writes a comma')    // '3', keydown {key:'.',code:'NumpadDecimal'}, '5' → '3,5'; igual con key ','
it('entering a field selects its content')            // volver a Cantidad y escribir '4' → '4'
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según las interfaces y normas de arriba.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front/src/lib
git commit -m "add enter navigation, text and number fields"
```

---

### Task 8: Lote, fecha y opciones

**Files:**
- Create: `src/lib/components/DigitsField.svelte`, `src/lib/components/DateField.svelte`, `src/lib/components/ChoiceField.svelte`, `src/lib/test/FieldsHarness.svelte`
- Test: `src/lib/components/Fields.svelte.spec.ts`

**Interfaces:**
- Consumes: `Field`, `inputClass`, `getFormNav` (tarea 7); `parseDateDigits`, `formatDate`, `todayIso` (tarea 3).
- Produces:
  - `<DigitsField label name value=$bindable('') length required? error?>`: solo cifras (quita el resto al escribir), `maxlength = length`, `inputmode="numeric"`. Error: "Tienen que ser {length} cifras".
  - `<DateField label name value=$bindable(todayIso()) required? error?>`: muestra `formatDate(value)`; al salir interpreta con `parseDateDigits`. Error: "Fecha no válida".
  - `<ChoiceField<T> label name options: { value: T; label: string }[] value=$bindable<T | null>(null) required? error?>`: `role="radiogroup"` enfocable, con una opción `role="radio"` por elemento, mostrada como `<kbd>n</kbd> label`. La cifra n elige la opción n (con `preventDefault`) y el clic también. Error: "Elige una opción".

- [ ] **Step 1: Escribir los tests**

`FieldsHarness` = `KeyboardRoot` + `Form` con DigitsField 'Lote' (`traceabilityCode1`, length 8, required), DateField 'Fecha' (`date`), ChoiceField 'Formato' (`format`, opciones PLAIN 'Simple' y VALUED 'Valorado', required) y TextField 'Fin'. Pinta los valores enlazados en `<output data-testid="…">`.

```ts
it('the lot keeps only digits and its leading zero')  // escribir '01a21-0426' → valor '01210426'
it('a short lot keeps the cursor')                    // '0121' {Enter} → 'Tienen que ser 8 cifras', foco sigue
it('the date starts as today')                        // input = formatDate(todayIso())
it('the date accepts six digits')                     // seleccionar, '121026' {Enter} → '12/10/2026', valor '2026-10-12', foco en Formato
it('an impossible date keeps the cursor')             // '310226' {Enter} → 'Fecha no válida'
it('a digit picks the option')                        // foco en Formato, '2' → 'Valorado' aria-checked=true; {Enter} → foco en Fin
it('digits outside the options do nothing')           // '7' → sin cambios
it('a required choice keeps the cursor')              // {Enter} sin elegir → 'Elige una opción'
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según las interfaces.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front/src/lib
git commit -m "add lot, date and choice fields"
```

---

### Task 9: Campo de código con consulta

**Files:**
- Create: `src/lib/components/CodeField.svelte`, `src/lib/test/CodeHarness.svelte`
- Test: `src/lib/components/CodeField.svelte.spec.ts`

**Interfaces:**
- Consumes: `Field`, `inputClass`, `getFormNav` (tarea 7).
- Produces: `<CodeField label name value=$bindable<number | null>(null) lookup: (code: number) => Promise<string | null> notFoundMessage: string required? error?>`.

Comportamiento:
- Solo cifras (quita el resto al escribir).
- Al salir: vacío y obligatorio → "Este dato es obligatorio".
- Si el código es el último que se encontró, deja pasar sin consultar.
- Si no, hace `await lookup(code)`. Con `null` muestra `notFoundMessage` y no deja pasar. Con un nombre lo pinta al lado del campo, fija `value` y deja pasar.
- Al cambiar el texto se borra el nombre.
- Si arranca con valor, consulta una vez para pintar el nombre.
- Un `ApiError` de `lookup` no se captura aquí: lo recoge el manejador global (tarea 11).

- [ ] **Step 1: Escribir los tests**

`CodeHarness` = `KeyboardRoot` + `Form` con CodeField 'Artículo' (`articleId`, `lookup` por prop, `notFoundMessage` 'No existe ningún artículo con ese código') y TextField 'Cantidad'.

```ts
it('shows the name and moves on when the code exists')  // lookup(12) → 'Tomate'; '12' {Enter} → 'Tomate' visible, foco en Cantidad, lookup 1 vez con 12
it('keeps the cursor when the code does not exist')     // lookup → null → mensaje visible, foco sigue
it('keeps only digits')                                 // '1a2' → '12'
it('a second Enter during the lookup does nothing')     // lookup pendiente; {Enter}{Enter} → lookup 1 vez; al resolver, foco en Cantidad
it('editing the code clears the old name')
it('an initial value shows its name')                   // value 7, lookup → 'Lechuga' visible sin tocar nada
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según la interfaz.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front/src/lib
git commit -m "add code field with lookup"
```

---

### Task 10: Diálogo, cambios sin guardar y avisos

**Files:**
- Create: `src/lib/components/Dialog.svelte`, `src/lib/components/unsaved.ts`, `src/lib/components/UnsavedGuard.svelte`, `src/lib/components/toast.svelte.ts`, `src/lib/components/Toaster.svelte`, `src/lib/test/DialogHarness.svelte`
- Modify: `src/routes/+layout.svelte` (añadir `<Toaster />`)
- Test: `src/lib/components/unsaved.spec.ts`, `src/lib/components/Dialog.svelte.spec.ts`, `src/lib/components/Toaster.svelte.spec.ts`

**Interfaces:**
- Consumes: `keymap`, `KeyAction`, `keyLabel` (tarea 5).
- Produces:
  - `<Dialog open=$bindable(false) title actions: KeyAction[] children?>`: `<dialog>` nativo con `showModal()`. Mientras está abierto pone una capa modal con `actions` y las pinta como botones grandes (`<kbd>` + texto). Al abrir guarda el foco y al cerrar lo devuelve. El evento nativo `cancel` lleva `preventDefault`: Esc solo hace lo que digan las acciones.
  - `unsaved.ts`:

```ts
export interface PendingNavigation { to: { url: URL } | null; willUnload: boolean; cancel(): void }
export function createUnsavedGuard(options: {
	isDirty: () => boolean;
	save: () => Promise<boolean>;
	goto: (url: URL) => Promise<void>;
	onAsk: (asking: boolean) => void;
}): { check(nav: PendingNavigation): void; keepEditing(): void; discard(): Promise<void>; saveAndLeave(): Promise<void> };
```

  - `<UnsavedGuard isDirty save>`: `beforeNavigate((nav) => guard.check(nav))` y un `Dialog` titulado "Hay cambios sin guardar" con las acciones Enter 'Seguir editando', Escape oculta → seguir editando, NumpadAdd 'Guardar y salir' y F8 'Salir sin guardar'.
  - `toast(message: string, durationMs = 3000): void`; `<Toaster />` con `role="status"`, encima de la barra de teclas.

- [ ] **Step 1: Escribir los tests**

```ts
// unsaved.spec.ts (node), con nav = { to: { url }, willUnload: false, cancel: vi.fn() }
it('lets clean forms leave')                              // isDirty false → ni cancel ni onAsk
it('stops a dirty form and asks')                         // cancel llamado, onAsk(true)
it('closing the window with changes uses the browser warning') // willUnload → cancel, sin onAsk
it('keep editing closes the question')                    // onAsk(false), goto no
it('discard leaves to the requested page')                // goto(url); un check() durante ese goto no cancela
it('save and leave only leaves when saving works')        // save false → sin goto; true → goto(url)

// Dialog.svelte.spec.ts: DialogHarness = KeyboardRoot + KeyBar + input 'Nombre' + useKeys([NumpadAdd 'Guardar' → spy])
// + botón 'Abrir' que abre un Dialog con [Enter 'Vale' → spy y cierra, Escape oculta → cierra]
it('shows its actions as buttons and in the key bar')     // botón 'Vale' visible; la barra muestra 'Vale' y no 'Guardar'
it('Enter runs the dialog action')                        // spy de Vale llamado una vez (sin doble clic nativo)
it('page keys do not reach the page while open')          // NumpadAdd → spy de Guardar no llamado
it('focus returns to where it was')                       // foco en Nombre, abrir, {Escape} → Nombre toHaveFocus

// Toaster.svelte.spec.ts
it('shows a message and hides it')                        // toast('Albarán 121 guardado', 200) → visible; luego desaparece
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según las interfaces. `UnsavedGuard` pasa `goto` de `$app/navigation` y abre el diálogo con `onAsk`.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS. La prueba de punta a punta del aviso va en el bloque de catálogo (spec §5.6).

- [ ] **Step 5: Commit**

```bash
git add front/src
git commit -m "add dialog, unsaved changes guard and toasts"
```

---

### Task 11: Errores en pantalla

**Files:**
- Create: `src/lib/components/errors.svelte.ts`, `src/lib/components/ErrorDialog.svelte`, `src/lib/components/ConnectionScreen.svelte`, `src/routes/+error.svelte`
- Modify: `src/routes/+layout.svelte`
- Test: `src/lib/components/ErrorDialog.svelte.spec.ts`, `src/lib/components/ConnectionScreen.svelte.spec.ts`

**Interfaces:**
- Consumes: `classifyError`, `errorMessages`, `ApiError` (tarea 4); `Dialog` (tarea 10); `keymap` (tarea 5).
- Produces:
  - `errorState: { view: ErrorView | null; connectionLost: boolean }` (reactivo), `showError(err: unknown): void` y `dismissError(): void`.
  - `<ErrorDialog />`:
    - Con `business`: título "No se ha podido hacer" y el mensaje.
    - Con `unexpected`: título "Error inesperado", el texto de `INTERNAL_ERROR` y un `<details>` con resumen "Detalles técnicos" (estado, código, detalle).
    - Acción Enter 'Entendido' (y Escape oculta) → `dismissError()`.
  - `<ConnectionScreen ping? intervalMs=3000 onRestored?>`:
    - Capa a pantalla completa con "No se puede conectar con SelfBill" y "Comprueba que el programa sigue abierto. Reintentando…".
    - Capa de teclas modal con Enter 'Reintentar', que hace ping al momento.
    - `ping` por defecto: `fetch('/api/articles/next-code')`; cualquier respuesta cuenta como conexión.
    - Al volver la conexión: `errorState.connectionLost = false` y `onRestored?.()`.

Cambios en el layout:
- Monta `<ErrorDialog />`.
- Monta `{#if errorState.connectionLost}<ConnectionScreen onRestored={invalidateAll} />{/if}`.
- Añade en `onMount` un listener de `unhandledrejection` que hace `showError(event.reason)` y `event.preventDefault()`.

`+error.svelte` según `page.status`:
- 404: "No se ha encontrado. Puede que se haya borrado."
- 503: `<ConnectionScreen onRestored={invalidateAll} />`.
- Cualquier otro: "No se ha podido cargar esta pantalla."
- Siempre con Escape 'Volver' → `goto(resolve('/'))`.

- [ ] **Step 1: Escribir los tests**

```ts
// ErrorDialog.svelte.spec.ts (con KeyboardRoot)
it('explains a business error in spanish')   // showError(new ApiError(409,'INVOICE_MIXED_MONTHS','Detail EN',{})) → texto de errorMessages visible; 'Detail EN' no
it('closes with Enter')                      // {Enter} → el diálogo desaparece, errorState.view === null
it('hides technical details of unexpected errors') // ApiError(500,'INTERNAL_ERROR','Internal server error') → 'Ha ocurrido un error inesperado…' visible; 'Detalles técnicos' visible; 'Internal server error' en el DOM pero no visible
it('connection errors raise the connection screen flag') // showError(networkError(...)) → errorState.connectionLost true, sin diálogo

// ConnectionScreen.svelte.spec.ts
it('retries on its own and closes when the api answers') // ping: false y luego true, intervalMs 50 → onRestored llamado, connectionLost false
it('Enter retries at once')                               // ping espía; {Enter} → ping llamado sin esperar al intervalo
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según las interfaces.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS. `+error.svelte` se prueba de punta a punta en el bloque de catálogo, que es el primero con `load`.

- [ ] **Step 5: Commit**

```bash
git add front/src
git commit -m "show errors and lost connection in spanish"
```

---

### Task 12: Listados

**Files:**
- Create: `src/lib/url.ts`, `src/lib/components/table.ts`, `src/lib/components/DataTable.svelte`, `src/lib/test/TableHarness.svelte`
- Test: `src/lib/url.spec.ts`, `src/lib/components/DataTable.svelte.spec.ts`

**Interfaces:**
- Consumes: `useKeys`, `isEditable` (tarea 5); `formatMoney` (tarea 3, solo en el harness).
- Produces:
  - `withParams(url: URL, params: Record<string, string | number | null | undefined>): string`: devuelve `pathname + search`. `null` o `undefined` quitan el parámetro. `page` es el número de página de la API, que empieza en 0.
  - `interface Column<T> { label: string; value: (row: T) => string; align?: 'left' | 'right' }`
  - `<DataTable<T> columns rows onOpen: (row: T) => void page?: { number: number; totalPages: number } onPage?: (n: number) => void emptyText='No hay nada que mostrar.'>`:
    - Fila seleccionada con `aria-selected`. Clic selecciona y doble clic abre.
    - Teclas: ↑/↓ (ocultas); Enter 'Abrir' (`when`: hay filas y el foco no está en un campo editable); AvPág 'Página siguiente' y RePág 'Página anterior' solo si existen.
    - Pie "Página {number + 1} de {totalPages}" cuando hay más de una página.
    - La selección vuelve a la primera fila cuando cambian las filas. Las columnas `right` llevan `.tabular`.

- [ ] **Step 1: Escribir los tests**

```ts
// url.spec.ts
const u = new URL('http://x/clients?page=2&q=pe');
expect(withParams(u, { page: 3 })).toBe('/clients?page=3&q=pe');
expect(withParams(u, { q: null })).toBe('/clients?page=2');
expect(withParams(new URL('http://x/clients'), { page: 0 })).toBe('/clients?page=0');

// DataTable.svelte.spec.ts: TableHarness = KeyboardRoot + KeyBar + DataTable con 3 filas
// (Frutas Paco 84, Bar Pepe 32, Hotel Sol 210), onOpen y onPage espías
it('renders the rows')                               // 'Bar Pepe' y '32,00 €' visibles
it('arrows move the selection and Enter opens it')   // {ArrowDown}{Enter} → onOpen(rows[1])
it('the selection stays inside the list')            // {ArrowUp} en la primera → sigue en 0; 5×{ArrowDown} → la última
it('double click opens a row')
it('page keys ask for the next and previous page')   // page {number:0,totalPages:3}: {PageDown} → onPage(1); {PageUp} → no; 'Página 1 de 3' visible
it('an empty list says so')                          // rows [] → 'No hay nada que mostrar.'; {Enter} → onOpen no
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL.

- [ ] **Step 3: Implementar** según las interfaces.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add front/src/lib
git commit -m "add data table and url params helper"
```

---

### Task 13: Empaquetado y pruebas de punta a punta

**Files:**
- Modify: `selfbill-api/pom.xml`, `front/package.json`, `front/playwright.config.ts`, `front/tsconfig.json` (añadir `"e2e"` a `include`)
- Create: `front/e2e/serve-jar.mjs`, `front/e2e/navigation.e2e.ts`

**Interfaces:**
- Consumes: el fallback de la tarea 1 y la app de las tareas 5 y 6.
- Produces:
  - `npm run package`: compila el front y genera el jar con el front dentro.
  - `npm run test:e2e`: `npm run package && playwright test`.
  - `node e2e/serve-jar.mjs`: arranca el jar en `127.0.0.1:18080` con una SQLite y una clave temporales.

- [ ] **Step 1: Copiar el build al jar**

En `pom.xml`, `maven-resources-plugin` con una ejecución `copy-front`:
- Fase `prepare-package`, objetivo `copy-resources`.
- `outputDirectory` `${project.build.outputDirectory}/static`.
- Recurso `${project.basedir}/../front/build`.

Si `front/build` no existe se salta sin fallar, así que `mvn test` sigue sin depender del front.

`package.json`: `"package": "npm run build && cd ../selfbill-api && mvnw -q clean package -DskipTests"`. En Windows npm ejecuta los scripts con `cmd`, donde `mvnw` es `mvnw.cmd`.

- [ ] **Step 2: Escribir `e2e/serve-jar.mjs`**

- Busca `../selfbill-api/target/selfbill-api-*.jar` (no `.original`).
- Crea una carpeta temporal con `fs.mkdtempSync(path.join(os.tmpdir(), 'selfbill-e2e-'))`.
- Usa `java` de `JAVA_HOME/bin` si existe y si no `java`.
- Lo lanza con `-jar <jar> --server.port=18080 --spring.datasource.url=jdbc:sqlite:<tmp>/e2e.db?foreign_keys=true&journal_mode=WAL&transaction_mode=IMMEDIATE --selfbill.credentials.key-file=<tmp>/secret.key` y `stdio: 'inherit'`.
- Mata el proceso hijo al recibir `SIGINT`/`SIGTERM` o al salir.

`playwright.config.ts`:
- `testDir: 'e2e'`, `testMatch: '**/*.e2e.ts'`.
- `use: { baseURL: 'http://127.0.0.1:18080' }`.
- `webServer: { command: 'node e2e/serve-jar.mjs', url: 'http://127.0.0.1:18080/', reuseExistingServer: false, timeout: 120_000 }`.

- [ ] **Step 3: Escribir `e2e/navigation.e2e.ts`**

```ts
test('opens a section with its number and comes back with Esc')
// goto(resolve('/')) → 6 tiles; press('1') → URL /delivery-notes, heading 'Albaranes'; press('Escape') → URL '/'
test('numpad digits work too')               // press('Numpad4') → /articles, heading 'Artículos'
test('the top bar opens any section')        // click en 'Facturas' dentro de <nav> → /invoices
test('a section survives a reload')          // goto('/clients') → heading 'Clientes'; reload → sigue (fallback del jar)
test('the key bar shows the keys of the screen') // en /clients el <footer> contiene 'Esc' y 'Salir'
test('the api still answers next to the front')  // request.get('/api/articles/next-code') → ok, JSON con 'code'
```

- [ ] **Step 4: Ejecutar**

Run: `npx playwright install chromium && npm run test:e2e`
Expected: 6 passed. Aquí no hay fase roja: las rutas existen desde la tarea 6, y estos tests prueban el empaquetado y el fallback del jar.

- [ ] **Step 5: Verificar que la API no depende del front**

Run (en `selfbill-api/`): `./mvnw test`
Expected: 128 tests, 0 fallos.

- [ ] **Step 6: Commit**

```bash
git add selfbill-api/pom.xml front
git commit -m "package the front inside the api jar and add e2e tests"
```

---

### Task 14: Comprobación en Edge y documentación

**Files:**
- Modify: `CLAUDE.md`, `front/README.md`
- Modify (solo si una tecla F no se puede anular): `front/src/lib/keyboard/keys.ts`, spec §5.3

- [ ] **Step 1: Arrancar como lo verá el usuario**

Run (en `front/`): `npm run package && node e2e/serve-jar.mjs`. En otra consola: `start msedge --app=http://127.0.0.1:18080/`.

- [ ] **Step 2: Comprobar a mano**

1. La ventana no tiene pestañas ni barra de direcciones.
2. En el inicio, de 1 a 6 abren cada sección, y Esc vuelve.
3. F1, F2, F3, F4, F7, F8 y F9 no hacen nada visible: ni ayuda de Edge, ni barra de búsqueda, ni aviso de navegación con cursor.
4. F5 recarga y se queda en la misma sección.
5. La barra de teclas se lee bien a 1920×1080 y no estorba.
6. `netstat -an | findstr 18080` muestra solo `127.0.0.1:18080`.

Si una tecla F no se puede anular: se quita de las que usa la app, se apunta en el spec §5.3 y se cambia por otra libre.

- [ ] **Step 3: Documentar**

- `CLAUDE.md`: sección "Front" con `npm run dev` (necesita la API en el 8080), `npm run api:types` (tras cambiar la API), `npm test`, `npm run test:e2e` y `npm run package`.
- `front/README.md`: los mismos comandos, más un enlace al spec.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md front/README.md
git commit -m "document front commands"
```
