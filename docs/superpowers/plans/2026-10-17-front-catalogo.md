# Front, catálogo: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pantallas de artículos y clientes (lista con buscador, ficha de alta y edición, borrado con aviso, alta en cadena, avisos de la ficha de cliente) sobre una base de campos y teclado endurecida, con el filtro `q` de clientes en la API.

**Architecture:** Dos piezas compartidas: `ListScreen` (buscador + `DataTable`) y `RecordForm`/`RecordScreen` (estado y acciones de una ficha; es el `useRecordForm` del spec, partido en una clase y el componente que la pinta, porque una función no puede renderizar diálogos). Reciben la navegación y la API inyectadas, así que se prueban fuera de la app. Las pantallas de cada entidad son concretas y finas. Las rutas de SvelteKit solo cargan datos y conectan con `goto`.

**Tech Stack:** SvelteKit 3 (Svelte 5 runes, TS 6), Tailwind 4, Vitest 4 (navegador), Playwright 1.60, `openapi-fetch`. API: Java 25, Spring Boot 3.5.7, Spring Data JPA con `Specification`.

**Spec:** [docs/superpowers/specs/2026-10-17-front-catalogo-design.md](../specs/2026-10-17-front-catalogo-design.md). La base: [2026-10-13-front-base-design.md](../specs/2026-10-13-front-base-design.md).

## Requisitos previos

- Rama `feature/front-catalogo`, que ya tiene el commit del spec.
- **Por instrucción del usuario:** cada tarea se commitea en local en cuanto pasa su revisión, y se sigue con la siguiente. El usuario revisa la PR final. Los implementadores no commitean: el paso "Commit" lo hace el controlador tras la revisión.
- Node 22.18. `JAVA_HOME` apunta al JDK 25; el `java` del PATH es el JDK 24.
- Comandos del front desde `front/`, y de la API desde `selfbill-api/` (`./mvnw`).
- Líneas base: `./mvnw test` 128 tests; `npm test` 145 tests; `npm run test:e2e` 6 tests.

## Global Constraints

- Convenciones de la base: `#lib/<ruta>.js` fuera de `lib/`; `resolve()` en todo `href`/`goto` interno; un `useKeys`/`keyLayer` por componente; "+" = `event.code === 'NumpadAdd'`; los textos de la API nunca se muestran.
- Sin dependencias nuevas.
- Textos en pantalla en español, exactos como en el spec y en este plan; código y comentarios en inglés.
- Las pruebas de punta a punta existentes siguen pasando: las listas tienen como encabezado "Artículos" y "Clientes", y con el buscador vacío la barra muestra "Esc Salir".
- Tests: node en `*.spec.ts`, navegador en `*.svelte.spec.ts`, punta a punta en `front/e2e/*.e2e.ts`. Primero el test, luego el código.
- Punta a punta en el puerto 18080, con base temporal, `workers: 1` y códigos fijos y distintos en cada prueba (la base es nueva en cada ejecución).
- Cada tarea del front termina con `npm run check`, `npm run lint` y `npm test` en verde. Cada tarea de la API, con `./mvnw test` en verde.

## Review Focus

1. Guardar un cliente con los opcionales vacíos (email, código postal) funciona: viajan como `null` y la API no los rechaza (tarea 7, `clientPayload` y la prueba de punta a punta del alta mínima).
2. Dos "+" seguidos en un alta crean un solo registro, y el siguiente código avanza una vez (tarea 5, `a second + while saving does nothing`).
3. Tras un alta en cadena, Esc en la ficha vacía vuelve a la lista sin el aviso de cambios sin guardar (tarea 5, `the chained blank form is clean`).
4. Buscar un código que no existe dice "No existe ningún cliente con el código 7." y Enter no abre nada (tarea 5, mensajes de `ListScreen`).
5. Cambiar el IVA de un cliente sin pendientes guarda sin preguntar; con pendientes pregunta, y confirmar guarda una sola vez (tarea 5, `confirmation before saving only when asked`; tarea 7, prueba de punta a punta).

Además, ampliado con el usuario al revisar el plan: buscar sin tildes ni mayúsculas ("garcia" encuentra "García" y "GARCÍA"), con una columna normalizada en la API (tarea 1, `ignoresAccentsAndCase`; tarea 7, prueba de punta a punta).

## Mapa de ficheros

API: `main/` = `selfbill-api/src/main/java/com/luispuchol/selfbill/selfbill_api/`, `test/` = `selfbill-api/src/test/java/com/luispuchol/selfbill/selfbill_api/`. Front: rutas relativas a `front/`.

| Fichero | Responsabilidad | Tarea |
|---|---|---|
| `main/shared/SearchText.java`, `db/migration/V4__Search_text.java`, `Client.java`, `Article.java`, filtros y specifications, sus tests, `src/lib/api/schema.d.ts` | Búsqueda normalizada, filtro `q` y tipos | 1 |
| `src/lib/components/select-on-focus.ts`, los seis campos, `Field.svelte` | Endurecer campos | 2 |
| `src/lib/keyboard/keys.ts`, `form-nav.ts`, `src/lib/components/unsaved.ts`, `UnsavedGuard.svelte`, `DataTable.svelte` | Endurecer teclado y ficha | 3 |
| `src/lib/api/client.ts`, `load.ts`, `src/routes/+layout.svelte`, `src/app.html` | Endurecer la app | 4 |
| `src/lib/records/{search,payload,list-memory}.ts`, `record-form.svelte.ts`, `src/lib/components/{ListScreen,RecordScreen}.svelte` | Piezas compartidas | 5 |
| `src/lib/catalog/{pending,article-form}.ts`, `src/routes/articles/**` | Artículos | 6 |
| `src/lib/catalog/{billing,client-form}.ts`, `src/routes/clients/**`, `front/e2e/clients.e2e.ts` | Clientes | 7 |
| `PENDIENTES.md`, `front/README.md` | Documentación | 8 |

