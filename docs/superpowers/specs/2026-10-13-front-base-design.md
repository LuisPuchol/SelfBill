# Front: la base

Fecha: 2026-10-13, acordado sección a sección con el usuario. Base:
[CONCEPTO.md](../../../CONCEPTO.md) y [PENDIENTES.md](../../../PENDIENTES.md) en su versión de
esta fecha. Plan de ejecución: [2026-10-13-front-base.md](../plans/2026-10-13-front-base.md).

---

## 1. Objetivo

Construir lo que comparten todas las pantallas del front, antes de hacer ninguna de ellas:
cómo se sirve y se abre la aplicación, el cliente de la API, el sistema de teclado, los
componentes, el estilo y el tratamiento de errores.

El front se hace por bloques, cada uno con su spec, su plan y su implementación (§8). Esta
es la primera pieza. Al terminarla, la aplicación arranca desde el jar de la API, muestra el
inicio con sus seis secciones, se navega con el pad numérico y con el ratón, y cada sección
es todavía una página vacía.

---

## 2. Usuarios y uso

| Tema | Decisión | Origen |
|---|---|---|
| Quién lo usa | Autónomos de 40–50 años. Tiene que ser muy intuitivo | Usuario |
| Cómo se introduce | Casi todo con el pad numérico | Usuario |
| Navegación | Inicio con botones grandes y una barra superior | Usuario |
| Teclas | Enter avanza, "+" del pad guarda, Esc sale, teclas F para lo poco frecuente | Usuario |
| Cómo se abre | Un icono arranca la API y abre Edge en modo aplicación | Usuario |
| Sistema operativo | Windows | Asumido, confirmado |
| Idioma | Solo español. Los textos en inglés de la API son de los PDF y correos | Asumido, confirmado |
| Pantalla | Monitor genérico de 1920×1080 | Usuario |
| Tema | Claro | Asumido, confirmado |
| Ayuda de teclas | Siempre visible, pequeña y abajo, sin estorbar: qué tecla del pad hace qué | Usuario |

Todo lo que se teclea en un albarán es numérico: código de cliente y de artículo (enteros),
cantidad y precio (decimales) y lote (8 dígitos). Por eso el pad numérico basta para el
trabajo diario. El pad no tiene Tab ni Esc: Enter hace de Tab.

---

## 3. Estado actual

El front se genera desde cero. Lo que existe es la API:

| Dónde | Qué hay |
|---|---|
| Recursos | Seis recursos bajo `/api`, OpenAPI con springdoc en `/v3/api-docs` y todas las respuestas con tipo concreto |
| Errores | `ProblemDetail` con `errorCode` y, en validación, `errors: {campo: mensaje}`. Los mensajes están en inglés, escritos en el código |
| Servido | La API no sirve ficheros estáticos, no tiene CORS y escucha en todas las interfaces |

---

## 4. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Cómo se sirve | SPA estática dentro del jar de la API: un proceso, un puerto y sin CORS | Usuario (§5.1) |
| Proyecto | Generar `front/` desde cero con `sv create` en SvelteKit 3. Si alguna dependencia falla, SvelteKit 2.70 | Diseño (§5.1) |
| Router | Por ruta, no por hash, con un fallback en Spring | Diseño (§5.1) |
| Tipos de la API | Generados con `openapi-typescript` y guardados en git | Diseño (§5.2) |
| Cliente | `openapi-fetch`, envuelto | Diseño (§5.2) |
| `ErrorCode` | Se expone en el OpenAPI para que la traducción sea exhaustiva | Diseño (§5.2) |
| Carga de datos | `load` de cada página e `invalidate`. Sin store global ni caché | Diseño (§5.2) |
| Teclado | Acciones declaradas por pantalla, en capas, y una barra de teclas generada de ellas | Diseño (§5.3) |
| Barra de teclas | Franja fina y discreta abajo; las acciones principales tienen además su botón en la pantalla | Usuario (§5.3) |
| Guardar | `NumpadAdd`, por código de tecla. El "+" del teclado principal escribe | Diseño (§5.3) |
| Salir sin guardar | Diálogo con F8. Un segundo Esc no descarta nada | Diseño (§5.3) |
| Componentes | Propios, con Tailwind y `<dialog>` nativo. Solo Lucide como dependencia de interfaz | Diseño (§5.4) |
| Textos de error | Siempre del front, en español. Nunca se muestra un texto de la API | Diseño (§5.5) |

---

## 5. Diseño

### 5.1. Estructura y servido

**Proyecto.** Se genera `front/` desde cero con `sv create`: SvelteKit 3, Svelte 5, Vite 8,
TypeScript, Tailwind 4, Vitest y Playwright. `adapter-static` con `fallback: 'index.html'`, y
`ssr = false` en `src/routes/+layout.ts`. SvelteKit 3.0.0 salió el 2026-10-01 y la 3.0.1 el
2026-10-06; pide Vite 8 y TypeScript 6, y mantiene `adapter-static` (4.0).

