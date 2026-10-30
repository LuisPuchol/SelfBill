#!/usr/bin/env python3
"""Checks an imported database against the views of the old program, run by a real SQL Server.

Loads the rows of a backup into a temporary LocalDB database, creates the program's own views from the
backup's source, and compares what they compute with what the import stored. Independent of the import:
it shares the backup reader, not the arithmetic.
"""
import argparse
import csv
import os
import sqlite3
import subprocess
import sys
import tempfile
from collections import Counter
from decimal import Decimal
from pathlib import Path

from legacy_rules import printed
from sql2000 import Backup

SERVER = r'(localdb)\MSSQLLocalDB'
SEPARATOR = '\t'
TABLES = {
    'art': 'ind int, nom char(35)',
    'cli': 'ind int, nom char(30), rec tinyint',
    'alb': 'ind int, ano int, cli int, fec datetime, fno int',
    'det': 'ind int, alb int, art int, tr1 char(15), tr2 char(15), tr3 char(15), ctd real, prv money',
    'fah': 'ind int, num int, fec datetime, cli int, typ tinyint, cob datetime, ret int',
}
VIEWS = ('detart', 'albi', 'fahi')


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
    for table, columns in TABLES.items():
        names = [c.split()[0] for c in columns.split(', ')]
        path = Path(folder) / f'{table}.tsv'
        with open(path, 'w', newline='', encoding='cp1252', errors='replace') as target:
            writer = csv.writer(target, delimiter=SEPARATOR, quoting=csv.QUOTE_NONE, escapechar='\\')
            for row in backup.rows(table):
                writer.writerow(['' if row.get(n) is None else _sql_text(row[n]) for n in names])
        run_sql(f'CREATE TABLE {table} ({columns})', database)
        run_sql(f"BULK INSERT {table} FROM '{path}' WITH (FIELDTERMINATOR='\\t', ROWTERMINATOR='\\n', CODEPAGE='1252')", database)
    for view in VIEWS:
        run_sql(backup.definition(view).replace("'", "''").join(("EXEC('", "')")), database)


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


def compare(target, database, folder):
    db = sqlite3.connect(target)
    results = Counter()

    lines = dict(db.execute('select id, total from delivery_notes_articles'))
    for ind, imp in fetch('SELECT ind, CONVERT(varchar(40), imp, 3) FROM detart', database, folder, 'lines'):
        stored = lines.pop(int(ind), None)
        results['lines'] += 1
        if stored is None or Decimal(str(stored)).quantize(Decimal('0.01')) != printed(as_float(imp)):
            results['lines_different'] += 1
            print(f'  line {ind}: the view gives {imp}, the import stored {stored}')
    results['lines_only_in_import'] += len(lines)

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
    views = fetch('SELECT ind, CONVERT(varchar(40), bas, 3), CONVERT(varchar(40), iva, 3), CONVERT(varchar(40), rc, 3), rec '
                  'FROM fahi', database, folder, 'invoices')
    for ind, bas, iva, rc, rec in views:
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
        # The view applies the surcharge flag the client has today; the import, the one it had then
        if (surcharge_percentage != 0) != (rec == '1'):
            results['invoices_surcharge_regime_changed_since'] += 1
        elif surcharge != printed(extra) or total != printed(base + tax + extra):
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

    backup = Backup(args.backup)
    failures = check_row_counts(backup)
    database = f'legacy_check_{os.getpid()}'
    run_sql(f'CREATE DATABASE {database}')
    try:
        with tempfile.TemporaryDirectory() as folder:
            load(backup, database, folder)
            results = compare(args.target, database, folder)
    finally:
        run_sql(f'ALTER DATABASE {database} SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE {database}')
    for key, value in sorted(results.items()):
        print(f'{key}: {value}')
    failures += sum(v for k, v in results.items() if 'different' in k or 'only_in' in k)
    print('RESULT:', 'OK' if not failures else f'{failures} differences')
    sys.exit(1 if failures else 0)


if __name__ == '__main__':
    main()
