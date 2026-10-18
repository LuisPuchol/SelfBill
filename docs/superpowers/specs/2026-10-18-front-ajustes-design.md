# Front: Ajustes (IVA y datos del titular)

Fecha: 2026-10-18, acordado con el usuario en el chat. Base:
[spec de la base del front](2026-10-13-front-base-design.md), [spec del catálogo](2026-10-17-front-catalogo-design.md),
[spec de los botones](2026-10-18-front-botones-design.md), [CONCEPTO.md](../../../CONCEPTO.md) §4 y §5, y
[PENDIENTES.md](../../../PENDIENTES.md) §2. Plan de ejecución:
[2026-10-18-front-ajustes.md](../plans/2026-10-18-front-ajustes.md).

---

## 1. Objetivo

Primer bloque de los que faltan (Ajustes → Albaranes → Facturas → Trazabilidad). Partiendo de una
base vacía, el usuario tiene que poder configurar lo que necesitan los demás:

- el IVA y el recargo vigentes desde una fecha: sin ellos no se valora un albarán (CONCEPTO §4);
- los datos fiscales del titular y su cuenta de correo: sin ellos no se emite ni se envía una
  factura (CONCEPTO §5).

Y ver en todo momento qué está configurado y qué falta.

---

## 2. Decisiones

| Tema | Decisión | Origen |
|---|---|---|
| Orden de los bloques | Ajustes → Albaranes → Facturas → Trazabilidad | Usuario |
| IVA | Lista de vigencias, con la vigente marcada y las futuras a la vista. Endpoint nuevo para listarlas | Usuario |
| Aviso de configuración | Recuadro en el inicio mientras falte algo | Usuario |
| Contraseña del correo | Opcional al crear el perfil | Usuario |
| Estructura | Ajustes es un menú con dos botones grandes; IVA es lista con ficha; titular, una ficha | Usuario (enfoque recomendado) |
| Contraseña en la respuesta | La API devuelve `hasMailPassword`, nunca la contraseña | PENDIENTES §2 |
| Contraseña indescifrable | El perfil carga igual; enviar un correo responde "no configurado" | PENDIENTES §2 |
| Porcentajes | Entre 0 y 100, en el front y en la API | Claude |
| Fecha propuesta | La primera vigencia propone el 1 de enero del año en curso; las siguientes, hoy | Claude |
| Logo y "probar el correo" | Fuera: el logo, con el bloque del PDF; probar el correo no tiene endpoint | Claude |
| Formularios | Una pieza común (`FormState` + `EditScreen`) para las fichas que no son de catálogo, sacada de `RecordForm` | Claude |

---

## 3. Estado actual

- `/settings` es un marcador de posición (`SectionPlaceholder`).
- `GET /api/tax-config` devuelve solo la vigencia en vigor hoy (404 `TAX_CONFIG_NOT_FOUND` si no hay).
  `POST` añade una vigencia desde una fecha (409 `TAX_CONFIG_ALREADY_EXISTS` si esa fecha ya existe).
  `PUT` corrige los tipos de la vigencia que empieza en una fecha. No hay forma de listarlas.
  Los porcentajes no admiten negativos, pero no tienen techo.
- `GET /api/user-profile` devuelve la contraseña del correo descifrada; con otra clave, da 500.
  `POST` exige la contraseña (`USER_PROFILE_MAIL_PASSWORD_REQUIRED`); `PUT` con `null` la
  conserva y con texto vacío la borra.
- `RecordForm` reúne valores, copia original, errores de la API por campo, bloqueo al guardar,
  altas en cadena y borrado, y presupone un `code`.

---

## 4. Diseño

### 4.1. API

- **`GET /api/tax-config/history`**: todas las vigencias, de la más nueva a la más antigua
  (`TaxConfigResponse[]`). Lista vacía si no hay.