**Router por ruta.** El router por hash obliga a que todos los enlaces empiecen por `#/`, y la
documentación de SvelteKit lo reserva para cuando no se controla el servidor. Aquí el servidor
es Spring.

```
front/src/
  routes/
    +layout.svelte        barra superior · contenido · barra de teclas
    +layout.ts            ssr = false
    +page.svelte          inicio
    delivery-notes/
    invoices/
    clients/
    articles/
    traceability/
    settings/
  lib/
    api/                  tipos generados, cliente, errores, textos de error
    keyboard/             capas de teclas, navegación con Enter, barra de teclas
    components/           §5.4
    format/               dinero, fechas y números en es-ES
```

Rutas y código en inglés, textos en pantalla en español. Cada sección es una carpeta y solo
comparte lo de `lib/`.

**Inicio.** Seis botones grandes, numerados y con icono:

| Tecla | Sección | Ruta |
|---|---|---|
| 1 | Albaranes | `/delivery-notes` |
| 2 | Facturas | `/invoices` |
| 3 | Clientes | `/clients` |
| 4 | Artículos | `/articles` |
| 5 | Trazabilidad | `/traceability` |
| 6 | Ajustes | `/settings` |

En el inicio no hay campos de texto, así que un número abre su sección sin conflicto. La barra
superior lleva las mismas seis secciones y se usa con el ratón. En esta pieza, cada sección es
una página con su título y Esc para volver.

**Desarrollo.** `npm run dev` en el puerto 5173, con proxy de `/api` a
`http://127.0.0.1:8080`.

**Producción.**

- `mvn package` mete el build del front en `static/` dentro del jar. `mvn test` no depende de
  Node ni del front. El mecanismo concreto se decide en el plan.
- Spring sirve `index.html` para cualquier petición GET que no empiece por `/api`,
  `/v3/api-docs` ni `/swagger-ui` y que no corresponda a un fichero del build. Una ruta
  desconocida bajo `/api` sigue devolviendo su error JSON.
- `server.address=127.0.0.1`. Cierra la parte de red del bug "Expuesto a la red, con la
  contraseña en claro" de PENDIENTES. La contraseña SMTP en la respuesta del perfil queda
  para el bloque de ajustes.

El lanzador (icono → `java -jar` → `msedge --app=http://127.0.0.1:8080`) no forma parte de
esta pieza (§7).

### 5.2. Cliente de la API

**Tipos.** `npm run api:types` ejecuta `openapi-typescript` contra
`http://127.0.0.1:8080/v3/api-docs` y escribe `src/lib/api/schema.d.ts`. El fichero va a git:
el front compila sin la API arrancada y los cambios de la API se ven en el diff. Tras cambiar
la API se regenera, y `npm run check` señala lo que se ha roto.

**Cliente.** `openapi-fetch` tipa rutas, parámetros, cuerpos y respuestas desde ese esquema:

```ts
const { data } = await api.GET('/api/clients/{id}', { params: { path: { id } } });
```

`lib/api/client.ts` lo envuelve con URL base `''`, la misma que la página. Vale igual con el
proxy de desarrollo y dentro del jar.

**Errores.** Toda respuesta que no sea 2xx se convierte en un `ApiError`:

```ts
class ApiError {
  status: number;
  code: ErrorCode | 'NETWORK' | 'UNKNOWN';
  detail: string;                        // texto de la API, en inglés: solo para diagnóstico
  fieldErrors: Record<string, string>;   // `errors` de VALIDATION_ERROR
}
```

Un fallo de red (la API no responde) es `NETWORK`. Un `errorCode` que el front no conoce, o una
respuesta sin él, es `UNKNOWN`.

**`ErrorCode` en el OpenAPI.** La API documenta la forma de sus errores (`errorCode` como enum
`ErrorCode` y `errors` como mapa) en las respuestas de error. Así `openapi-typescript` genera
el enum, y `lib/api/messages.ts` es un `Record<ErrorCode, …>`: un código nuevo en Java no
compila en el front hasta que tenga su texto en español.

**Carga de datos.** Cada página carga en su `+page.ts` (`load`) y, tras guardar, recarga con
`invalidate`. Los listados usan la paginación de la API (`{content, page: {number, size,
totalElements, totalPages}}`) y llevan página y filtros en la URL, para que F5 no pierda la
posición.

**Dinero.** Los totales oficiales son siempre los de la API. Un total provisional mientras se
teclea, como el del editor de albaranes, se calcula en céntimos enteros con el redondeo de
CONCEPTO §4: total de línea redondeado a céntimos, mitad hacia arriba, y luego suma. La función
vive en `lib/format/` y se prueba con tests unitarios.

