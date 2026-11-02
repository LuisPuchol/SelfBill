#!/usr/bin/env python3
"""Checks an imported database against the views of the old program, run by a real SQL Server.

Loads the rows of a backup into a temporary LocalDB database, creates the program's own views from the
backup's source, and compares what they compute with what the import stored. Independent of the import:
it shares the backup reader, not the arithmetic.
"""
import argparse
import csv
import os
import re
import sqlite3
import subprocess
import sys
import tempfile
from collections import Counter, defaultdict
from decimal import Decimal
from pathlib import Path

from legacy_rules import VAT_TABLE_SINCE, printed
from sql2000 import open_backup

SERVER = r'(localdb)\MSSQLLocalDB'
SEPARATOR = '\t'
TABLES = {
    'art': 'ind int, nom char(35)',
    'cli': 'ind int, nom char(30), rec tinyint',
    'alb': 'ind int, ano int, cli int, fec datetime, fno int',
    'det': 'ind int, alb int, art int, tr1 char(15), tr2 char(15), tr3 char(15), ctd real, prv money',
    'fah': 'ind int, num int, fec datetime, cli int, typ tinyint, cob datetime, ret int',
    'iva': 'ind int, tpo int, ti real, dsd datetime, hst datetime, re real',
}
FUNCTIONS = ('ivatp',)
VIEWS = ('detart', 'albi', 'fahi')
# Until 1-1-2023 fahi multiplied by these literals; the current view keeps that version in a comment
LITERAL_RATES = {'dbo.ivatp(f.fec,0)': '.04', 'dbo.ivatp(f.fec,1)': '.005'}


def run_sql(sql, database='master', output=None):
    command = ['sqlcmd', '-S', SERVER, '-d', database, '-b', '-W', '-h', '-1', '-s', SEPARATOR, '-Q', 'SET NOCOUNT ON; ' + sql]
    if output:
        command += ['-o', str(output)]
    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f'{sql[:200]}\n{result.stdout}\n{result.stderr}')


def check_row_counts(backup):
    problems = 0
    for table in backup.tables():
        declared = backup.declared_rows(table)
        read = len(backup.rows(table))
        if declared is not None and declared != read:
            problems += 1
            print(f'  {table}: the server counted {declared} rows, the reader found {read}')
    print(f'Row counts against sysindexes: {problems} tables differ')
    return problems


def load(backup, database, folder):
    """Loads the tables and creates the program's functions and views. True when the VAT table exists."""
    present = set(backup.tables())
    for table, columns in TABLES.items():
        if table not in present:
            continue
        names = [c.split()[0] for c in columns.split(', ')]
        path = Path(folder) / f'{table}.tsv'
        with open(path, 'w', newline='', encoding='cp1252', errors='replace') as target:
            writer = csv.writer(target, delimiter=SEPARATOR, quoting=csv.QUOTE_NONE, escapechar='\\')
            for row in backup.rows(table):
                writer.writerow(['' if row.get(n) is None else _sql_text(row[n]) for n in names])
        run_sql(f'CREATE TABLE {table} ({columns})', database)
        run_sql(f"BULK INSERT {table} FROM '{path}' WITH (FIELDTERMINATOR='\\t', ROWTERMINATOR='\\n', CODEPAGE='1252')", database)
    with_vat_table = 'iva' in present
    for function in FUNCTIONS if with_vat_table else ():
        create(backup.definition(function), database)
    for view in VIEWS:
        create(backup.definition(view), database)
    if with_vat_table:
        create(literal_view(backup.definition('fahi')), database)
    return with_vat_table


def create(definition, database):
    run_sql(definition.replace("'", "''").join(("EXEC('", "')")), database)


def literal_view(definition):
    """fahi as it was before the VAT table, named fahi_literal."""
    for call, literal in LITERAL_RATES.items():
        if call not in definition:
            raise ValueError(f'fahi no longer calls {call}: the literal view cannot be rebuilt')
        definition = definition.replace(call, literal)
    return re.sub(r'(?i)create\s+view\s+(\[dbo\]\.)?\[?fahi\]?', 'CREATE VIEW fahi_literal', definition, count=1)


def _sql_text(value):
    if hasattr(value, 'strftime'):
        return value.strftime('%Y-%m-%dT%H:%M:%S')
    return str(value).strip().replace('\t', ' ')


def fetch(sql, database, folder, name):
    path = Path(folder) / f'{name}.out'
    run_sql(sql, database, path)
    with open(path, encoding='cp1252') as source:
        return [line.rstrip('\r\n').split(SEPARATOR) for line in source if line.strip()]


def as_float(text):
    return 0.0 if text == 'NULL' else float(text)


