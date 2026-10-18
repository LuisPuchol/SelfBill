# Front: botones de las acciones principales

Fecha: 2026-10-18, acordado con el usuario en el chat. Base:
[spec de la base del front](2026-10-13-front-base-design.md) (§5.3: "las acciones principales tienen
además su botón grande en la propia pantalla"), [spec del catálogo](2026-10-17-front-catalogo-design.md)
y [PENDIENTES.md](../../../PENDIENTES.md) §5. Plan de ejecución:
[2026-10-18-front-botones.md](../plans/2026-10-18-front-botones.md).

---

## 1. Objetivo

Hoy la lista y la ficha solo se manejan con teclas: la barra de abajo es una chuleta, no se puede
pulsar. Quien usa el ratón no puede crear, borrar ni cambiar de página sin saberse las teclas.

Con este bloque, alguien que solo use el ratón puede crear, abrir, guardar, borrar, cambiar de página
y volver sin tocar el teclado, salvo para escribir. Quien usa el teclado sigue exactamente igual.

Además, se puede borrar desde la lista, sin entrar en la ficha.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Borrar desde la lista | Botón "Borrar" y F8 en la lista, sobre la fila marcada, con la misma pregunta que en la ficha | Usuario |
| Juego de botones | Lista: Nuevo, Abrir, Borrar y las dos páginas. Ficha: Guardar, Borrar, Volver. Al inicio se va con la barra de arriba | Usuario |
| Posición | En la línea del título, a la derecha, en todas las pantallas. Las páginas, debajo de la tabla | Usuario |
| Una definición | Tecla y botón llaman a la misma función. El botón se activa cuando la acción tiene sentido, sin mirar el foco | Claude |
| Acción no disponible | El botón sale gris, no desaparece, para que nada cambie de sitio | Claude |
| Foco | Los botones no se quedan con el cursor ni se alcanzan con Tab | Claude |
| Texto del botón | El de la acción, el mismo que en la chuleta. En la ficha, Esc pasa a llamarse "Volver" en los dos sitios | Claude |
| Sin API en desarrollo | El proxy de Vite corta la conexión en vez de responder 502: el front muestra "Sin conexión" | Claude, tras el fallo visto por el usuario |

---

## 3. Estado actual

- `ListScreen` declara sus teclas con `useKeys`. Enter, ↑/↓ y AvPág/RePág solo valen desde el
  buscador (`inSearch()`); con el foco fuera, los atiende `DataTable`.
- `DataTable` pinta "Página 1 de 3" y tiene sus propias teclas de página.
- `RecordScreen` declara "+", F8 y Esc. La pregunta de borrar vive en `RecordForm` (`askDelete`,
  `confirmDelete`, `cancelDelete`, `deleteDialog`) y su diálogo en `RecordScreen`.
- Cada ficha (`ArticleRecord`, `ClientRecord`) define `remove` y `pendingForDelete` dentro de sus
  opciones de `RecordForm`.
- Los diálogos ya tienen botones que se pueden pulsar. Las filas de la tabla se marcan con un clic y
  se abren con doble clic.
- En desarrollo, con la API parada, el proxy de Vite responde 502. `request()` lo trata como un error
  de la API y no como falta de conexión, y la pantalla muestra "No se ha podido cargar esta pantalla".

---

## 4. Diseño

### 4.1. `ActionButton`

Un botón grande que representa una acción del teclado.

- Recibe la acción (`KeyAction`: tecla, texto y `run`) y un `enabled` aparte. El clic llama al mismo
  `run` que la tecla.
- Pinta el texto de la acción y, en una etiqueta pequeña, la tecla con el mismo nombre que la
  chuleta: "+", "Esc", "AvPág", "F8".
- Gris y sin efecto cuando `enabled()` es falso.
- `tabindex="-1"` y sin robar el foco al pulsarlo: el cursor sigue en el buscador o en el campo donde
  estaba, y Enter sigue navegando como antes.
- Variantes: normal, destacada (Guardar) y peligro (Borrar, con el texto en rojo).
- Al menos 48 px de alto y letra de la base. Nunca solo icono.

Con un diálogo abierto, la pantalla de detrás está inerte (`showModal`): sus botones no responden.

### 4.2. Lista

**Botones**, en la línea del título y en este orden:

| Botón | Tecla | Activo cuando |
|---|---|---|
| Nuevo | F2 | Siempre |
| Abrir | Enter | Hay filas o hay una búsqueda en camino (la misma regla que Enter, sin mirar el foco) |
| Borrar | F8 | Hay filas |

**Páginas.** La fila de páginas pasa de `DataTable` a `ListScreen`: texto y teclas. Con más de una
página, debajo de la tabla:
`[ Página anterior · RePág ]  Página 1 de 3  [ Página siguiente · AvPág ]`, cada botón gris en su
extremo. Las teclas de página ya no dependen del foco: `ListScreen` las atiende siempre.

**Teclas.** Se añade F8 "Borrar" a las de la lista del catálogo (§4.1 de su spec). Funciona con el
foco en el buscador o en la tabla.

**Borrar desde la lista.**

1. Se toma la fila marcada.
2. Misma pregunta que en la ficha (§4.4): "¿Borrar el artículo 7?", el nombre y el aviso de
   pendientes. Enter o Esc "No borrar", F8 "Borrar".
3. Tras borrar:
   - toast "Artículo 7 borrado";
   - la página recarga sus datos con la misma URL: misma búsqueda y misma página;
   - la marca se queda en la misma posición, o en la última fila si era la última;
   - si la página queda vacía y no es la primera, se va a la anterior;
   - si el registro era el "último abierto" que recuerda la lista, se olvida.
4. Si el borrado falla, va a `showError` y la lista se recarga igualmente (puede que ya no exista).
5. Al cerrarse el diálogo, el cursor vuelve donde estaba.

### 4.3. Ficha

**Botones**, en la línea del título y en este orden:

| Botón | Tecla | Activo cuando |
|---|---|---|
| Guardar (destacado) | "+" | Siempre |
| Borrar (peligro) | F8 | En edición, no en un alta |
| Volver | Esc | Siempre |

El clic hace lo mismo que la tecla: Guardar valida y deja el cursor en el primer error; Volver pasa
por el aviso de cambios sin guardar.

### 4.4. Borrado común

- **`DeleteFlow`** (`records/`): cuenta los pendientes, abre la pregunta, borra, muestra el toast y
  avisa a quien lo usa (`onDeleted`). Si falla, cierra la pregunta, va a `showError` y avisa
  (`onFailed`). Ignora una segunda petición mientras trabaja.
- **`DeleteDialog`** (`components/`): el diálogo de la pregunta con sus teclas. Lo usan
  `RecordScreen` y `ListScreen`.
- **`RecordForm`** usa un `DeleteFlow`. Su comportamiento no cambia: al borrar, se marca limpia y
  vuelve a la lista.
- **Lo que cada entidad sabe de sí misma para borrar** (entidad, `remove`, `pendingForDelete`) pasa a
  un módulo por entidad en `catalog/`, compartido por su ficha y su lista.

### 4.5. Proxy de desarrollo

`vite.config.ts` configura el proxy de `/api` para que un fallo de conexión con la API destruya la
respuesta en lugar de contestar 502. El `fetch` del navegador falla como en producción, `request()`
lo clasifica como `NETWORK` y sale la pantalla "Sin conexión". Solo afecta a `npm run dev`.

---

## 5. Tests

| Nivel | Qué |
|---|---|
| Componente | `ActionButton`: texto y tecla; gris cuando no está activo y sin ejecutar; el clic ejecuta; el foco se queda en un campo enfocado; no se alcanza con Tab |
| Componente | `ListScreen`: Nuevo y Abrir hacen lo de F2 y Enter; Borrar gris sin filas; F8 y el botón preguntan con el nombre y los pendientes; F8 confirma y se llama a `remove` y a `onDeleted`; Enter y Esc cancelan; la marca tras recargar; página vacía va a la anterior; botones de página grises en los extremos y activos en medio; AvPág con el foco fuera del buscador |
| Componente | `RecordScreen`: Guardar, Borrar y Volver hacen lo de sus teclas; Borrar gris en un alta |
| Componente | `DeleteFlow`: pregunta con pendientes y sin ellos; error al contar; error al borrar avisa con `onFailed`; doble petición ignorada |
| e2e | Artículos solo con el ratón: Nuevo, escribir, Guardar, Volver, clic en la fila, Borrar, Borrar en el diálogo: la fila desaparece y sale el toast |
| e2e | Clientes: F8 en la lista sobre un cliente con un albarán pendiente: el aviso dice el número; tras borrar, la fila desaparece |
| e2e | Con más de 20 artículos: "Página siguiente" lleva a la página 2 y "Página anterior" vuelve |
| Manual | `npm run dev` con la API parada: "Sin conexión" en vez de la pantalla de error |

Los tests existentes de borrado en la ficha deben seguir pasando sin cambiar lo que comprueban.

---

## 6. Fuera de alcance

- Un botón "Salir" en la lista: al inicio se va con la barra de arriba.
- Borrar varios a la vez.
- Lo que queda en PENDIENTES §5 (una sola marca de "trabajando" para guardar y borrar, etc.).

---

## 7. Riesgos

- **Botones que roban el foco.** Romperían Enter en la tabla y en los formularios. Se evita con
  `tabindex="-1"` y anulando el `mousedown`, y lo comprueban los tests de `ActionButton`.
- **Mover las páginas de `DataTable` a `ListScreen`.** Toca las teclas de página, que hoy solo
  cubren tests de componente (no hay e2e con más de una página). Los tests de página de
  `DataTable.svelte.spec.ts` pasan a `ListScreen.svelte.spec.ts`, y un e2e nuevo pasa de página con
  el botón.
- **El arreglo del proxy depende del orden de los manejadores de error de Vite** (el de `configure`
  va antes que el suyo). Si una versión nueva de Vite lo cambia, vuelve el 502. Solo afecta al
  desarrollo, y la comprobación manual lo detecta.