Los componentes de prueba (`*Harness.svelte`) van en `src/lib/test/`.

---

### Task 1: Búsqueda sin tildes ni mayúsculas y filtro `q` en la API

El `lower()` de SQLite solo entiende ASCII: `lower('GARCÍA')` da `'garcÍa'`, así que "garcia" no
encuentra "García" (comprobado). Se busca sobre una columna `search_text` ya normalizada en Java: sin
tildes ni diéresis y en minúsculas. Decidido con el usuario al revisar el plan.

**Files:**
- Create: `main/shared/SearchText.java`, `test/shared/SearchTextTest.java`
- Create: `selfbill-api/src/main/java/db/migration/V4__Search_text.java` (migración Java de Flyway, paquete `db.migration`)
- Modify: `main/catalog/client/Client.java`, `main/catalog/article/Article.java`
- Modify: `main/catalog/client/ClientFilter.java`, `main/catalog/client/ClientSpecification.java`, `main/catalog/article/ArticleFilter.java` (descripción), `main/catalog/article/ArticleSpecification.java`
- Create: `test/catalog/client/ClientQueriesTest.java`, `test/catalog/article/ArticleQueriesTest.java`, `test/SearchTextBackfillTest.java`
- Modify: `test/OpenApiDocsTest.java`
- Modify: `front/src/lib/api/schema.d.ts` (regenerado)

**Interfaces:**
- Produces:
  - `SearchText.normalize(String... parts): String`. Une las partes no nulas con un espacio, aplica NFD, quita las marcas (`\p{M}`), pasa a minúsculas con `Locale.ROOT` y recorta. `"GARCÍA"` → `"garcia"`, `"Peña"` → `"pena"`.
  - Columna `search_text TEXT NOT NULL DEFAULT ''` en `clients` y `articles`:
    - cliente: `normalize(name, businessName, nif)`;
    - artículo: `normalize(name)`.
  - Se mantiene con un método `@PrePersist @PreUpdate` en cada entidad (`refreshSearchText()`), y el campo se mapea como `@Column(name = "search_text", nullable = false)`.
  - `V4__Search_text extends BaseJavaMigration`: `ALTER TABLE … ADD COLUMN` en las dos tablas y después `backfill(Connection)`. Es un método estático público que rellena `search_text` de todas las filas, también las borradas, con `SearchText.normalize` (selects por `id` y updates preparados).
  - `GET /api/clients?q=`: `like` de `'%' + normalize(q) + '%'` sobre `search_text`; Y con los demás filtros; en blanco se ignora.
  - `GET /api/articles?name=`: el filtro existente pasa a comparar sobre `search_text` con el texto normalizado.
  - Los filtros `name` y `businessName` de clientes no cambian: el front no los usa.
  - En el front: `paths['/api/clients']['get']['parameters']['query']['q']`.

- [ ] **Step 1: Escribir los tests**

```java
// SearchTextTest (unitario)
assertThat(SearchText.normalize("García")).isEqualTo("garcia");
assertThat(SearchText.normalize("GARCÍA")).isEqualTo("garcia");
assertThat(SearchText.normalize("Peña")).isEqualTo("pena");
assertThat(SearchText.normalize("Bar Pepe", null, "B1")).isEqualTo("bar pepe b1");

// ClientQueriesTest extends DomainIntegrationTest: clientService.getAllClients(ClientFilter.builder()…build(), PageRequest.of(0, 20));
// clientes creados con clientService.createClient(clientRequest(…)), ajustando nombre, razón social y NIF con el builder:
//   (10, "Frutería García", "Frutas Levante SL", "B11111111"),
//   (11, "Bar Pepe", "Hostelería GARCÍA SC", "B22222222"),
//   (12, "Hotel Sol", "Turismo SA", "A33333333")
@Test void findsByTradeNameBusinessNameOrNif()   // q "garcia" → 10 y 11; q "B2222" → 11; q "HOTEL" → 12
@Test void ignoresAccentsAndCase()                // q "GARCÍA" → 10 y 11; q "frutería" → 10
@Test void combinesWithTheCodeRange()             // q "garcia" + codeFrom 11 → solo 11
@Test void ignoresABlankQ()                       // q "  " → los tres (más el cliente 1 de DomainIntegrationTest)
@Test void keepsSearchTextUpToDateOnUpdate()      // actualizar el nombre del 12 a "Hotel Mar" → q "mar" → 12

// ArticleQueriesTest extends DomainIntegrationTest
@Test void findsByNameIgnoringAccentsAndCase()   // "Plátano canario" → name "platano" y "PLÁTANO" lo encuentran

// SearchTextBackfillTest extends DomainIntegrationTest
@Test void backfillNormalizesExistingRows()
// jdbc: UPDATE clients SET search_text = '' (y articles); V4__Search_text.backfill(conexión del DataSource);
// el cliente 1 de DomainIntegrationTest queda con normalize(name, businessName, nif)
```

En `OpenApiDocsTest.documentsTheApi`, añadir: `jsonPath("$.paths['/api/clients'].get.parameters[?(@.name == 'q')]").exists()`.

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `./mvnw test -Dtest=SearchTextTest,ClientQueriesTest,ArticleQueriesTest,SearchTextBackfillTest,OpenApiDocsTest`
Expected: FAIL, no compila (no existen `SearchText`, `q` ni la migración).

- [ ] **Step 3: Implementar**

- `ClientFilter`: nuevo componente `@Schema(description = "Part of the trade name, the legal name or the NIF, ignoring case and accents") String q`.
- `ArticleFilter.name`: descripción "Part of the name, ignoring case and accents".
- `SchemaTest` debe seguir pasando: Hibernate valida que `search_text` exista tras la migración.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `./mvnw test`
Expected: los 128 de antes más los nuevos, 0 fallos, `ModularityTest` incluido.

