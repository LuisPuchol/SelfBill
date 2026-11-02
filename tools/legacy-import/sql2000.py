"""Reads SQL Server 2000 database backups without a server.

Current SQL Server versions refuse to restore them, so this walks the 8 KB data pages inside the
backup file, takes the schema from the backup's own syscolumns and decodes the FixedVar records.
Pages the allocation map marks as free and ghost records are skipped: they hold deleted rows.
"""
import struct
from datetime import datetime, timedelta
from decimal import Decimal

PAGE_SIZE = 8192
SECTOR_SIZE = 512
EPOCH = datetime(1900, 1, 1)

PAGE_TYPE_DATA = 1
PAGE_TYPE_PFS = 11
KNOWN_PAGE_TYPES = {1, 2, 3, 4, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17}

PFS_HEADER = 100
PFS_PAGES = 8088
PFS_ALLOCATED = 0x40

SYSOBJECTS = 1
SYSINDEXES = 2
SYSCOLUMNS = 3

STATUS_NULL_BITMAP = 0x10
STATUS_VARIABLE = 0x20
RECORD_PRIMARY = 0
RECORD_FORWARDED = 1

COLSTAT_COMPUTED = 4
OFF_ROW_BIT = 0x7FFF


class Backup:
    def __init__(self, path):
        self.path = path
        self.pages = self._find_pages(open(path, 'rb').read())
        self.allocated = self._read_allocation()
        self.columns_by_object, self.objects = self._read_catalog()

    @staticmethod
    def _find_pages(data):
        pages = {}
        for offset in range(0, len(data) - PAGE_SIZE + 1, SECTOR_SIZE):
            if data[offset] != 1 or data[offset + 1] not in KNOWN_PAGE_TYPES:
                continue
            page_id = struct.unpack_from('<I', data, offset + 32)[0]
            file_id = struct.unpack_from('<H', data, offset + 36)[0]
            slots = struct.unpack_from('<H', data, offset + 22)[0]
            if file_id == 1 and page_id < 2_000_000 and slots <= 4096:
                pages[page_id] = data[offset:offset + PAGE_SIZE]
        return pages

    def _read_allocation(self):
        allocated = {}
        for page_id, page in self.pages.items():
            if page[1] == PAGE_TYPE_PFS:
                first = 0 if page_id == 1 else page_id
                for i in range(PFS_PAGES):
                    allocated[first + i] = bool(page[PFS_HEADER + i] & PFS_ALLOCATED)
        return allocated

    def _records(self, object_id):
        """Live records of an object as (page, offset), by page and slot order."""
        for page_id in sorted(self.pages):
            page = self.pages[page_id]
            if page[1] != PAGE_TYPE_DATA or struct.unpack_from('<I', page, 24)[0] != object_id:
                continue
            if not self.allocated.get(page_id, True):
                continue
            for slot in range(struct.unpack_from('<H', page, 22)[0]):
                offset = struct.unpack_from('<H', page, PAGE_SIZE - 2 - 2 * slot)[0]
                if offset and (page[offset] >> 1) & 7 in (RECORD_PRIMARY, RECORD_FORWARDED):
                    yield page, offset

    def _read_catalog(self):
        # syscolumns has a fixed layout: it is read by hand, and it describes everything else
        columns = {}
        for page, offset in self._records(SYSCOLUMNS):
            colstat = struct.unpack_from('<h', page, offset + 22)[0]
            number = struct.unpack_from('<h', page, offset + 32)[0]
            variable = _variable_columns(page, offset)
            if number != 0 or colstat & COLSTAT_COMPUTED or not variable:
                continue
            columns.setdefault(struct.unpack_from('<i', page, offset + 4)[0], []).append({
                'name': variable[0].decode('utf-16-le'),
                'xtype': page[offset + 8],
                'length': struct.unpack_from('<h', page, offset + 12)[0],
                'scale': page[offset + 15],
                'colid': struct.unpack_from('<h', page, offset + 16)[0],
                'xoffset': struct.unpack_from('<h', page, offset + 18)[0],
                'bitpos': page[offset + 20],
            })
        for table_columns in columns.values():
            table_columns.sort(key=lambda column: column['colid'])

        objects = {}
        for page, offset in self._records(SYSOBJECTS):
            row = _decode_record(page, offset, columns[SYSOBJECTS])
            objects[row['id']] = {'name': row['name'], 'xtype': (row['xtype'] or '').strip()}
        return columns, objects

    def _table_id(self, table):
        return next(i for i, o in self.objects.items() if o['name'] == table and o['xtype'] == 'U')

    def tables(self):
        return sorted(o['name'] for o in self.objects.values() if o['xtype'] == 'U')

    def rows(self, table):
        object_id = self._table_id(table)
        columns = self.columns_by_object[object_id]
        return [_decode_record(page, offset, columns) for page, offset in self._records(object_id)]

    def close(self):
        """Nothing to release: the file was read whole. Same interface as sqlserver.RestoredBackup."""

    def definition(self, name):
        """The source of a view, procedure or function, as the server keeps it in syscomments."""
        comments_id = next(i for i, o in self.objects.items() if o['name'] == 'syscomments')
        object_id = next(i for i, o in self.objects.items() if o['name'] == name and o['xtype'] in ('V', 'P', 'FN'))
        columns = self.columns_by_object[comments_id]
        parts = {}
        for page, offset in self._records(comments_id):
            row = _decode_record(page, offset, columns)
            if row['id'] == object_id:
                parts[row['colid']] = row['ctext']
        text = b''.join(parts[colid] for colid in sorted(parts))
        # Text created through a Unicode client is UTF-16, the rest ANSI
        return text.decode('utf-16-le') if text[1::2].count(0) > len(text) / 4 else text.decode('cp1252')

    def declared_rows(self, table):
        """The row count the server kept in sysindexes, to check that no page was missed."""
        object_id = self._table_id(table)
        columns = self.columns_by_object[SYSINDEXES]
        for page, offset in self._records(SYSINDEXES):
            row = _decode_record(page, offset, columns)
            if row['id'] == object_id and row['indid'] in (0, 1):
                return int.from_bytes(row['rowcnt'], 'little')  # binary(8) in this version
        return None


