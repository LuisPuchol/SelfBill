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
