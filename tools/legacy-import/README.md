# Importación del programa antiguo

Construye una base de SelfBill a partir de las copias de seguridad del programa anterior
(Visual FoxPro sobre SQL Server). Es de un solo uso, pero se puede repetir: si cambia el esquema o
aparecen copias más recientes, se vuelve a ejecutar. Necesita Python 3 y, para copias de SQL Server
2005 o posterior y para la comprobación, SQL Server LocalDB.

- **SQL Server 2000**: ningún SQL Server actual restaura sus copias, así que `sql2000.py` lee las
  páginas de datos directamente del fichero y saca el esquema de la propia copia.
- **SQL Server 2005 en adelante**: `sqlserver.py` restaura la copia en una base temporal de LocalDB,
  lee las tablas con SQL y la borra al terminar. Si el fichero tiene varias copias, usa la última.

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

3. Comprobar el resultado contra un SQL Server real (LocalDB). Carga las filas de la copia, crea la
   función y las vistas del programa a partir del código de la propia copia y compara cada línea,
   albarán y factura:

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
| `det` | `delivery_notes_articles` | No tiene clave: unas pocas líneas distintas comparten id y reciben uno nuevo |
| `fah` | `invoices` + `invoice_lines` | `ISSUED`, con copia de los datos fiscales |
| `fah.typ` 1 / 3 | serie `GENERAL` / `WITHOUT_VAT` | Conservan el número |
| `fah.typ` otro | `GENERAL`, en negativo | Tenían contador propio, que no existe en SelfBill |
| `iva` | `tax_config` | Tabla de tipos por fechas, desde 2023. Sin ella, 4 % y 0,5 % |
| `cfg` | `user_profile` | También como copia del titular en cada factura |
| `apt`, `prv`, `gst`, `fah.ret`, `fah.cob` | CSV | SelfBill no tiene dónde guardarlos |

## Reglas

- **Una base por titular.** Cada titular tiene su propia numeración de albaranes y facturas: juntarlos
  obligaría a cambiar números de documentos ya entregados.
- **Importes como se imprimieron.** Se reproduce la aritmética antigua: `real * money` da `real`, las sumas
  son `float`, y cada cifra se redondea desde la suma sin redondear. Las cifras impresas no siempre cuadran
  al céntimo entre sí, igual que en el papel. Recalcular con las reglas actuales cambiaría facturas entregadas.
- **IVA de cada época.** Hasta el 1-1-2023 la vista multiplicaba por los literales `.04` y `.005`; desde
  entonces lee la tabla `iva`, cuyo tipo es un `real` (el 4 % vale 0,0399999991). Cada factura se calcula
  con la fórmula vigente en su fecha, que es con la que se imprimió: reimpresa hoy con el programa, alguna
  antigua saldría con un céntimo distinto. La comprobación usa las dos vistas, cada una para su época.
- **Cliente de cada documento.** El programa antiguo no guardaba copia y reutilizaba códigos, así que al
  reimprimir salía el cliente de hoy. Aquí cada documento lleva el cliente de la primera copia tomada tras
  crearlo. Si un código cambió de manos entre dos copias, el silencio más largo (60 días o más) marca el cambio
  y esos documentos se listan como estimados. Los clientes anteriores de un código quedan en borrado lógico,
  que libera el código sin perder el documento (CONCEPTO §7).
- **Números con contador propio, en negativo.** Ponerlos tras el último de la serie general ocuparía los
  números que la serie viva va a usar a continuación.
- **NIF fantasma en la serie S.** Sus clientes nunca tuvieron NIF, y los vivos comparten el de `--ghost-nif`. Es decisión
  del usuario: emitir en la serie S no lo exige (CONCEPTO §5). Solo en su ficha: las facturas ya emitidas
  conservan la copia sin NIF con la que se imprimieron.
- **Fechas intactas.** Solo se infiere la que falta, porque la columna la exige. Las que parecen mal tecleadas
  se conservan y se listan en `anomalias.csv`: es lo que salió impreso.
- **Albaranes sin fecha y sin factura, apartados.** El programa antiguo nunca los pudo facturar, porque no
  entraban en ningún periodo. Con una fecha inferida, SelfBill los ofrecería para facturar hoy: se importan
  borrados y se listan en `albaranes-apartados.csv`. Se recuperan vaciando su `deleted_at` y el de sus líneas.
- **Lotes.** Se quitan las teclas sueltas (`.`, `+`, `ç`) sin perder ninguna cifra y se suben a los primeros
  huecos. Los que no tienen 8 cifras se conservan: es el histórico de trazabilidad. Sin lote, el primer hueco
  queda vacío, porque la columna no admite nulos.