- **Porcentajes**: `vatPercentage` y `surchargePercentage` entre 0 y 100, los dos incluidos.
- **Perfil**:
  - `UserProfileResponse` cambia `mailPassword` por `hasMailPassword: boolean`. Responder ya no
    descifra nada.
  - `POST` admite el perfil sin contraseña (sin ella, o en blanco: se guarda sin contraseña).
    Desaparece `USER_PROFILE_MAIL_PASSWORD_REQUIRED`.
  - Al enviar un correo, una contraseña que no se puede descifrar cuenta como no configurada:
    `MAIL_NOT_CONFIGURED`, no 500.
- `schema.d.ts` se regenera con una API temporal en otro puerto y una base y una clave temporales:
  nunca en el 8080.

### 4.2. Inicio

Mientras falte algo, un recuadro arriba del todo, en color de aviso (no rojo):

| Falta | Texto |
|---|---|
| IVA y titular | "Falta configurar el IVA y los datos del titular. Pulsa 6 para ir a Ajustes." |
| Solo el IVA | "Falta configurar el IVA. Pulsa 6 para ir a Ajustes." |
| Solo el titular | "Faltan los datos del titular. Pulsa 6 para ir a Ajustes." |
| Hay vigencias, pero ninguna hoy | En lugar de "el IVA": "No hay IVA vigente hoy" ("No hay IVA vigente hoy y faltan los datos del titular. Pulsa 6…") |

Con todo configurado no se ve nada. La carga del inicio pregunta a la API: si falla por
conexión, la base ya enseña "Sin conexión".

### 4.3. Ajustes (`/settings`)

- Dos botones grandes, como los del inicio: **1 IVA y recargo** y **2 Datos del titular**. Cada uno
  lleva una línea de estado:
  - IVA: "IVA 4 % · Recargo 0,5 %" (la vigente), "No hay IVA vigente hoy" o "Sin configurar".
  - Titular: el nombre fiscal, o "Sin configurar".
- Teclas: 1 y 2 abren; Esc vuelve al inicio.

### 4.4. IVA y recargo (`/settings/tax`)

- Tabla `Desde · IVA · Recargo · (estado)`, de la más nueva a la más antigua:
  - la vigente lleva "Vigente";
  - una futura lleva "A partir del 01/01/2027";
  - las anteriores, nada.
- La fila marcada al entrar es la vigente (o la primera si no hay vigente).
- Botones y teclas: `Nuevo · F2`, `Corregir · Enter` (gris sin filas) y `Volver · Esc` (a Ajustes).
- Lista vacía: "Todavía no hay ningún tipo. Pulsa F2 para añadir el primero."
- Porcentajes con coma y sin ceros de más: "4 %", "0,5 %", "21 %".

### 4.5. Ficha de un tipo

**Nuevo** (`/settings/tax/new`): Desde\*, IVA %\*, Recargo %\*.

- Fecha propuesta: si no hay ninguna vigencia, el 1 de enero del año en curso; si hay, hoy.
- IVA y recargo: números con hasta 2 decimales, entre 0 y 100 ("Tiene que estar entre 0 y 100").
- Fecha repetida (409): bajo Desde, "Ya hay un tipo que empieza ese día: corrígelo desde la lista."

**Corregir** (`/settings/tax/[validFrom]`): el título dice la fecha ("Tipo desde el 01/01/2026") y
la fecha no es un campo. Solo IVA % y Recargo %. Debajo, un aviso fijo: "Los albaranes y facturas
ya hechos conservan el tipo con el que se valoraron." Una fecha que no existe lleva a la 404.

**Ambas**: `Guardar · +`, `Volver · Esc` (a la lista, con el aviso de cambios sin guardar). Al
guardar: toast "Tipo desde el 01/01/2026 guardado" y vuelta a la lista con esa fila marcada.

No hay borrado: la API no lo tiene. Una vigencia con la fecha mal se corrige añadiendo la buena.

### 4.6. Datos del titular (`/settings/profile`)

Dos bloques; Enter los recorre en orden:

| Bloque | Campos |
|---|---|
| Datos fiscales | Nombre completo\*, Nombre fiscal\*, NIF\*, Dirección\*, Teléfono |
| Correo | Email\*, Contraseña de aplicación, Servidor, Puerto |