- [ ] **Step 5: Regenerar los tipos del front**

Arrancar la API en el 8080 con base y clave desechables:

```bash
./mvnw -q spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:sqlite:target/types.db --selfbill.credentials.key-file=target/types.key"
```

Después, `npm run api:types` en `front/` y parar la API (`netstat -ano | grep 8080`, `taskkill //PID <pid> //F`). Run: `npm run check`. Expected: 0 errores, y `schema.d.ts` tiene `q?: string` en la consulta de `/api/clients`.

- [ ] **Step 6: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 2: Endurecer los campos

**Files:**
- Create: `src/lib/components/select-on-focus.ts`
- Modify: `src/lib/components/{TextField,NumberField,DigitsField,DateField,CodeField,ChoiceField,Field}.svelte`
- Test: `src/lib/components/Fields.svelte.spec.ts`, `Form.svelte.spec.ts`, `src/lib/test/{FieldsHarness,FormHarness}.svelte`

**Interfaces:**
- Produces:
  - `selectOnFocus(node: HTMLInputElement): { destroy(): void }` (acción `use:selectOnFocus`).
  - `TextField` gana `validate?: (value: string) => string | undefined` y `maxlength?: number`.
  - `NumberField` con `decimals={0}` sirve para códigos enteros.

Reglas:
- **Selección al entrar:** `selectOnFocus` sustituye a `onfocus={() => input.select()}` en Text, Number, Digits, Date y Code. Un `mousedown` sobre el campo sin foco marca el clic; en el `focus` se selecciona todo; en el primer `mouseup` tras ese clic se hace `preventDefault`, para que el navegador no deshaga la selección. Un clic en el campo ya enfocado coloca el cursor con normalidad.
- **Decimal del pad:** `NumberField` solo actúa con `code === 'NumpadDecimal'` si `key` es "." o "," y no hay Ctrl, Alt ni Meta. Con `decimals = 0` la tecla decimal no inserta nada, y un texto con decimales da "Escribe un número entero".
- **Error externo:** `error || ownError` en los seis campos.
- **Cuándo se avisa:**
  - Al salir (`onblur`), solo lo mal escrito: Number (no es un número), Digits (longitud distinta de `length`), Date (fecha no válida) y Text (resultado de `validate` si no está vacío).
  - "Este dato es obligatorio" solo en el `commit` (Enter o `validateAll`).
  - `DateField` deja de avisar de "obligatorio" al salir.
  - `CodeField` no valida al salir.
- **`TextField`:** aplica `validate` al salir y en el `commit` (si no está vacío), y pasa `maxlength` al `<input>`.
- **`Field`:** renderiza siempre `<p id={errorId} class="min-h-[1.5em] text-danger" aria-live="polite">`, con el texto solo si hay error, para no desplazar el contenido. El snippet sigue recibiendo `describedBy` solo si hay error.
- **Autocompletado:** `autocomplete="off"` en todos los `<input>` de los campos.

- [ ] **Step 1: Escribir los tests**

```ts
it('clicking into a field selects its content')       // NumberField con 10,00: userEvent.click y teclear '4' → '4'
it('the numpad decimal key with NumLock off is left alone')
// keydown {key:'Delete', code:'NumpadDecimal'} → defaultPrevented false, texto igual;
// {key:'.', code:'NumpadDecimal'} → defaultPrevented true
it('an integer field ignores the decimal key')        // decimals 0: '10', keydown '.' NumpadDecimal → '10'
it('an empty error prop does not hide the own error') // TextField required, error "" → Enter → 'Este dato es obligatorio'
it('leaving a field reports what is mistyped')        // Digits length 8 '0121' + Tab → 'Tienen que ser 8 cifras'
it('leaving a field does not report what is missing') // Text required vacío + Tab → sin 'Este dato es obligatorio'; Enter → sí
it('leaving an empty required date says nothing')     // DateField required vacío + Tab → sin error
it('a text field runs its own check on leaving')      // validate = v => v.includes('@') ? undefined : 'Revisa el email'; 'pepe' + Tab → 'Revisa el email'
it('the error line keeps its place')                  // sin error: existe el <p> del error, vacío
it('fields turn autocomplete off')                    // cada input tiene autocomplete="off"
```

Si algún test existente esperaba el error de "obligatorio" de la fecha al salir, se cambia al comportamiento nuevo.

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npx vitest --run --project client src/lib/components/Fields.svelte.spec.ts src/lib/components/Form.svelte.spec.ts`
Expected: FAIL en los tests nuevos.

- [ ] **Step 3: Implementar** según las reglas de arriba.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 3: Endurecer el teclado y la ficha

**Files:**
- Modify: `src/lib/keyboard/keys.ts`, `src/lib/keyboard/form-nav.ts`, `src/lib/components/unsaved.ts`, `src/lib/components/UnsavedGuard.svelte`, `src/lib/components/DataTable.svelte`
- Test: `src/lib/keyboard/keys.spec.ts`, `src/lib/keyboard/form-nav.svelte.spec.ts`, `src/lib/components/unsaved.spec.ts`, `src/lib/components/DataTable.svelte.spec.ts`, `src/lib/test/TableHarness.svelte`

**Interfaces:**
- Produces:
  - `KeyEventLike` gana `repeat`; `dispatch` ignora `event.repeat` salvo `ArrowUp`, `ArrowDown`, `PageUp` y `PageDown`.
  - `FormNav.focusFirstField(): void`: el primer campo en orden de página.
  - `FormNav.restoreFocus(): void`: el último campo registrado que tuvo el foco, o el primero.
  - `createUnsavedGuard` gana la opción `onClosed?: () => void`, llamada cuando el aviso se cierra sin salir (`keepEditing` o un `saveAndLeave` cuyo `save` devuelve false). Un `saveAndLeave` fallido ahora cierra el aviso (`onAsk(false)`).
  - `UnsavedGuard` gana la prop `restoreFocus?: () => void`, llamada en `onClosed` después de `await tick()`, cuando `Dialog` ya devolvió su foco.
  - `DataTable` gana `selected = $bindable(0)`. Sus `when` excluyen el foco en `BUTTON` o `A` además de los editables, y se reevalúan al moverse el foco (`createSubscriber` sobre `focusin`/`focusout`, como `FormNav.hasFocus`).

- [ ] **Step 1: Escribir los tests**

```ts
// keys.spec.ts
it('ignores auto-repeat except arrows and page keys') // Enter repeat → no ejecuta; ArrowDown repeat → ejecuta
it('numpad plus saves from a text field')             // {key:'+', code:'NumpadAdd', target:{tagName:'INPUT'}} → ejecuta
it('main keyboard plus keeps typing in a text field') // {key:'+', code:'BracketRight', target:{tagName:'INPUT'}} → no ejecuta, sin preventDefault

