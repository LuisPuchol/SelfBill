# Importación del programa antiguo

Construye una base de SelfBill a partir de las copias de seguridad del programa anterior
(Visual FoxPro sobre SQL Server 2000). Es de un solo uso, pero se puede repetir: si cambia el
esquema o aparecen copias más recientes, se vuelve a ejecutar. Solo necesita Python 3.

Ningún SQL Server actual restaura copias de la versión 2000, así que `sql2000.py` lee las páginas
de datos directamente del fichero y saca el esquema de la propia copia.

**Las copias y las bases generadas tienen datos personales: no se suben al repositorio.**

## Uso

1. Crear una base vacía con el esquema vigente, con la propia API:

   ```powershell
   java -jar selfbill-api.jar --spring.main.web-application-type=none "--spring.datasource.url=jdbc:sqlite:plantilla.db"
   ```

2. Importar un titular. Se pasan todas sus copias con su fecha, de la más antigua a la más reciente.
   Los documentos salen de la última; las anteriores dicen quién era cada código en cada momento.

   ```powershell
   python import_legacy.py --template plantilla.db --out selfbill.db --annexes informe `
       --title "Titular, años" --logo logo.jpg --ghost-nif 12345678Z `
       --snapshot 2007-02-08=copia1 --snapshot 2011-06-28=copia2
   ```

   Nunca sobrescribe una base: si `--out` existe, se detiene. El reloj de la máquina importa: Hibernate
   guarda las fechas en milisegundos según su zona horaria.

3. Comprobar el resultado contra un SQL Server real (LocalDB). Carga las filas de la copia, crea las
   vistas del programa a partir del código de la propia copia y compara cada línea, albarán y factura:

   ```powershell
   python check_against_sql_server.py copia2 selfbill.db
   ```

Tests: `python -m unittest test_legacy_import`, desde esta carpeta.

## Correspondencia

| Antiguo | SelfBill | Notas |
|---|---|---|
| `cli` | `clients` | `nom` va a nombre y razón social. `fax` solo se usa si no hay segundo teléfono |
| `cli.typ` 3 | `WITHOUT_VAT` | "Base" en el programa antiguo; el resto lleva IVA |
| `cli.rec` | `WITH_SURCHARGE` | Solo con IVA: el programa antiguo no aplicaba recargo a los de tipo 3 |
| `cli.fra` = 1 | formato `VALUED` | Facturaba cada albarán al momento: es el cliente para el que existe el valorado |
| `cli.va`, `cli.vf` | se pierden | Eran el número de copias a imprimir, no un formato |
| `art` | `articles` | |
| `alb` | `delivery_notes` | `ano` es el número. Todos en `PLAIN`: es lo que imprimía |
| `det` | `delivery_notes_articles` | |
| `fah` | `invoices` + `invoice_lines` | `ISSUED`, con copia de los datos fiscales |
| `fah.typ` 1 / 3 | serie `GENERAL` / `WITHOUT_VAT` | Conservan el número |
| `fah.typ` otro | `GENERAL`, tras la última | Tenían contador propio, que no existe en SelfBill |
| `cfg` | `user_profile` | También como copia del titular en cada factura |
| `apt`, `prv`, `gst`, `fah.ret`, `fah.cob` | CSV | SelfBill no tiene dónde guardarlos |

## Reglas

- **Una base por titular.** Cada titular tiene su propia numeración de albaranes y facturas: juntarlos
  obligaría a cambiar números de documentos ya entregados.
- **Importes como se imprimieron.** Se reproduce la aritmética antigua: `real * money` da `real`, las sumas
  son `float`, y cada cifra se redondea desde la suma sin redondear. Las cifras impresas no siempre cuadran
  al céntimo entre sí, igual que en el papel. Recalcular con las reglas actuales cambiaría facturas entregadas.
- **Cliente de cada documento.** El programa antiguo no guardaba copia y reutilizaba códigos, así que al
  reimprimir salía el cliente de hoy. Aquí cada documento lleva el cliente de la primera copia tomada tras
  crearlo. Si un código cambió de manos entre dos copias, el silencio más largo (60 días o más) marca el cambio
  y esos documentos se listan como estimados. Los clientes anteriores de un código quedan en borrado lógico,
  que libera el código sin perder el documento (CONCEPTO §7).
- **NIF fantasma en la serie S.** Sus clientes nunca tuvieron NIF y emitir lo exige, así que los vivos comparten el de
  `--ghost-nif`. Solo en su ficha: las facturas ya emitidas conservan la copia sin NIF con la que se imprimieron.
- **Fechas intactas.** Solo se infiere la que falta, porque la columna la exige. Las que parecen mal tecleadas
  se conservan y se listan en `anomalias.csv`: es lo que salió impreso.
- **Lotes.** Se quitan las teclas sueltas (`.`, `+`, `ç`) sin perder ninguna cifra y se suben a los primeros
  huecos. Los que no tienen 8 cifras se conservan: es el histórico de trazabilidad. Sin lote, el primer hueco
  queda vacío, porque la columna no admite nulos.