- Longitudes y formatos como la API: nombres 255, NIF 20, dirección 500, email 50 con el patrón
  de la API, teléfono 20, servidor 255, puerto entre 1 y 65535.
- **Contraseña**: campo de contraseña, vacío siempre al entrar.
  - Si hay una guardada, debajo: "Guardada. Déjalo vacío para conservarla."
  - Vacío al guardar: se envía `null` y se conserva. Escrito: se sustituye.
- **Servidor y puerto**: nota "Déjalos vacíos con Gmail, Outlook, Yahoo o iCloud."
- Sin perfil, guardar hace `POST`; con perfil, `PUT`.
- Al guardar: toast "Datos del titular guardados" y vuelta a Ajustes. Esc vuelve a Ajustes pasando
  por el aviso de cambios sin guardar.
- `Guardar · +` y `Volver · Esc`, como en la ficha de un tipo.

### 4.7. Piezas comunes

- **`FormState<V>`** (`records/`): valores y copia original, `isDirty`, errores de la API por campo
  (se borran al cambiar el campo), `restoreFocus`, y traducción de un `VALIDATION_ERROR` a errores
  bajo cada campo. `RecordForm` pasa a apoyarse en ella sin cambiar su comportamiento.
- **`EditScreen`** (`components/`): título y botones `Guardar · +` / `Volver · Esc` en
  `ScreenHeader`, el formulario y el cursor en el primer campo al entrar. La ficha de un tipo y la
  del titular la usan, cada una con su `UnsavedGuard`.
- **`NumberField`** gana `validate`, como `TextField`.
- **`Tile`** gana una línea de estado opcional.
- **`settings/status.ts`**: lee de la API qué está configurado (vigencia de hoy o no, si hay
  alguna, y el perfil). Lo usan el inicio y Ajustes.
- **`formatPercent`** (`format/number.ts`): "4 %", "0,5 %".

---

## 5. Tests

| Nivel | Qué |
|---|---|
| API | Historial vacío y ordenado; techo de 100 en los dos porcentajes; perfil creado sin contraseña y en blanco; `hasMailPassword` tras guardar, conservar y borrar; el perfil se lee con una contraseña cifrada con otra clave; el envío con esa contraseña da `MAIL_NOT_CONFIGURED`; OpenAPI con el endpoint nuevo |
| Unidad | `formatPercent`; `status.ts` (vigente, solo futura, ninguna; perfil sí o no) |
| Componente | `FormState` y `EditScreen` (guardar valida antes, "+" doble guarda una vez, los botones hacen lo de sus teclas); `RecordScreen` sin cambios; `NumberField` con `validate`; aviso del inicio en sus variantes; menú de Ajustes con sus estados; lista de IVA (vigente, futura, vacía, F2, Enter); ficha de un tipo (fecha propuesta, fecha repetida bajo el campo, porcentaje fuera de rango, corregir sin fecha); ficha del titular (alta y edición, contraseña vacía conservada y escrita enviada, nota de guardada) |
| e2e | Añadir y corregir una vigencia desde la lista; guardar el titular y ver su estado en Ajustes; el aviso del inicio desaparece al configurar ambos. Los tests leen primero lo que hay en la base compartida y no dependen del orden |

---

## 6. Fuera de alcance

- Logo del titular (con el bloque del PDF).
- Probar la cuenta de correo.
- Borrar una vigencia (no hay endpoint).
- El bug de la base que depende del directorio de arranque (PENDIENTES §2), que no es del front.

---

## 7. Riesgos

- **Sacar `FormState` de `RecordForm`** toca el catálogo. Lo cubren los tests de `RecordScreen`,
  que no deben cambiar, y los e2e del catálogo.
- **Cambiar la respuesta del perfil** rompe a quien leyera `mailPassword`. Hoy no hay ninguna pantalla
  que lo use; los tests de la API se adaptan.
- **La base compartida de los e2e**: otros tests ya crean una vigencia por la API. Los de este bloque
  no pueden dar por hecho que la base está vacía.