// form-nav.svelte.spec.ts
it('restoreFocus goes back to the last focused field') // foco en el 2.º campo, luego en un botón de fuera → restoreFocus → 2.º campo
it('focusFirstField focuses the first in page order')

// unsaved.spec.ts
it('a failed save and leave closes the question')     // save → false: onAsk(false), onClosed llamado, goto no
it('keep editing calls onClosed')

// DataTable.svelte.spec.ts (TableHarness gana un botón "Otro" y pinta `selected`)
it('Enter does not open a row while a button has focus') // foco en "Otro", Enter → onOpen no
it('bind:selected follows the arrows')                   // ↓ → selected 1
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL en los tests nuevos.

- [ ] **Step 3: Implementar** según las interfaces.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 4: Endurecer la app

**Files:**
- Modify: `src/lib/api/client.ts`, `src/lib/api/load.ts`, `src/routes/+layout.svelte`, `src/app.html`
- Test: `src/lib/api/client.spec.ts`, `src/lib/api/load.spec.ts`

**Interfaces:**
- Produces:
  - `request()`: un rechazo es `NETWORK` solo si es `TypeError` o un `DOMException` con `name` `AbortError` o `TimeoutError`. Cualquier otro es `new ApiError(0, 'UNKNOWN', <mensaje>, {})`.
  - `loaded()`: `NETWORK` → `error(503, 'Sin conexión')`; otro `ApiError` → `error(status || 500, 'Error')`. Ya no importa `messages.ts`.

Cambios sin test de componente (usan `$app/*`):
- **Layout:** el listener de `unhandledrejection` hace `console.error(event.reason)` antes de `showError`. Un listener nuevo de `error` llama a `showError(event.error)` si `event.error instanceof Error`, sin `preventDefault`, así queda en la consola. Ambos se quitan al desmontar.
- **`inert`:** `<main inert={errorState.connectionLost}>` y `TopBar` dentro de `<div inert={errorState.connectionLost}>`.
- **`app.html`:** `data-sveltekit-preload-data="tap"`.

- [ ] **Step 1: Escribir los tests**

```ts
// client.spec.ts
await expect(request(Promise.reject(new DOMException('t', 'TimeoutError')))).rejects.toMatchObject({ code: 'NETWORK' });
await expect(request(Promise.reject(new SyntaxError('Unexpected token <')))).rejects.toMatchObject({ code: 'UNKNOWN', status: 0 });
// el detail del UNKNOWN contiene 'Unexpected token <'; el test existente de TypeError → NETWORK sigue igual

// load.spec.ts: los casos de 404 y 503 siguen; se quita cualquier aserción sobre el texto del mensaje
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npx vitest --run --project server src/lib/api`
Expected: FAIL en el caso `SyntaxError` (hoy da `NETWORK`).

- [ ] **Step 3: Implementar** según las interfaces y los cambios de arriba.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test && npm run build`
Expected: PASS.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 5: Piezas compartidas: `ListScreen` y `RecordForm`

**Files:**
- Create: `src/lib/records/search.ts`, `payload.ts`, `list-memory.ts`, `record-form.svelte.ts`
- Create: `src/lib/components/ListScreen.svelte`, `src/lib/components/RecordScreen.svelte`
- Create: `src/lib/test/ListHarness.svelte`, `src/lib/test/RecordHarness.svelte`
- Test: `src/lib/records/{search,payload,list-memory}.spec.ts`, `src/lib/components/ListScreen.svelte.spec.ts`, `src/lib/components/RecordScreen.svelte.spec.ts`

**Interfaces:**
- Consumes: `DataTable` con `bind:selected` y `FormNav.focusFirstField`/`restoreFocus` (tarea 3), `Form` con `bind:nav`, `Dialog`, `useKeys`, `ApiError`, `fieldErrorMessages`, `showError`.
- Produces:

```ts
// search.ts
export function isCodeSearch(text: string): boolean;          // solo cifras tras recortar
export function listQuery(text: string, textParam: 'name' | 'q'):
	{ codeFrom?: number; codeTo?: number; name?: string; q?: string };
// payload.ts
export function trimOrNull(value: string | null | undefined): string | null;
// list-memory.ts
export interface ListMemory { href: string; openedId?: number }
export function rememberList(section: string, memory: ListMemory): void;
export function recallList(section: string): ListMemory | undefined;

