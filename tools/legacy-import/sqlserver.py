"""Reads backups of SQL Server 2005 and later by restoring them in LocalDB, which still opens them.

Same interface as sql2000.Backup. The restored database is temporary: close() drops it.
"""
import json
import shutil
import subprocess
import tempfile
import uuid
from datetime import datetime
from decimal import Decimal
from pathlib import Path

from legacy_rules import as_real

SERVER = r'(localdb)\MSSQLLocalDB'
SEPARATOR = '\t'


def run(query, database='master', table=False):
    """Runs T-SQL with sqlcmd. A table comes back as rows of fields; otherwise, the raw output."""
    with tempfile.TemporaryDirectory() as folder:
        output = Path(folder) / 'out.txt'
        options = ['-W', '-h', '-1', '-s', SEPARATOR] if table else ['-y', '0']
        command = ['sqlcmd', '-S', SERVER, '-d', database, '-b', '-f', '65001', *options,
                   '-Q', 'SET NOCOUNT ON; ' + query, '-o', str(output)]
        result = subprocess.run(command, capture_output=True, text=True)
        text = output.read_text(encoding='utf-8-sig') if output.exists() else ''
        if result.returncode:
            raise RuntimeError(f'{query[:300]}\n{text}\n{result.stdout}\n{result.stderr}')
    if table:
        return [line.split(SEPARATOR) for line in text.splitlines() if line.strip()]
    return text


def backup_sets(path):
    """(position, database version, finish date) of each backup set in the file."""
    rows = run(f"RESTORE HEADERONLY FROM DISK = N'{path}'", table=True)
    return [(int(r[5]), int(r[10]), r[18]) for r in rows]


class RestoredBackup:
    def __init__(self, path, position=None):
        self.path = path
        self.position = position or max(p for p, _, _ in backup_sets(path))
        self.database = f'legacy_read_{uuid.uuid4().hex[:12]}'
        self.folder = tempfile.mkdtemp(prefix='legacy-read-')
        files = run(f"RESTORE FILELISTONLY FROM DISK = N'{path}' WITH FILE = {self.position}", table=True)
        moves = ', '.join(f"MOVE N'{f[0]}' TO N'{Path(self.folder) / (f[0] + ('.ldf' if f[2] == 'L' else '.mdf'))}'" for f in files)
        run(f"RESTORE DATABASE [{self.database}] FROM DISK = N'{path}' WITH FILE = {self.position}, {moves}, RECOVERY")
        self.objects = {row['name']: row['type'].strip() for row in self._json(
            "SELECT name, type FROM sys.objects WHERE is_ms_shipped = 0")}

    def close(self):
        run(f'ALTER DATABASE [{self.database}] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [{self.database}]')
        shutil.rmtree(self.folder, ignore_errors=True)

    def _json(self, select):
        # FOR JSON splits a long result in rows of 2033 characters: joined, they are one document
        lines = run(f'{select} FOR JSON PATH, INCLUDE_NULL_VALUES', self.database).splitlines()
        start = next((i for i, line in enumerate(lines) if line.startswith('[')), None)
        return [] if start is None else json.loads(''.join(lines[start:]))

    def tables(self):
        return sorted(name for name, kind in self.objects.items() if kind == 'U')

    def columns(self, table):
        return [(row['name'], row['type']) for row in self._json(
            f"SELECT c.name, t.name AS type FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id "
            f"WHERE c.object_id = OBJECT_ID(N'[{table}]') ORDER BY c.column_id")]

    def rows(self, table):
        columns = self.columns(table)
        # Floats and money travel as text with all their digits; JSON numbers would round them
        expressions = ', '.join(_exported(name, kind) for name, kind in columns)
        rows = self._json(f'SELECT {expressions} FROM [{table}] ORDER BY 1')
        return [{name: _imported(row[name], kind) for name, kind in columns} for row in rows]

    def definition(self, name):
        rows = self._json(f"SELECT definition FROM sys.sql_modules WHERE object_id = OBJECT_ID(N'[{name}]')")
        return rows[0]['definition']

    def declared_rows(self, table):
        rows = self._json(f"SELECT SUM(rows) AS n FROM sys.partitions WHERE object_id = OBJECT_ID(N'[{table}]') AND index_id IN (0, 1)")
        return rows[0]['n']


def _exported(name, kind):
    if kind in ('real', 'float'):
        return f'CONVERT(varchar(40), [{name}], 3) AS [{name}]'
    if kind in ('money', 'smallmoney'):
        return f'CONVERT(varchar(40), [{name}], 2) AS [{name}]'
    if kind in ('datetime', 'smalldatetime'):
        return f'CONVERT(varchar(23), [{name}], 126) AS [{name}]'
    return f'[{name}]'


def _imported(value, kind):
    if value is None:
        return None
    if kind == 'real':
        return as_real(float(value))
    if kind == 'float':
        return float(value)
    if kind in ('money', 'smallmoney'):
        return Decimal(value)
    if kind in ('datetime', 'smalldatetime'):
        return datetime.fromisoformat(value)
    return value