def invoice_views(database, folder, with_vat_table):
    """What the program printed for each invoice: before the VAT table, the literal view; since, the current one."""
    columns = ('SELECT ind, CONVERT(varchar(23), fec, 126), CONVERT(varchar(40), bas, 3), CONVERT(varchar(40), iva, 3), '
               'CONVERT(varchar(40), rc, 3), rec FROM ')
    current = fetch(columns + 'fahi', database, folder, 'invoices')
    if not with_vat_table:
        return [(ind, *rest) for ind, _, *rest in current]
    literal = {row[0]: row for row in fetch(columns + 'fahi_literal', database, folder, 'invoices_literal')}
    since = VAT_TABLE_SINCE.isoformat()
    chosen = [literal[row[0]] if row[1] == 'NULL' or row[1] < since else row for row in current]
    return [(row[0], *row[2:]) for row in chosen]


def compare(target, database, folder, with_vat_table):
    db = sqlite3.connect(target)
    results = Counter()

    # Line by line inside each note: the import renumbers the few lines whose id the old table repeated
    stored_lines = defaultdict(list)
    for note, total in db.execute('select delivery_note_id, total from delivery_notes_articles'):
        stored_lines[note].append(Decimal(str(total)).quantize(Decimal('0.01')))
    view_lines = defaultdict(list)
    for note, imp in fetch('SELECT alb, CONVERT(varchar(40), imp, 3) FROM detart', database, folder, 'lines'):
        view_lines[int(note)].append(printed(as_float(imp)))
        results['lines'] += 1
    for note, totals in view_lines.items():
        stored = stored_lines.pop(note, [])
        if sorted(stored) != sorted(totals):
            results['lines_different'] += len(totals)
            print(f'  note {note}: the view gives lines {sorted(totals)}, the import stored {sorted(stored)}')
    results['lines_only_in_import'] += sum(len(v) for v in stored_lines.values())

    notes = dict(db.execute('select id, total from delivery_notes'))
    for ind, imp in fetch('SELECT ind, CONVERT(varchar(40), imp, 3) FROM albi', database, folder, 'notes'):
        stored = notes.pop(int(ind), None)
        results['notes'] += 1
        if stored is None or Decimal(str(stored)).quantize(Decimal('0.01')) != printed(as_float(imp)):
            results['notes_different'] += 1
            print(f'  note {ind}: the view gives {imp}, the import stored {stored}')
    results['notes_only_in_import'] += len(notes)

    invoices = {row[0]: row[1:] for row in db.execute(
        'select id, subtotal, vat_amount, surcharge_amount, total, surcharge_percentage from invoices')}
    for ind, bas, iva, rc, rec in invoice_views(database, folder, with_vat_table):
        stored = invoices.pop(int(ind), None)
        results['invoices'] += 1
        if stored is None:
            results['invoices_only_in_view'] += 1
            print(f'  invoice id {ind} is not in the import')
            continue
        subtotal, vat, surcharge, total, surcharge_percentage = (Decimal(str(v)).quantize(Decimal('0.01')) for v in stored)
        base, tax, extra = as_float(bas), as_float(iva), as_float(rc)
        if subtotal != printed(base):
            results['invoices_base_different'] += 1
            print(f'  invoice id {ind}: base {bas} against {subtotal}')
        if vat != printed(tax):
            results['invoices_vat_different'] += 1
            print(f'  invoice id {ind}: VAT {iva} against {vat}')
        if surcharge == printed(extra) and total == printed(base + tax + extra):
            continue
        # The view applies the surcharge flag the client has today; the import, the one it had then
        if (surcharge_percentage != 0) != (rec == '1'):
            results['invoices_surcharge_regime_changed_since'] += 1
        else:
            results['invoices_total_different'] += 1
            print(f'  invoice id {ind}: surcharge {rc} against {surcharge}, total against {total}')
    results['invoices_only_in_import'] += len(invoices)
    db.close()
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup', help='The backup that was imported')
    parser.add_argument('target', help='The imported database')
    args = parser.parse_args()

    backup = open_backup(args.backup)
    database = f'legacy_check_{os.getpid()}'
    try:
        failures = check_row_counts(backup)
        run_sql(f'CREATE DATABASE {database}')
        try:
            with tempfile.TemporaryDirectory() as folder:
                with_vat_table = load(backup, database, folder)
                results = compare(args.target, database, folder, with_vat_table)
        finally:
            run_sql(f'ALTER DATABASE {database} SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE {database}')
    finally:
        backup.close()
    for key, value in sorted(results.items()):
        print(f'{key}: {value}')
    failures += sum(v for k, v in results.items() if 'different' in k or 'only_in' in k)
    print('RESULT:', 'OK' if not failures else f'{failures} differences')
    sys.exit(1 if failures else 0)


if __name__ == '__main__':
    main()