def open_backup(path):
    """SQL Server 2000 backups are read page by page; later ones need LocalDB to restore them."""
    import sqlserver
    try:
        versions = {version for _, version, _ in sqlserver.backup_sets(path)}
    except (OSError, RuntimeError):
        return Backup(path)  # without LocalDB, only this reader is available
    return Backup(path) if max(versions) < SQL2005_DATABASE_VERSION else sqlserver.RestoredBackup(path)


SQL2005_DATABASE_VERSION = 611


def _variable_columns(page, offset):
    status = page[offset]
    position = offset + struct.unpack_from('<H', page, offset + 2)[0]
    column_count = struct.unpack_from('<H', page, position)[0]
    position += 2
    if status & STATUS_NULL_BITMAP:
        position += (column_count + 7) // 8
    if not status & STATUS_VARIABLE:
        return []
    count = struct.unpack_from('<H', page, position)[0]
    position += 2
    ends = [struct.unpack_from('<H', page, position + 2 * i)[0] & OFF_ROW_BIT for i in range(count)]
    start = position + 2 * count - offset
    values = []
    for end in ends:
        values.append(page[offset + start:offset + end])
        start = end
    return values


def _decode_record(page, offset, columns):
    position = offset + struct.unpack_from('<H', page, offset + 2)[0]
    column_count = struct.unpack_from('<H', page, position)[0]
    bitmap = b''
    if page[offset] & STATUS_NULL_BITMAP:
        bitmap = page[position + 2:position + 2 + (column_count + 7) // 8]
    variable = _variable_columns(page, offset)

    row = {}
    for column in columns:
        index = column['colid'] - 1
        if bitmap and index < column_count and bitmap[index // 8] >> (index % 8) & 1:
            row[column['name']] = None
        elif column['xoffset'] < 0:
            position = -column['xoffset'] - 1
            row[column['name']] = decode_value(column, variable[position] if position < len(variable) else None)
        else:
            start = offset + column['xoffset']
            row[column['name']] = decode_value(column, page[start:start + column['length']])
    return row


def decode_value(column, raw):
    if raw is None:
        return None
    xtype = column['xtype']
    if xtype in DECODERS:
        return DECODERS[xtype](raw)
    if xtype in (106, 108):
        sign = 1 if raw[0] == 1 else -1
        return Decimal(sign * int.from_bytes(raw[1:], 'little')).scaleb(-column['scale'])
    if xtype == 104:
        return bool(raw[0] >> column['bitpos'] & 1)
    return raw


def _datetime(raw):
    ticks, days = struct.unpack('<ii', raw)
    return EPOCH + timedelta(days=days, milliseconds=ticks * 10 / 3)


def _smalldatetime(raw):
    minutes, days = struct.unpack('<HH', raw)
    return EPOCH + timedelta(days=days, minutes=minutes)


DECODERS = {
    48: lambda raw: raw[0],
    52: lambda raw: struct.unpack('<h', raw)[0],
    56: lambda raw: struct.unpack('<i', raw)[0],
    127: lambda raw: struct.unpack('<q', raw)[0],
    59: lambda raw: struct.unpack('<f', raw)[0],
    62: lambda raw: struct.unpack('<d', raw)[0],
    60: lambda raw: Decimal(struct.unpack('<q', raw)[0]).scaleb(-4),
    122: lambda raw: Decimal(struct.unpack('<i', raw)[0]).scaleb(-4),
    61: _datetime,
    58: _smalldatetime,
    175: lambda raw: raw.decode('cp1252', 'replace'),
    167: lambda raw: raw.decode('cp1252', 'replace'),
    239: lambda raw: raw.decode('utf-16-le', 'replace'),
    231: lambda raw: raw.decode('utf-16-le', 'replace'),
}