### 5.3. Sistema de teclado

**Reglas fijas.**

| Tecla | Efecto |
|---|---|
| Enter | Siguiente campo |
| Shift+Enter | Campo anterior. El Tab normal también funciona |
| `NumpadAdd` | Guardar o terminar, si la pantalla tiene esa acción. El "+" del teclado principal escribe |
| `NumpadDecimal` | En un campo numérico, siempre la coma decimal, escriba Windows "." o "," |
| Esc | Sube un nivel: diálogo → formulario → listado → inicio |
| 1–6 | Solo en el inicio: abre la sección |

**Acciones por pantalla.** Cada pantalla declara sus acciones una vez:

```ts
useKeys([
  { key: 'NumpadAdd', label: 'Guardar',         run: save },
  { key: 'Escape',    label: 'Salir',           run: exit },
  { key: 'F3',        label: 'Buscar artículo', run: openSearch },
]);
```

- Un único `keydown` en `window`, en el layout raíz, despacha la tecla a la capa de arriba.
- Las capas se apilan: un diálogo abierto pone las suyas encima y desactiva las de debajo
  hasta cerrarse.
- La barra de teclas es una franja fina fija abajo, con letra pequeña, que no compite con el
  contenido. Muestra qué tecla hace qué en la pantalla actual:
  `Enter Siguiente · + Guardar · Esc Salir`. Se dibuja a partir de las acciones de la capa de
  arriba, así que siempre está al día.
- La barra es una chuleta, no la forma de usar el ratón: las acciones principales tienen
  además su botón grande en la propia pantalla.

**Navegación con Enter.** Un formulario recorre sus campos en el orden del DOM. Un campo puede
validar al salir, también de forma asíncrona: el código de artículo consulta
`GET /api/articles/by-code/{code}`. Si existe, pinta el nombre y avanza. Si no, avisa y el
cursor no se mueve. Un formulario puede decidir qué hace Enter en su último campo; el editor de
albaranes abrirá una línea nueva.

**Cambios sin guardar.** Cualquier salida de un formulario modificado pasa por
`beforeNavigate`: Esc, la barra superior o Alt+←. Aparece un diálogo:

| Tecla | Opción |
|---|---|
| Enter o Esc | Seguir editando. Es la opción por defecto |
| `NumpadAdd` | Guardar y salir |
| F8 | Salir sin guardar |

Cerrar la ventana o pulsar F5 con cambios dispara el aviso nativo (`beforeunload`).

**Teclas del navegador.** Solo se usan F2, F3, F4, F8 y F9, y se anula su acción en Edge. No se
usan F5, F11 ni F12. Antes de dar la pieza por buena se comprueba a mano en Edge en modo
aplicación.

### 5.4. Componentes y estilo

**Sin librería de componentes.** shadcn-svelte y bits-ui son compatibles, pero su gestión del
teclado y del foco choca con §5.3, y su estilo por defecto (letra de 14 px, compacto, grises
suaves) es el contrario del buscado. Se usan componentes propios con Tailwind y el `<dialog>`
nativo. La única dependencia de interfaz es `@lucide/svelte` para los iconos.

| Componente | Comportamiento |
|---|---|
| `TextField` | Texto: nombre, dirección, email |
| `NumberField` | Decimal con coma, alineado a la derecha, decimales fijos al salir (10 → 10,00) |
| `CodeField` | Código entero que consulta al pulsar Enter y muestra el nombre al lado |
| `DigitsField` | Longitud fija de dígitos y conserva el cero inicial: el lote de 8 |
| `DateField` | `121026` → `12/10/2026` con el pad. Por defecto, hoy |
| `ChoiceField` | Opciones numeradas, sin desplegable: "1 Simple · 2 Valorado" |
| `DataTable` | Fila seleccionada con ↑↓ o clic, Enter para abrirla, y paginación |
| `Dialog` | `<dialog>` nativo con su capa de teclas |
| `KeyBar` | Franja fina abajo, letra pequeña: tecla y función en la pantalla actual |
| `TopBar`, `Tile` | Barra superior y botones del inicio |
| `Toast` | Aviso breve que desaparece solo |

Los campos llevan la etiqueta siempre visible encima, nunca solo un placeholder.

**Estilo.**

- Letra base de 18 px, campos de al menos 48 px de alto y botones grandes. La barra de
  teclas es la excepción: pequeña y discreta.
- El campo activo lleva un borde grueso de color y el fondo resaltado.
- Contraste alto y tema claro. El rojo solo para errores y acciones destructivas.
- Los estados (borrador, emitida, pendiente) tienen color y siempre texto.
- Ningún botón solo con icono.
- Fuente del sistema (Segoe UI en Windows), sin descargas. Importes con `tabular-nums`.
- Formatos de España: `1.234,56 €` y `12/10/2026`, todos desde `lib/format/`.
- Textos llanos: "Guardar", "Emitir factura", "Ya existe un cliente con ese código".
- Colores, tamaños y espaciados como variables en el bloque `@theme` de `app.css`. Cambiar el
  aspecto es tocar ese bloque.