// record-form.svelte.ts
export interface Saved { id: number; code: number }
export interface RecordFormOptions<V extends { code: number | null }> {
	entity: { one: string; Label: string; duplicateCode: ErrorCode }; // { one: 'cliente', Label: 'Cliente', duplicateCode: 'CLIENT_DUPLICATE_CODE' }
	id: number | null;                                   // null = alta
	initial: V;
	toPayload(values: V): unknown;
	create(payload: unknown): Promise<Saved>;
	update(id: number, payload: unknown): Promise<Saved>;
	remove(id: number): Promise<void>;
	blank(): Promise<V>;                                 // la siguiente alta, con el siguiente código
	pendingForDelete(id: number): Promise<string | null>; // cuerpo del aviso, o null
	confirmBeforeSave?(values: V, original: V): Promise<string | null>;
	fieldMessages?: Record<string, string>;
	goToList(savedId?: number): void | Promise<void>;
	toast(message: string): void;
}
export class RecordForm<V extends { code: number | null }> {
	constructor(options: RecordFormOptions<V>);
	values: V;                                           // $state, enlazable desde los campos
	nav: FormNav | undefined;                            // lo rellena RecordScreen (bind:nav)
	readonly id: number | null;
	readonly isDirty: boolean;
	deleteDialog: { title: string; body: string | null } | null;
	confirmMessage: string | null;
	errorFor(name: string): string | undefined;          // error de la API mientras el campo no cambie
	save(): Promise<boolean>;
	confirmSave(): Promise<void>;
	cancelConfirm(): void;
	askDelete(): Promise<void>;
	confirmDelete(): Promise<void>;
	cancelDelete(): void;
	exit(): void;                                        // goToList()
	restoreFocus(): void;                                // primer campo con error; si no hay, nav.restoreFocus()
}
```

**`ListScreen.svelte`** (`generics="T"`):
- Props: `title`, `entity: { one; many }`, `columns: Column<T>[]`, `rows: T[]`, `page: { number; totalPages }`, `search: string`, `selectId?: number`, `rowId: (row: T) => number`, `onSearch(text)`, `onPage(n)`, `onOpen(row)`, `onNew()`, `onExit()`.
- Encabezado `<h1>{title}</h1>` y buscador `TextField`-like propio (`<input>` con etiqueta "Buscar (código o nombre)", `use:selectOnFocus`, `autocomplete="off"`), enfocado al montar.
- `onSearch` 300 ms después de la última tecla; Esc con texto lo vacía y llama a `onSearch('')` al momento.
- Un `useKeys`:
  - ↑/↓ ocultas, cuando el foco está en el buscador → mueven `selected` de `DataTable`.
  - Enter "Abrir" (buscador enfocado y hay filas) → `onOpen(rows[selected])`.
  - F2 "Nuevo" → `onNew()`.
  - Escape → con texto "Borrar búsqueda"; vacío "Salir" → `onExit()`.
- Al cambiar `rows`, si `selectId` está en la página se selecciona esa fila.
- Sin filas, el mensaje de `DataTable`:
  - con `search` vacío: "Todavía no hay {many}. Pulsa F2 para dar de alta el primero.";
  - con `isCodeSearch(search)`: "No existe ningún {one} con el código {search}.";
  - si no: "No hay ningún {one} que coincida con «{search}».".

**`RecordScreen.svelte`** (`generics="V"`):
- Props: `form: RecordForm<V>`, `title: string`, `children`.
- `<h1>{title}</h1>`, `<Form bind:nav={form.nav} onLast={() => form.nav?.focusFirstField()}>`.
- Diálogo de borrar: abierto con `form.deleteDialog`; título `deleteDialog.title`, cuerpo `deleteDialog.body`; Enter "No borrar" → `cancelDelete`, F8 "Borrar" → `confirmDelete`.
- Diálogo de confirmación: abierto con `form.confirmMessage`; Enter "Volver a la ficha" → `cancelConfirm`, `NumpadAdd` "Guardar igualmente" → `confirmSave`.
- Un `useKeys`: `NumpadAdd` "Guardar" → `save`; F8 "Borrar" (`when: form.id !== null`) → `askDelete`; Escape "Salir" → `exit`.
- No incluye `UnsavedGuard` (usa `$app/navigation`): lo ponen las páginas.

**`RecordForm.save()`:**
1. Si ya está guardando, devuelve false.
2. `await nav.validateAll()`; si falla, false.
3. Si hay `confirmBeforeSave` y devuelve un mensaje: `confirmMessage = mensaje` y false.
4. Si no, `persist()`:
   - `id === null` → `create`; si no, `update`.
   - Error con `entity.duplicateCode` → error de `code` "Ya existe un {one} con ese código", foco en `code`, false.
   - `VALIDATION_ERROR` → `fieldErrorMessages(e.fieldErrors, fieldMessages)`, `nav.focusFirst(claves)`, false.
   - Otro error → `showError(e)`, false.
   - Éxito → `toast('{Label} {code} guardado')`.
     - Alta: `values = await blank()`, original = esa copia, errores vacíos, tras `tick()` foco en `code`.
     - Edición: original = `values`, `goToList(id)`.
5. `confirmSave()` limpia `confirmMessage` y hace `persist()` respetando el bloqueo.

**`RecordForm`, resto:**
- `askDelete()`: `deleteDialog = { title: '¿Borrar el {one} {code}?', body: await pendingForDelete(id) }`.
- `confirmDelete()`: `remove(id)`, `toast('{Label} {code} borrado')`, original = `values`, `goToList()`.
- `errorFor(name)`: devuelve el error mientras `values[name]` siga igual que cuando llegó.
- `isDirty`: comparación de `values` con el original (JSON).

- [ ] **Step 1: Escribir los tests**

```ts
// search.spec.ts
expect(isCodeSearch('7')).toBe(true); expect(isCodeSearch(' 12 ')).toBe(true);
expect(isCodeSearch('7a')).toBe(false); expect(isCodeSearch('')).toBe(false);
expect(listQuery('7', 'q')).toEqual({ codeFrom: 7, codeTo: 7 });
expect(listQuery('garcía', 'q')).toEqual({ q: 'garcía' });
expect(listQuery('tom', 'name')).toEqual({ name: 'tom' });
expect(listQuery('  ', 'q')).toEqual({});
// payload.spec.ts
expect(trimOrNull('  a ')).toBe('a'); expect(trimOrNull('   ')).toBeNull(); expect(trimOrNull(null)).toBeNull();
// list-memory.spec.ts
expect(recallList('clients')).toBeUndefined();
rememberList('clients', { href: '/clients?q=g', openedId: 3 }); expect(recallList('clients')).toEqual({ href: '/clients?q=g', openedId: 3 });

