#!/usr/bin/env python3
"""Builds a SelfBill database from the backups of the old FoxPro + SQL Server 2000 program. See README.md."""
import argparse
import csv
import shutil
import sqlite3
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from pathlib import Path

import legacy_rules as rules
from sql2000 import Backup

DATE_DOUBT_DAYS = 45
TAX_VALID_FROM = datetime(2001, 1, 1)


@dataclass(frozen=True)
class ClientVersion:
    code: int
    name: str
    nif: str | None
    address: str | None
    postal_code: str | None
    city: str | None
    province: str | None
    phone: str | None
    phone2: str | None
    taxed: bool
    surcharged: bool
    valued: bool

    def same_as(self, other):
        return rules.same_party(self.name, self.nif, other.name, other.nif)


@dataclass(frozen=True)
class ArticleVersion:
    code: int
    name: str

    def same_as(self, other):
        return rules.comparable_name(self.name) == rules.comparable_name(other.name)


def client_version(row):
    location = rules.split_location(row['cop'], row['pob'], row['pvn'])
    return ClientVersion(
        code=row['ind'],
        name=rules.clean_text(row['nom']) or f"CLIENTE {row['ind']}",
        nif=rules.clean_nif(row['cif']),
        address=rules.clean_text(row['dir']),
        phone=rules.clean_text(row['tel']),
        phone2=rules.clean_text(row['te2']) or rules.clean_text(row['fax']),
        taxed=row['typ'] != rules.TYPE_WITHOUT_VAT,
        surcharged=row['rec'] == 1,
        # fra=1 invoiced each delivery note on the spot: the client SelfBill's valued format exists for
        valued=row.get('fra') == 1,
        **location)


def article_version(row):
    return ArticleVersion(code=row['ind'], name=rules.clean_text(row['nom']) or f"ARTICULO {row['ind']}")


@dataclass
class Identity:
    """One real client or article behind a code: the old program reused codes for somebody else."""
    code: int
    versions: list = field(default_factory=list)  # (snapshot index, version)
    row_id: int | None = None
    used: bool = False
    replaced_on: datetime | None = None

    @property
    def latest(self):
        return self.versions[-1][1]


class CodeHistory:
    """What each code meant in each backup, so a document gets the client it really had."""

    def __init__(self, snapshots, table, build):
        self.snapshots = snapshots
        self.versions = defaultdict(list)
        self.identities = defaultdict(list)
        self.windows = {}
        self.last_index = len(snapshots) - 1
        for index, (_, backup) in enumerate(snapshots):
            for row in backup.rows(table):
                version = build(row)
                history = self.versions[version.code]
                if not history or history[-1][1] != version:
                    history.append((index, version))
                self._add_to_identity(version.code, index, version)
        self._close_replaced()

    def _add_to_identity(self, code, index, version):
        for identity in self.identities[code]:
            if identity.latest.same_as(version):
                identity.versions.append((index, version))
                return
        identity = Identity(code)
        identity.versions.append((index, version))
        self.identities[code].append(identity)

    def _close_replaced(self):
        for code, identities in self.identities.items():
            for identity in identities:
                last_seen = identity.versions[-1][0]
                if last_seen < self.last_index:
                    identity.replaced_on = self.snapshots[min(last_seen + 1, self.last_index)][0]

    def version_at(self, code, index):
        """The version in the first backup taken once the document existed."""
        history = self.versions.get(code)
        if not history:
            return None
        current = history[0][1]
        for at, version in history:
            if at > index:
                break
            current = version
        return current

    def place_changes(self, dates_by_code_and_index):
        """A code that changed hands between two backups: its long silence tells when, if it has one."""
        for code, history in self.versions.items():
            for (_, before), (index, after) in zip(history, history[1:]):
                if before.same_as(after):
                    continue
                dates = dates_by_code_and_index.get((code, index), [])
                self.windows[(code, index)] = (before, rules.split_after_silence(dates))

    def resolve(self, code, index, date):
        """The version a document had, and whether that is only an estimate."""
        version = self.version_at(code, index)
        window = self.windows.get((code, index))
        if window is None:
            return version, False
        before, cut = window
        return (before if cut and date and date < cut else version), True

    def identity_of(self, version):
        for identity in self.identities[version.code]:
            if any(v == version or v.same_as(version) for _, v in identity.versions):
                return identity
        raise ValueError(f'no identity for {version}')