### 5.5. Errores y avisos

Los textos de error son siempre del front, en español. La API devuelve sus mensajes en inglés,
tanto el `detail` como los de validación (`"NIF cannot be blank"`), y no se muestran nunca.

| Situación | Cómo se ve |
|---|---|
| Dato mal puesto | Mensaje rojo bajo el campo y el cursor en el primero que falla. El front valida lo evidente antes de enviar, con su propio texto. Si la API marca un campo en `errors`, se pinta en el mismo sitio con el texto del front para ese campo o, si no lo tiene, "Revisa este dato". Las rutas de línea se resuelven: `lines[2].quantity` → línea 3, cantidad |
| Regla de negocio (`errorCode` conocido) | Diálogo con el texto de `messages.ts`: qué ha pasado y qué hacer. Enter lo cierra y el cursor vuelve donde estaba |
| La API no responde (`NETWORK`) | Pantalla completa: "No se puede conectar con SelfBill". Al arrancar reintenta sola unos segundos ("Arrancando…"). Después, Enter reintenta |
| Error inesperado (500, `UNKNOWN`, `INTERNAL_ERROR`) | Diálogo "Ha ocurrido un error inesperado. La operación no se ha completado.", con estado, `errorCode` y `detail` plegados |
| Error al cargar una página (un registro borrado da 404) | `+error.svelte` con el mensaje en llano y Esc para volver |
| Éxito | `Toast` breve: "Albarán 121 guardado" |

### 5.6. Tests

| Nivel | Herramienta | Qué cubre en esta pieza |
|---|---|---|
| Unitario | Vitest (node) | Formatos es-ES, total en céntimos, interpretación de `10,5` / `10.5` / `121026`, `ProblemDetail` → `ApiError`, pila de capas de teclas |
| Componente | Vitest en modo navegador (Chromium) | Cada campo de §5.4, navegación con Enter, `CodeField` con la API simulada, `KeyBar` muestra las acciones de la capa de arriba y cambia al abrir un diálogo, diálogo de cambios sin guardar |
| Punta a punta | Playwright contra la API real con una SQLite temporal | Inicio → sección con su número, Esc de vuelta al inicio, barra superior |

- La API arranca para los tests con `--spring.datasource.url` apuntando a un fichero temporal;
  Flyway crea el esquema.
- El diálogo de cambios sin guardar se prueba de punta a punta en el primer bloque con
  formularios (catálogo): en esta pieza no hay formularios reales.
- Los tests se escriben antes que el código.
- `npm run check` y `npm run lint` acompañan a los tests. No se añade CI.

---

## 6. Cambios en la API

| Cambio | Motivo |
|---|---|
| Servir el build del front desde `static/` del jar | §5.1 |
| Fallback a `index.html` para rutas que no son `/api`, OpenAPI, Swagger ni ficheros | §5.1 |
| `server.address=127.0.0.1` | §5.1, PENDIENTES "Expuesto a la red" |
| Documentar la forma del error y el enum `ErrorCode` en el OpenAPI | §5.2 |

---

## 7. Fuera de alcance

- Las pantallas de cada sección: cada bloque de §8 tiene su spec.
- El lanzador y el instalador para Windows.
- Importar datos del programa antiguo.
- Las plantillas de PDF del backend (PENDIENTES, "Generación real de PDF").
- Interfaz en otros idiomas, tema oscuro y CI.

---

## 8. Hoja de ruta del front

Cada bloque es un ciclo propio de spec, plan e implementación:

1. **La base** (este documento).
2. **Catálogo**: artículos y clientes. CRUD sencillo, fija los patrones de listado y
   formulario.
3. **Albaranes**: el uso diario y el editor de líneas.
4. **Facturación**: agrupar los pendientes del mes, borrador → emitida, PDF y correo.
5. **Ajustes**: perfil del titular e IVA con vigencias.
6. **Trazabilidad**: búsqueda por artículo, lote y fechas (CONCEPTO §10).

---

## 9. Riesgos

| Riesgo | Qué se hace |
|---|---|
| SvelteKit 3 tiene once días y alguna dependencia puede no soportarlo | Se comprueba al generar el proyecto. Si algo falla, SvelteKit 2.70 |
| Edge en modo aplicación puede no dejar anular alguna tecla F | Prueba manual al cerrar la pieza. Si una falla, se cambia por otra libre |
| `NumpadDecimal` da "." o "," según la configuración de Windows | `NumberField` acepta las dos y lo cubre un test de componente |
| Edge abre antes de que la API esté lista | Reintento al arrancar (§5.5) |