// ListScreen.svelte.spec.ts (ListHarness: KeyboardRoot + KeyBar + ListScreen con 3 filas y espías)
it('the search box has the cursor on entry')
it('searches after a pause')                        // teclear 'gar' → onSearch aún no; tras ~300 ms, una vez con 'gar'
it('Enter in the search box opens the selected row')
it('arrows move the selection from the search box') // ↓ y Enter → onOpen(rows[1])
it('Esc clears the search first, then exits')       // con texto: vacío, onSearch('') al momento, onExit no; Esc otra vez → onExit
it('F2 asks for a new record')
it('says why the list is empty')                    // los tres mensajes, con entity { one: 'cliente', many: 'clientes' }; con '7' sin filas, Enter → onOpen no
it('selects the remembered row')                    // selectId = id de la 3.ª → aria-selected en la 3.ª
it('names Esc after what it will do')               // vacío: 'Salir'; con texto: 'Borrar búsqueda'

// RecordScreen.svelte.spec.ts (RecordHarness: KeyboardRoot + KeyBar + RecordScreen con NumberField code (decimals 0) y TextField name required, opciones con espías; entity artículo)
it('saving checks every field first')               // name vacío, '+' → create no, 'Este dato es obligatorio', foco en name
it('a repeated code shows under the code field')    // create rechaza ApiError(409,'ARTICLE_DUPLICATE_CODE') → 'Ya existe un artículo con ese código', foco en code
it('a new record chains into another new one')      // create → {id:5, code:101}: toast 'Artículo 101 guardado'; blank() {code:102, name:''} se ve; foco en code
it('the chained blank form is clean')               // tras lo anterior, form.isDirty false
it('an edit goes back to the list')                 // id 5: update resuelve → goToList(5), toast
it('a second + while saving does nothing')          // create pendiente; '+' '+' → create una vez
it('API field errors show and clear when the field changes') // VALIDATION_ERROR {name:'x'}, fieldMessages {name:'Pon un nombre'} → visible; teclear en name → desaparece
it('delete asks with the pending numbers')          // body 'Está en 3 albaranes…' → F8: título '¿Borrar el artículo 101?' y cuerpo; Enter → remove no; F8 F8 → remove(5), toast 'Artículo 101 borrado', goToList()
it('confirmation before saving only when asked')    // confirmBeforeSave null → update directo; mensaje → diálogo con el mensaje, update no; '+' → update una vez
it('Enter on the last field goes back to the first')
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npm test`
Expected: FAIL, no existen los módulos.

- [ ] **Step 3: Implementar** según las interfaces y reglas de arriba.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test`
Expected: PASS.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 6: Artículos

**Files:**
- Create: `src/lib/catalog/pending.ts`, `src/lib/catalog/article-form.ts`
- Create: `src/routes/articles/+page.ts`, `src/routes/articles/new/+page.ts`, `src/routes/articles/new/+page.svelte`, `src/routes/articles/[id]/+page.ts`, `src/routes/articles/[id]/+page.svelte`, `src/lib/catalog/ArticleRecord.svelte`
- Modify: `src/routes/articles/+page.svelte` (deja de ser `SectionPlaceholder`)
- Modify: `front/playwright.config.ts` (`workers: 1`, `fullyParallel: false`)
- Test: `src/lib/catalog/pending.spec.ts`, `src/lib/catalog/article-form.spec.ts`, `front/e2e/articles.e2e.ts`

**Interfaces:**
- Consumes: `ListScreen`, `RecordForm`, `RecordScreen` (tarea 5), `UnsavedGuard` con `restoreFocus` (tarea 3), `request`, `api`, `loaded`, `withParams`, `toast`.
- Produces:
  - `count(n: number, one: string, many: string): string` (`'1 albarán'`, `'3 albaranes'`).
  - `articlePendingMessage(notes: number): string | null`.
  - `interface ArticleValues { code: number | null; name: string }`.
  - `articlePayload(v: ArticleValues): { code: number | null; name: string }`, con el nombre recortado.

Pantallas:
- **Lista** (`/articles`):
  - `+page.ts` lee `q` y `page` de la URL y llama a `GET /api/articles` con `listQuery(q, 'name')`, `page`, `size: 20` y `sort: 'code'`.
  - `ListScreen` con title "Artículos", `entity { one: 'artículo', many: 'artículos' }` y columnas Código y Nombre.
  - `onSearch` → `goto(withParams(url, { q: text || null, page: null }), { replaceState: true, keepFocus: true, noScroll: true })`.
  - `onOpen` → `rememberList('articles', { href: url.pathname + url.search, openedId: row.id })` y `goto(resolve('/articles/[id]', { id: String(row.id) }))`.
  - `onNew` → `/articles/new`; `onExit` → `/`.
  - `selectId` = `recallList('articles')?.openedId`.