def moneyed(value):
    return str(value)


def epoch_ms(value):
    """Hibernate keeps a LocalDateTime as epoch milliseconds in the zone of the machine."""
    return int(value.timestamp() * 1000)


class Importer:

    def __init__(self, snapshots, logo):
        self.snapshots = snapshots
        self.logo = logo
        self.backup = snapshots[-1][1]
        self.anomalies = []
        self.estimated = []
        self.stats = Counter()

    def anomaly(self, kind, document, detail):
        self.anomalies.append({'tipo': kind, 'documento': document, 'detalle': detail})

    def load(self):
        self.notes = {row['ind']: row for row in self.backup.rows('alb')}
        self.invoices = {row['ind']: row for row in self.backup.rows('fah')}
        self.lines = defaultdict(list)
        for row in sorted(self.backup.rows('det'), key=lambda r: r['ind']):
            if row['art'] == 0:  # the old printouts joined on the article, so these never showed
                self.stats['lines_without_article'] += 1
                self.anomaly('linea descartada', f"albarán id {row['alb']}", 'línea sin artículo (código 0), cantidad 0')
                continue
            self.lines[row['alb']].append(row)
        self.note_first_seen = self._first_seen('alb')
        self.invoice_first_seen = self._first_seen('fah')
        self.clients = CodeHistory(self.snapshots, 'cli', client_version)
        self.articles = CodeHistory(self.snapshots, 'art', article_version)

    def _first_seen(self, table):
        first = {}
        for index, (_, backup) in enumerate(self.snapshots):
            for row in backup.rows(table):
                first.setdefault(row['ind'], index)
        return first

    def discard_phantom_invoices(self):
        with_notes = {note['fno'] for note in self.notes.values() if note['fno']}
        for ind, invoice in list(self.invoices.items()):
            if ind not in with_notes:
                del self.invoices[ind]
                self.stats['phantom_invoices'] += 1
                self.anomaly('factura descartada', f"id {ind}", f"número {invoice['num']}, tipo {invoice['typ']}, "
                             f"cliente {invoice['cli']}: sin albaranes ni fecha, nunca fue un documento")

    def resolve_dates(self):
        """Every date stays as it was printed. Only a missing one is inferred: the columns need a value."""
        ordered = sorted(self.notes.values(), key=lambda n: n['ano'])
        self.note_date = {}
        for position, note in enumerate(ordered):
            date = note['fec']
            if date is None:
                invoice = self.invoices.get(note['fno'])
                date = invoice['fec'] if invoice else None
                date = date or self._nearest_date(ordered, position)
                self.stats['note_dates_inferred'] += 1
                self.anomaly('fecha inferida', f"albarán {note['ano']}", f"sin fecha en el programa antiguo; se usa {date:%d/%m/%Y}")
            self.note_date[note['ind']] = date
        self._report_odd_note_dates(ordered)

        self.invoice_date = {}
        notes_of = self._notes_by_invoice()
        for ind, invoice in self.invoices.items():
            latest = max(self.note_date[n['ind']] for n in notes_of[ind])
            date = invoice['fec']
            if date is None:
                date = latest
                self.stats['invoice_dates_inferred'] += 1
                self.anomaly('fecha inferida', self._invoice_label(invoice), f"sin fecha en el programa antiguo; se usa la de su albarán, {date:%d/%m/%Y}")
            elif abs((date - latest).days) > DATE_DOUBT_DAYS:
                self.stats['odd_invoice_dates'] += 1
                self.anomaly('fecha dudosa', self._invoice_label(invoice), f"fecha {date:%d/%m/%Y}, su último albarán es de {latest:%d/%m/%Y}; se conserva tal cual")
            self.invoice_date[ind] = date

    @staticmethod
    def _nearest_date(ordered, position):
        for distance in range(1, len(ordered)):
            for other in (position - distance, position + distance):
                if 0 <= other < len(ordered) and ordered[other]['fec']:
                    return ordered[other]['fec']
        raise ValueError('no dates at all')

    def _report_odd_note_dates(self, ordered):
        dates = [note['fec'] for note in ordered]
        for position, note in enumerate(ordered):
            if note['fec'] is None:
                continue
            around = [d for d in dates[max(0, position - 5):position + 6] if d]
            middle = sorted(around)[len(around) // 2]
            if abs((note['fec'] - middle).days) > DATE_DOUBT_DAYS:
                self.stats['odd_note_dates'] += 1
                self.anomaly('fecha dudosa', f"albarán {note['ano']}", f"fecha {note['fec']:%d/%m/%Y}, sus vecinos son de {middle:%d/%m/%Y}; se conserva tal cual")

    def _notes_by_invoice(self):
        notes_of = defaultdict(list)
        for note in self.notes.values():
            if note['fno'] in self.invoices:
                notes_of[note['fno']].append(note)
        return notes_of

    def number_invoices(self):
        """Keeps every number. Only types with a counter SelfBill does not have go after the general series."""
        self.number = {}
        general = [i['num'] for i in self.invoices.values() if i['typ'] == rules.TYPE_GENERAL]
        next_general = max(general, default=0) + 1
        for ind, invoice in sorted(self.invoices.items(), key=lambda kv: self.invoice_date[kv[0]]):
            if invoice['typ'] == rules.TYPE_WITHOUT_VAT:
                self.number[ind] = ('WITHOUT_VAT', invoice['num'])
            elif invoice['typ'] == rules.TYPE_GENERAL:
                self.number[ind] = ('GENERAL', invoice['num'])
            else:
                self.number[ind] = ('GENERAL', next_general)
                self.stats['renumbered_invoices'] += 1
                self.anomaly('factura renumerada', f"tipo {invoice['typ']}, número {invoice['num']}",
                             f"tenía contador propio, que SelfBill no tiene; ahora es la {next_general}")
                next_general += 1

    def place_identity_changes(self):
        client_dates = defaultdict(list)
        article_dates = defaultdict(list)
        for ind, note in self.notes.items():
            index = self.note_first_seen[ind]
            date = self.note_date[ind]
            client_dates[(note['cli'], index)].append(date)
            for line in self.lines[ind]:
                article_dates[(line['art'], index)].append(date)
        self.clients.place_changes(client_dates)
        self.articles.place_changes(article_dates)

    def _invoice_label(self, invoice):
        prefix = 'S-' if invoice['typ'] == rules.TYPE_WITHOUT_VAT else ''
        return f"factura {prefix}{invoice['num']}"

    def resolve_parties(self):
        self.invoice_client = {}
        for ind, invoice in self.invoices.items():
            version, estimated = self.clients.resolve(invoice['cli'], self.invoice_first_seen[ind], self.invoice_date[ind])
            if version is None:
                raise ValueError(f"invoice {ind} points to unknown client {invoice['cli']}")
            self.invoice_client[ind] = version
            self.clients.identity_of(version).used = True
            if estimated:
                self.estimated.append({
                    'documento': self._invoice_label(invoice), 'fecha': f'{self.invoice_date[ind]:%d/%m/%Y}',
                    'codigo': version.code, 'cliente asignado': version.name, 'nif': version.nif or ''})
                self.stats['estimated_invoices'] += 1

        self.note_client = {}
        for ind, note in self.notes.items():
            invoice = self.invoices.get(note['fno'])
            if invoice and invoice['cli'] == note['cli']:
                self.note_client[ind] = self.invoice_client[note['fno']]
                continue
            version, _ = self.clients.resolve(note['cli'], self.note_first_seen[ind], self.note_date[ind])
            self.note_client[ind] = version
            self.clients.identity_of(version).used = True
            if invoice:
                self.stats['notes_of_another_client'] += 1
                self.anomaly('cliente distinto', f"albarán {note['ano']}",
                             f"su cliente es el {note['cli']}, el de su {self._invoice_label(invoice)} es el {invoice['cli']}; se conserva tal cual")

    def compute(self):
        self.note_lines = {}
        self.note_sum = {}
        for ind in self.notes:
            lines = []
            for line in self.lines[ind]:
                version, _ = self.articles.resolve(line['art'], self.note_first_seen[ind], self.note_date[ind])
                if version is None:
                    version = ArticleVersion(line['art'], f"ARTICULO {line['art']}")
                    self.anomaly('artículo desconocido', f"línea id {line['ind']}", f"artículo {line['art']} no aparece en ninguna copia")
                self.articles.identity_of(version).used = True
                amount = rules.line_amount(line['ctd'], line['prv'])
                lots = rules.lot_slots(line['tr1'], line['tr2'], line['tr3'])
                self._count_lots(line, lots)
                lines.append({
                    'id': line['ind'], 'article': version, 'amount': amount, 'lots': lots,
                    'quantity': rules.printed(line['ctd']), 'price': line['prv'].quantize(rules.CENT),
                    'total': rules.printed(amount)})
                if line['ctd'] < 0:
                    self.stats['negative_quantity_lines'] += 1
                if line['ctd'] == 0:
                    self.stats['zero_quantity_lines'] += 1
            self.note_lines[ind] = lines
            self.note_sum[ind] = rules.plain_sum(line['amount'] for line in lines)

        self.invoice_amounts = {}
        for ind, notes in self._notes_by_invoice().items():
            invoice = self.invoices[ind]
            self.invoice_amounts[ind] = rules.invoice_amounts(
                [self.note_sum[n['ind']] for n in notes],
                taxed=invoice['typ'] != rules.TYPE_WITHOUT_VAT,
                surcharged=self.invoice_client[ind].surcharged,
                retention=invoice.get('ret'))

    def _count_lots(self, line, lots):
        for raw in (line['tr1'], line['tr2'], line['tr3']):
            text = (raw or '').strip()
            if not text:
                continue
            cleaned = rules.clean_lot(text)
            if cleaned is None:
                self.stats['lots_emptied'] += 1
            elif cleaned != text:
                self.stats['lots_cleaned'] += 1
        self.stats['lines'] += 1
        if not lots[0]:
            self.stats['lines_without_lot'] += 1
        for lot in lots:
            if lot and len(lot) != 8:
                self.stats['lots_not_8_digits'] += 1

    def write(self, template, out):
        shutil.copyfile(template, out)
        db = sqlite3.connect(out)
        db.execute('PRAGMA foreign_keys = ON')
        with db:
            self._write_catalog(db)
            self._write_invoices(db)
            self._write_notes(db)
            self._write_settings(db)
        problems = db.execute('PRAGMA foreign_key_check').fetchall()
        if problems:
            raise ValueError(f'foreign keys broken: {problems[:5]}')
        db.execute('PRAGMA journal_mode = DELETE')
        db.close()

    def _write_catalog(self, db):
        now = epoch_ms(self.snapshots[-1][0])
        for code in sorted(self.articles.identities):
            for identity in self.articles.identities[code]:
                if identity.replaced_on is not None and not identity.used:
                    continue
                version = identity.latest
                deleted = epoch_ms(identity.replaced_on) if identity.replaced_on else None
                identity.row_id = db.execute(
                    'insert into articles (code, name, search_text, created_at, updated_at, deleted_at) values (?,?,?,?,?,?)',
                    (code, version.name, rules.search_text(version.name), now, now, deleted)).lastrowid
                self.stats['articles_live' if deleted is None else 'articles_replaced'] += 1

        for code in sorted(self.clients.identities):
            for identity in self.clients.identities[code]:
                if identity.replaced_on is not None and not identity.used:
                    continue
                version = identity.latest
                deleted = epoch_ms(identity.replaced_on) if identity.replaced_on else None
                identity.row_id = db.execute(
                    'insert into clients (code, business_name, name, nif, address, city, province, postal_code, phone, phone2,'
                    ' vat_type, surcharge_type, default_delivery_note_format, search_text, created_at, updated_at, deleted_at)'
                    ' values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                    (code, version.name, version.name, version.nif, version.address, version.city, version.province,
                     version.postal_code, version.phone, version.phone2,
                     'WITH_VAT' if version.taxed else 'WITHOUT_VAT', _surcharge_type(version.taxed and version.surcharged),
                     'VALUED' if version.valued else 'PLAIN',
                     rules.search_text(version.name, version.name, version.nif), now, now, deleted)).lastrowid
                self.stats['clients_live' if deleted is None else 'clients_replaced'] += 1

    def _write_invoices(self, db):
        fis = self._issuer()
        for ind, invoice in sorted(self.invoices.items()):
            series, number = self.number[ind]
            client = self.invoice_client[ind]
            amounts = self.invoice_amounts[ind]
            taxed = invoice['typ'] != rules.TYPE_WITHOUT_VAT
            surcharged = taxed and client.surcharged
            when = epoch_ms(self.invoice_date[ind])
            db.execute(
                'insert into invoices (id, series, code, date, status, client_id, client_code, client_business_name,'
                ' client_nif, client_address, client_city, client_province, client_postal_code, issuer_fiscal_name,'
                ' issuer_nif, issuer_address, subtotal, vat_percentage, vat_amount, surcharge_percentage,'
                ' surcharge_amount, total, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                (ind, series, number, when, 'ISSUED', self.clients.identity_of(client).row_id, client.code, client.name,
                 client.nif, client.address, client.city, client.province, client.postal_code,
                 fis['fiscal_name'], fis['nif'], fis['address'],
                 moneyed(amounts['subtotal']), moneyed(rules.OLD_VAT_PERCENTAGE if taxed else Decimal(0)),
                 moneyed(amounts['vat_amount']), moneyed(rules.OLD_SURCHARGE_PERCENTAGE if surcharged else Decimal(0)),
                 moneyed(amounts['surcharge_amount']), moneyed(amounts['total']), when, when))

    def _write_notes(self, db):
        for ind, note in sorted(self.notes.items()):
            invoice = self.invoices.get(note['fno'])
            client = self.note_client[ind]
            taxed = invoice['typ'] != rules.TYPE_WITHOUT_VAT if invoice else client.taxed
            surcharged = taxed and (self.invoice_client[note['fno']] if invoice else client).surcharged
            when = epoch_ms(self.note_date[ind])
            # Every old note printed plain ("Total Importe"), and that is the document the client received
            db.execute(
                'insert into delivery_notes (id, code, date, total, format, vat_percentage, surcharge_percentage,'
                ' vat_type, surcharge_type, client_id, invoice_id, created_at, updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?)',
                (ind, note['ano'], when, moneyed(rules.printed(self.note_sum[ind])), 'PLAIN',
                 moneyed(rules.OLD_VAT_PERCENTAGE if taxed else Decimal(0)),
                 moneyed(rules.OLD_SURCHARGE_PERCENTAGE if surcharged else Decimal(0)),
                 'WITH_VAT' if taxed else 'WITHOUT_VAT', _surcharge_type(surcharged),
                 self.clients.identity_of(client).row_id, note['fno'] if invoice else None, when, when))
            for line in self.note_lines[ind]:
                article_id = self.articles.identity_of(line['article']).row_id
                code1, code2, code3 = line['lots']
                db.execute(
                    'insert into delivery_notes_articles (id, article_id, delivery_note_id, traceability_code1,'
                    ' traceability_code2, traceability_code3, quantity, price, total, created_at, updated_at)'
                    ' values (?,?,?,?,?,?,?,?,?,?,?)',
                    (line['id'], article_id, ind, code1, code2, code3, moneyed(line['quantity']), moneyed(line['price']),
                     moneyed(line['total']), when, when))
                if invoice:
                    db.execute(
                        'insert into invoice_lines (invoice_id, delivery_note_id, article_id, delivery_note_code,'
                        ' delivery_note_date, article_name, article_code, traceability_code1, traceability_code2,'
                        ' traceability_code3, quantity, price, total, created_at, updated_at)'
                        ' values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',
                        (note['fno'], ind, article_id, note['ano'], when, line['article'].name, line['article'].code,
                         code1, code2, code3, moneyed(line['quantity']), moneyed(line['price']), moneyed(line['total']),
                         when, when))

    def _issuer(self):
        row = next(iter(self.backup.rows('cfg')))
        name = rules.clean_text(row['fis'])
        address = ', '.join(filter(None, (rules.clean_text(row['dir']), rules.clean_text(row['pob']))))
        return {'fiscal_name': name, 'nif': rules.issuer_nif(row['nif']), 'address': address}

    def _write_settings(self, db):
        now = epoch_ms(self.snapshots[-1][0])
        issuer = self._issuer()
        logo = Path(self.logo).read_bytes() if self.logo else None
        db.execute(
            'insert into user_profile (full_name, fiscal_name, nif, address, logo_data, logo_content_type, created_at, updated_at)'
            ' values (?,?,?,?,?,?,?,?)',
            (issuer['fiscal_name'], issuer['fiscal_name'], issuer['nif'], issuer['address'], logo,
             'image/jpeg' if logo else None, now, now))
        db.execute(
            'insert into tax_config (vat_percentage, surcharge_percentage, valid_from, created_at, updated_at) values (?,?,?,?,?)',
            (moneyed(rules.OLD_VAT_PERCENTAGE), moneyed(rules.OLD_SURCHARGE_PERCENTAGE), epoch_ms(TAX_VALID_FROM), now, now))

    def run(self, template, out):
        self.load()
        self.discard_phantom_invoices()
        self.resolve_dates()
        self.number_invoices()
        self.place_identity_changes()
        self.resolve_parties()
        self.compute()
        self.write(template, out)


