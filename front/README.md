# SelfBill front

Interfaz de SelfBill: una SPA de SvelteKit (adapter estático) pensada para usarse con teclado y que se
empaqueta dentro del jar de `selfbill-api`, que la sirve. Está pensada para usarse en una ventana de Edge en modo app, que abrirá un lanzador que aún no existe.

Especificación de la base: [../docs/superpowers/specs/2026-10-13-front-base-design.md](../docs/superpowers/specs/2026-10-13-front-base-design.md)

## Comandos

- `npm run dev`: servidor de desarrollo. Necesita la API en marcha en el puerto 8080
  (`./mvnw spring-boot:run` en `selfbill-api/`); `/api` se redirige por proxy.
- `npm run api:types`: regenera `src/lib/api/schema.d.ts` desde la API (también necesita la API en el 8080).
- `npm test`: tests unitarios y de componentes.
- `npm run test:e2e`: construye el jar y lanza Playwright contra él en el puerto 18080 con una BD temporal.
- `npm run package`: build del front + jar; es el entregable.
- `npm run check` / `npm run lint`: tipos y estilo.

## Convenciones

- Fuera de `lib/`, los módulos TS se importan como `#lib/<ruta>.js` (sin extensión no pasa el
  type-check) y los componentes como `#lib/<ruta>.svelte`.
- Todo `href`/`goto` interno pasa por `resolve()` de `$app/paths` (ESLint lo exige).
- Cada pantalla declara sus teclas con `useKeys`: un `useKeys`/`keyLayer` por componente; las
  capas modales, en su propio componente.
- El "+" de guardar es `NumpadAdd` por código de tecla (el "+" del teclado principal escribe).
- Los textos de la API (en inglés) nunca se muestran: los textos en español viven en
  `src/lib/api/messages.ts`, y un `ErrorCode` nuevo no compila hasta tener su texto.
- Antes de guardar, la pantalla valida todo el formulario: `<Form bind:nav>` y
  `if (await nav?.validateAll()) save()`. Así salen todos los errores y el cursor va al primero.
- Las listas usan `ListScreen`. Las fichas con código usan `RecordForm` + `RecordScreen`, con la
  navegación y la API inyectadas; los demás formularios, `FormState` + `EditScreen`, y la página
  trae el guardado (`form.save(persist)`). Cada ficha (el componente del formulario) añade su
  `UnsavedGuard`.
- Cada acción principal tiene su tecla y su `ActionButton`, ambos construidos desde el mismo
  `KeyAction`; el `enabled` del botón ignora el foco. Los botones van en `ScreenHeader`.
- Los campos de una celda de tabla llevan `compact`: la etiqueta solo la lee el lector de pantalla y el error ocupa menos.
- `ListScreen.extraActions` añade una tecla y su botón a la lista; `deletion.canDelete` deja fuera de borrar las filas que no deben.
- `RecordScreen`: `readOnly` quita guardar y borrar, `canDelete` condiciona el borrado y `onLast` dice adónde va el cursor tras el último campo.
- `RecordForm`: `firstField` (si no es `code`), `mapFieldPath` (campo de pantalla de un error de la API) y `comparable` (qué cuenta como cambio sin guardar).