- **Ficha** (`ArticleRecord.svelte`, usada por `new` y `[id]`):
  - `RecordForm` con `entity { one: 'artículo', Label: 'Artículo', duplicateCode: 'ARTICLE_DUPLICATE_CODE' }`.
  - `pendingForDelete` cuenta `GET /api/delivery-notes?articleId&invoiced=false&size=1` → `articlePendingMessage(page.totalElements)`.
  - `blank()` llama a `next-code`.
  - `goToList(id)` vuelve a `recallList('articles')?.href ?? '/articles'`, recordando `openedId` = id.
  - Campos: `NumberField` "Código" `name="code"` `decimals={0}` required; `TextField` "Nombre" `name="name"` required.
  - Título: "Artículo nuevo" o "Artículo {code}". La página añade `<UnsavedGuard isDirty={() => form.isDirty} save={() => form.save()} restoreFocus={() => form.restoreFocus()} />`.
  - `new/+page.ts` carga `next-code`; `[id]/+page.ts` usa `loaded(request(api.GET('/api/articles/{id}', …)))`.

- [ ] **Step 1: Escribir los tests**

```ts
// pending.spec.ts
expect(count(1, 'albarán', 'albaranes')).toBe('1 albarán');
expect(articlePendingMessage(0)).toBeNull();
expect(articlePendingMessage(1)).toBe('Está en 1 albarán sin facturar. Si lo borras no se podrán volver a guardar.');
expect(articlePendingMessage(3)).toBe('Está en 3 albaranes sin facturar. Si lo borras no se podrán volver a guardar.');
// article-form.spec.ts
expect(articlePayload({ code: 101, name: '  Tomate pera ' })).toEqual({ code: 101, name: 'Tomate pera' });
```

`e2e/articles.e2e.ts` (códigos 9101–9199; esperar a la barra antes de pulsar teclas, como en `navigation.e2e.ts`):

```ts
test('two new articles in a row')     // /articles → F2 → código sugerido; nombre 'Tomate e2e A' → NumpadAdd → toast 'Artículo N guardado' → ficha con N+1 → 'Tomate e2e B' → NumpadAdd → Esc → buscar 'e2e' muestra los dos
test('search by name and by code')    // código exacto + Enter → URL /articles/<id>
test('an edit returns to the list with the row selected')
test('a repeated code is reported under Código') // 'Ya existe un artículo con ese código'
test('delete asks and removes')       // F8 → '¿Borrar el artículo N?' → Enter (no borra) → F8 F8 → desaparece de la lista
test('unsaved changes')
// cambiar el nombre, Esc → 'Hay cambios sin guardar'; Enter → sigue y el campo Nombre tiene el foco;
// Esc, F8 → lista, el nombre no cambió; cambiar otra vez, Esc, NumpadAdd → guardado y lista
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npx vitest --run --project server src/lib/catalog`
Expected: FAIL, no existen los módulos.