def _surcharge_type(surcharged):
    return 'WITH_SURCHARGE' if surcharged else 'WITHOUT_SURCHARGE'


# ---- Annexes: what SelfBill has no place for, and everything a person should look at

def spanish(value):
    if isinstance(value, Decimal):
        return str(value).replace('.', ',')
    if isinstance(value, datetime):
        return f'{value:%d/%m/%Y}'
    return '' if value is None else value


def write_csv(path, rows, fields=None):
    rows = list(rows)
    fields = fields or (list(rows[0]) if rows else [])
    with open(path, 'w', newline='', encoding='utf-8-sig') as target:
        writer = csv.DictWriter(target, fields, delimiter=';')
        writer.writeheader()
        for row in rows:
            writer.writerow({k: spanish(v) for k, v in row.items()})


def write_annexes(importer, folder):
    folder.mkdir(parents=True, exist_ok=True)
    backup = importer.backup
    suppliers = {row['ind']: rules.clean_text(row['nom']) for row in backup.rows('prv')}
    categories = {row['ind']: rules.clean_text(row['nom']) for row in backup.rows('gst')}

    write_csv(folder / 'anomalias.csv', importer.anomalies, ['tipo', 'documento', 'detalle'])
    write_csv(folder / 'clientes-estimados.csv', importer.estimated, ['documento', 'fecha', 'codigo', 'cliente asignado', 'nif'])
    write_csv(folder / 'compras-y-gastos.csv', ({
        'fecha': row['fec'], 'proveedor': suppliers.get(row['prv'], row['prv']), 'categoria': categories.get(row['gst'], row['gst']),
        'concepto': rules.clean_text(row['nom']), 'base': row['bas'], 'tipo iva': row['tiv'], 'iva': row['iva'], 'pago': row['cob']}
        for row in sorted(backup.rows('apt'), key=lambda r: (r['fec'] or datetime.min, r['ind']))),
        ['fecha', 'proveedor', 'categoria', 'concepto', 'base', 'tipo iva', 'iva', 'pago'])
    write_csv(folder / 'proveedores.csv', ({
        'codigo': row['ind'], 'nombre': rules.clean_text(row['nom']), 'nif': rules.clean_nif(row['cif']), 'direccion': rules.clean_text(row['dir']),
        'cp': rules.clean_text(row['cop']), 'poblacion': rules.clean_text(row['pob']), 'provincia': rules.clean_text(row['pvn']),
        'telefono': rules.clean_text(row['tel']), 'fax': rules.clean_text(row['fax'])}
        for row in sorted(backup.rows('prv'), key=lambda r: r['ind'])))
    write_csv(folder / 'categorias-gasto.csv', ({'codigo': row['ind'], 'nombre': rules.clean_text(row['nom'])}
                                                for row in sorted(backup.rows('gst'), key=lambda r: r['ind'])))

    retained = []
    collected = []
    for ind, invoice in sorted(importer.invoices.items(), key=lambda kv: kv[1]['num']):
        amounts = importer.invoice_amounts[ind]
        if invoice.get('ret'):
            retained.append({
                'factura': importer._invoice_label(invoice), 'fecha': importer.invoice_date[ind], 'cliente': invoice['cli'],
                'nombre': importer.invoice_client[ind].name, 'base': amounts['subtotal'], 'total factura': amounts['total'],
                'retencion %': invoice['ret'], 'retencion': amounts['retention_amount'],
                'total a pagar': amounts['total'] - amounts['retention_amount']})
        if invoice.get('cob'):
            collected.append({'factura': importer._invoice_label(invoice), 'cobro': invoice['cob']})
    write_csv(folder / 'retenciones-irpf.csv', retained,
              ['factura', 'fecha', 'cliente', 'nombre', 'base', 'total factura', 'retencion %', 'retencion', 'total a pagar'])
    write_csv(folder / 'cobros.csv', collected, ['factura', 'cobro'])

    rows = []
    for code in sorted(importer.clients.identities):
        for identity in importer.clients.identities[code]:
            if identity.row_id is None:
                continue
            version = identity.latest
            missing = [label for label, value in (('NIF', version.nif), ('dirección', version.address),
                                                  ('código postal', version.postal_code), ('población', version.city)) if not value]
            rows.append({
                'codigo': code, 'nombre': version.name, 'nif': version.nif, 'estado': 'borrado (código reutilizado)' if identity.replaced_on else 'vivo',
                'iva': 'con IVA' if version.taxed else 'serie S', 'recargo': 'sí' if version.taxed and version.surcharged else 'no',
                'formato albaran': 'valorado' if version.valued else 'simple', 'falta para emitir': ', '.join(missing)})
    write_csv(folder / 'clientes.csv', rows)