- [ ] **Step 3: Implementar** según las interfaces y la descripción de las pantallas.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test && npm run test:e2e`
Expected: PASS. Las pruebas de punta a punta son las 6 de antes más las de artículos.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 7: Clientes

**Files:**
- Create: `src/lib/catalog/billing.ts`, `src/lib/catalog/client-form.ts`, `src/lib/catalog/ClientRecord.svelte`
- Create: `src/routes/clients/+page.ts`, `new/+page.ts`, `new/+page.svelte`, `[id]/+page.ts`, `[id]/+page.svelte`
- Modify: `src/routes/clients/+page.svelte`, `src/lib/catalog/pending.ts`
- Test: `src/lib/catalog/billing.spec.ts`, `client-form.spec.ts`, `pending.spec.ts`, `front/e2e/clients.e2e.ts`

**Interfaces:**
- Consumes: lo mismo que la tarea 6, más `count` (`pending.ts`).
- Produces:

```ts
// pending.ts (se añade)
export function clientPendingMessage(notes: number, drafts: number): string | null;
export function regimeChangeMessage(notes: number, drafts: number): string | null;
// billing.ts
export function missingBillingData(v: { nif: string; address: string; postalCode: string; city: string }): string[];
// orden y textos: 'NIF', 'dirección', 'código postal', 'población'
export function missingBillingMessage(missing: string[]): string | null;
// client-form.ts
export interface ClientValues {
	code: number | null; name: string; businessName: string; nif: string;
	address: string; postalCode: string; city: string; province: string;
	phone: string; phone2: string; email: string;
	vatType: 'WITH_VAT' | 'WITHOUT_VAT'; surchargeType: 'WITH_SURCHARGE' | 'WITHOUT_SURCHARGE';
	defaultDeliveryNoteFormat: 'PLAIN' | 'VALUED';
}
export const EMAIL_PATTERN: RegExp; // /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,6}$/, el de la API
export function clientPayload(v: ClientValues): ClientRequest; // nombres recortados; opcionales con trimOrNull
```

Pantallas:
- **Lista** (`/clients`): como la de artículos, con `listQuery(q, 'q')`, title "Clientes", `entity { one: 'cliente', many: 'clientes' }` y columnas Código, Nombre, Razón social, Población y NIF.
- **Ficha** (`ClientRecord.svelte`):
  - Campos en bloques con `<fieldset>` y `<legend>`, en este orden de DOM:
    - Identificación: Código (`NumberField` `decimals={0}`, required), Nombre comercial (required, maxlength 255), Razón social (required, 255), NIF (20).
    - Dirección: Dirección (500), Código postal (`DigitsField` `length={5}`), Población (100), Provincia (100).
    - Contacto: Teléfono (20), Teléfono 2 (20), Email (50, `validate`: `EMAIL_PATTERN` o "Revisa el email").
    - Facturación: `ChoiceField` IVA [WITH_VAT 'Con IVA', WITHOUT_VAT 'Sin IVA'], Recargo [WITHOUT_SURCHARGE 'Sin recargo', WITH_SURCHARGE 'Con recargo'], Formato de albarán [PLAIN 'Simple', VALUED 'Valorado'], los tres required.
  - En un alta: WITH_VAT, WITHOUT_SURCHARGE, PLAIN.
  - Recuadro `missingBillingMessage(missingBillingData(form.values))`, en `text-draft` sobre fondo claro, encima de los bloques, solo si no es null.
  - `entity { one: 'cliente', Label: 'Cliente', duplicateCode: 'CLIENT_DUPLICATE_CODE' }`.
  - `pendingForDelete` y `confirmBeforeSave` cuentan `GET /api/delivery-notes?clientId&invoiced=false&size=1` y `GET /api/invoices?clientId&status=DRAFT&size=1`.
  - `confirmBeforeSave` solo cuenta si cambió `vatType` o `surchargeType` respecto al original; si no, null.
  - Título: "Cliente nuevo" o "Cliente {code}".

- [ ] **Step 1: Escribir los tests**

```ts
// pending.spec.ts
expect(clientPendingMessage(0, 0)).toBeNull();
expect(clientPendingMessage(5, 1)).toBe('Tiene 5 albaranes sin facturar y 1 factura en borrador. Si lo borras no se podrán editar ni facturar.');
expect(clientPendingMessage(1, 0)).toBe('Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.');
expect(clientPendingMessage(0, 2)).toBe('Tiene 2 facturas en borrador. Si lo borras no se podrán editar ni facturar.');
expect(regimeChangeMessage(5, 1)).toBe('Cambiar el IVA o el recargo recalculará 5 albaranes sin facturar y 1 factura en borrador de este cliente.');
expect(regimeChangeMessage(0, 0)).toBeNull();
// billing.spec.ts
expect(missingBillingData({ nif: '', address: 'x', postalCode: '', city: 'x' })).toEqual(['NIF', 'código postal']);
expect(missingBillingMessage(['NIF'])).toBe('Para poder facturarle faltan: NIF.');
expect(missingBillingMessage(['NIF', 'código postal'])).toBe('Para poder facturarle faltan: NIF y código postal.');
expect(missingBillingMessage(['NIF', 'dirección', 'código postal', 'población'])).toBe('Para poder facturarle faltan: NIF, dirección, código postal y población.');
expect(missingBillingMessage([])).toBeNull();
// client-form.spec.ts
// clientPayload con email '' y postalCode '' → email null, postalCode null; name '  Bar  ' → 'Bar'
expect(EMAIL_PATTERN.test('pedidos@frutas.es')).toBe(true); expect(EMAIL_PATTERN.test('pepe@')).toBe(false);
```

`e2e/clients.e2e.ts` (códigos 9201–9299). La preparación crea datos con `request`:
- `POST /api/tax-config` `{ validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }` (un 409 se ignora);
- un artículo, un cliente y un albarán `{ date: <hoy>T08:00:00, clientId, lines: [{ articleId, traceabilityCode1: '01210426', quantity: 1, price: 1 }] }`.

```ts
test('a minimal client saves and says what is missing for invoices')
// F2, nombre y razón social → recuadro 'Para poder facturarle faltan: NIF, dirección, código postal y población.' → NumpadAdd → guardado;
// abrirlo, rellenar NIF, dirección, CP 46001 y población → el recuadro desaparece → NumpadAdd
test('search by business name and by NIF')         // razón social 'Frutas García SL' encontrada tecleando 'garcia' (sin tilde); NIF por un trozo
test('a regime change with a pending delivery note asks first')
// abrir el cliente con albarán, IVA '2', NumpadAdd → 'Cambiar el IVA o el recargo recalculará 1 albarán sin facturar de este cliente.';
// Enter → sigue en la ficha sin guardar; NumpadAdd y NumpadAdd → guardado (lista)
test('deleting a client with a pending delivery note warns with the number')
// F8 → 'Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.' → Enter → no se borra
test('a missing client shows the not-found page') // /clients/9999 → 'No se ha encontrado' → Esc → '/'
```

- [ ] **Step 2: Ejecutar y ver que falla**

Run: `npx vitest --run --project server src/lib/catalog`
Expected: FAIL en los módulos y funciones nuevos.

- [ ] **Step 3: Implementar** según las interfaces y la descripción de las pantallas.

- [ ] **Step 4: Ejecutar y ver que pasa**

Run: `npm run check && npm run lint && npm test && npm run test:e2e`
Expected: PASS, y nada escuchando en el 18080 al acabar.

- [ ] **Step 5: Commit (lo hace el controlador tras la revisión de la tarea)**

---

### Task 8: Documentación

**Files:**
- Modify: `PENDIENTES.md`, `front/README.md`, `src/lib/components/Form.svelte` (comentario)

- [ ] **Step 1: PENDIENTES §5**

Quitar lo hecho: todo el grupo "Endurecer campos y teclado", y del segundo grupo todo salvo:
- `CodeField` con un código inicial inexistente;
- `SpaFallbackResolver`;
- iconos de Lucide;
- la regla de `decimals`.

Añadir:
- El "+" justo después de Enter en un `CodeField` con la consulta en curso se ignora (decidir con el editor de albaranes).
- En artículos, el aviso de borrar no cuenta los albaranes que están en facturas en borrador.

- [ ] **Step 2: Convenciones del README**

En "Convenciones" de `front/README.md`, una línea: las listas usan `ListScreen` y las fichas `RecordForm` + `RecordScreen`, con la navegación y la API inyectadas; la página añade `UnsavedGuard`. Corregir el ejemplo de `validateAll` a `nav?.validateAll()`, también en el comentario de la prop en `Form.svelte`.

- [ ] **Step 3: Verificar**

Run: `npm run lint`
Expected: limpio.

- [ ] **Step 4: Commit (lo hace el controlador tras la revisión de la tarea)**