def control_totals(out):
    db = sqlite3.connect(out)
    year = "strftime('%Y', date / 1000, 'unixepoch', 'localtime')"
    totals = db.execute(
        f'select series, {year}, count(*), round(sum(subtotal), 2), round(sum(vat_amount), 2),'
        f' round(sum(surcharge_amount), 2), round(sum(total), 2) from invoices group by 1, 2 order by 1, 2').fetchall()
    counts = {table: db.execute(f'select count(*) from {table}').fetchone()[0] for table in (
        'clients', 'articles', 'delivery_notes', 'delivery_notes_articles', 'invoices', 'invoice_lines')}
    next_numbers = db.execute('select series, max(code) + 1 from invoices group by series').fetchall()
    next_note = db.execute('select max(code) + 1 from delivery_notes').fetchone()[0]
    pending = db.execute('select count(*) from delivery_notes where invoice_id is null').fetchone()[0]
    dates = db.execute("select date(min(date) / 1000, 'unixepoch', 'localtime'), date(max(date) / 1000, 'unixepoch', 'localtime') from delivery_notes").fetchone()
    # The old reports rounded each figure from the unrounded sum, so the printed figures may not add up
    lines_differ = db.execute(
        'select count(*) from invoices i where abs((select sum(total) from invoice_lines l where l.invoice_id = i.id) - i.subtotal) > 0.001').fetchone()[0]
    total_differs = db.execute(
        'select count(*) from invoices where abs(subtotal + vat_amount + surcharge_amount - total) > 0.001').fetchone()[0]
    db.close()
    return totals, counts, next_numbers, next_note, pending, dates, lines_differ, total_differs


def write_report(importer, out, folder, title):
    totals, counts, next_numbers, next_note, pending, dates, lines_differ, total_differs = control_totals(out)
    stats = importer.stats
    issuer = importer._issuer()
    lines = [
        f'# {title}', '',
        f'Titular: {issuer["fiscal_name"]} ({issuer["nif"]}), {issuer["address"]}', '',
        'Copias usadas, de la más antigua a la vigente:', '',
        *[f'- {date:%d/%m/%Y}: `{backup.path}`' for date, backup in importer.snapshots], '',
        '## Contenido', '',
        '| Dato | Filas |', '|---|---:|',
        f'| Clientes vivos | {stats["clients_live"]} |',
        f'| Clientes anteriores de un código reutilizado (borrado lógico) | {stats["clients_replaced"]} |',
        f'| Artículos vivos | {stats["articles_live"]} |',
        f'| Artículos anteriores de un código reutilizado | {stats["articles_replaced"]} |',
        f'| Albaranes | {counts["delivery_notes"]} |',
        f'| Líneas de albarán | {counts["delivery_notes_articles"]} |',
        f'| Facturas emitidas | {counts["invoices"]} |',
        f'| Líneas de factura | {counts["invoice_lines"]} |',
        f'| Albaranes sin facturar | {pending} |', '',
        f'Fechas de albarán: {dates[0]} a {dates[1]} (incluye fechas mal tecleadas, ver `anomalias.csv`).', '',
        'Siguientes números: ' + ', '.join(f"{'S-' if s == 'WITHOUT_VAT' else ''}{n}" for s, n in next_numbers) + f'; albarán {next_note}.', '',
        '## Totales de control', '',
        '| Serie | Año | Facturas | Base | IVA | Recargo | Total |', '|---|---|---:|---:|---:|---:|---:|',
        *[f"| {'S' if s == 'WITHOUT_VAT' else 'General'} | {y} | {n} | {b:.2f} | {v:.2f} | {r:.2f} | {t:.2f} |" for s, y, n, b, v, r, t in totals], '',
        'Los importes son los que imprimía el programa antiguo, que redondeaba cada cifra desde la suma sin redondear. '
        f'Por eso, igual que en el papel, en {lines_differ} facturas las líneas no suman exactamente la base y en '
        f'{total_differs} la base más los impuestos difiere del total, por céntimos.', '',
        '## Limpieza aplicada', '',
        f'- Facturas fantasma descartadas (sin albaranes): {stats["phantom_invoices"]}',
        f'- Facturas con contador propio renumeradas: {stats["renumbered_invoices"]}',
        f'- Facturas cuyo cliente es una estimación (`clientes-estimados.csv`): {stats["estimated_invoices"]}',
        f'- Fechas de albarán inferidas: {stats["note_dates_inferred"]}; de factura: {stats["invoice_dates_inferred"]}',
        f'- Fechas que parecen mal tecleadas y se conservan: {stats["odd_note_dates"]} de albarán, {stats["odd_invoice_dates"]} de factura',
        f'- Albaranes de un cliente distinto al de su factura: {stats["notes_of_another_client"]}',
        f'- Líneas sin artículo descartadas: {stats["lines_without_article"]}',
        f'- Lotes: {stats["lots_cleaned"]} limpiados de teclas sueltas, {stats["lots_emptied"]} vaciados (solo basura), '
        f'{stats["lots_not_8_digits"]} conservados sin las 8 cifras actuales, {stats["lines_without_lot"]} líneas sin lote',
        f'- Líneas con cantidad negativa (devoluciones de envase, abonos): {stats["negative_quantity_lines"]}; cantidad cero: {stats["zero_quantity_lines"]}', '',
    ]
    (folder / 'informe.md').write_text('\n'.join(lines), encoding='utf-8')


def parse_snapshot(text):
    date, _, path = text.partition('=')
    return datetime.strptime(date, '%Y-%m-%d'), Backup(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--template', required=True, help='Empty SelfBill database, migrated by the API')
    parser.add_argument('--out', required=True, help='Database to create')
    parser.add_argument('--annexes', required=True, help='Folder for the report and the CSV files')
    parser.add_argument('--title', required=True, help='Title of the report')
    parser.add_argument('--logo', help='JPEG printed on the old documents')
    parser.add_argument('--snapshot', action='append', required=True, metavar='YYYY-MM-DD=PATH',
                        help='A backup of this titular with its date, oldest first; the last one is the data to import')
    args = parser.parse_args()

    out = Path(args.out)
    if out.exists():
        raise SystemExit(f'{out} already exists: this tool never overwrites a database')
    importer = Importer([parse_snapshot(text) for text in args.snapshot], args.logo)
    importer.run(args.template, out)
    folder = Path(args.annexes)
    write_annexes(importer, folder)
    write_report(importer, out, folder, args.title)
    print(f'{out}: ' + ', '.join(f'{k}={v}' for k, v in sorted(importer.stats.items())))


if __name__ == '__main__':
    main()
