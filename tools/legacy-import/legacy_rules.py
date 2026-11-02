"""Rules that turn values of the old program into SelfBill values. No I/O."""
import re
import struct
import unicodedata
from datetime import datetime
from decimal import ROUND_HALF_UP, Decimal

CENT = Decimal('0.01')

# The views hardcoded these until the program got its VAT table (iva) on 1-1-2023
OLD_VAT_PERCENTAGE = Decimal('4')
OLD_SURCHARGE_PERCENTAGE = Decimal('0.5')
OLD_VAT_RATE = .04
OLD_SURCHARGE_RATE = .005
VAT_TABLE_SINCE = datetime(2023, 1, 1)

# A client of type 3 is billed base only ("Base" in cb_cli_typ), which SelfBill calls the S series
TYPE_WITHOUT_VAT = 3
TYPE_GENERAL = 1

# A code silent for this long between two backups has probably passed to another client
MIN_SILENCE_DAYS = 60


def as_real(value):
    """The value a SQL Server `real` holds."""
    return struct.unpack('<f', struct.pack('<f', float(value)))[0]


def line_amount(quantity, price):
    """quantity * price as the old views computed it: real * money is real, with its rounding."""
    return as_real(as_real(quantity) * as_real(price))


def plain_sum(values):
    """SUM() of SQL Server. Python's sum() compensates float errors, and the old totals carry them."""
    total = 0.0
    for value in values:
        total += value
    return total


def printed(value):
    """kstr(x, n, 2) of the old reports: FoxPro rounds a double as its 15 significant digits show it."""
    return Decimal(format(value, '.15g')).quantize(CENT, ROUND_HALF_UP)


def vat_row(vat_table, date):
    """The row of the program's VAT table in force on the date (dsd and hst both inclusive), if any."""
    return next((row for row in vat_table if date and row['dsd'] <= date <= row['hst']), None)


def printed_rates(vat_table, date):
    """(VAT rate, surcharge rate, VAT %, surcharge %) the invoice of that date was printed with.

    Before 2023 the view multiplied by the literals .04 and .005. Since then it divides the real columns of the
    table by 100, which stays a real: 4 % is 0.0399999991. None when the table has no row for the date: the old
    program then printed no VAT and a total of zero.
    """
    if not vat_table or date is None or date < VAT_TABLE_SINCE:
        return OLD_VAT_RATE, OLD_SURCHARGE_RATE, OLD_VAT_PERCENTAGE, OLD_SURCHARGE_PERCENTAGE
    row = vat_row(vat_table, date)
    if row is None:
        return None
    return (as_real(as_real(row['ti']) / 100), as_real(as_real(row['re']) / 100),
            Decimal(str(row['ti'])).normalize(), Decimal(str(row['re'])).normalize())


def percentages_at(vat_table, date):
    """VAT and surcharge % in force on a date, as SelfBill freezes them in a delivery note."""
    row = vat_row(vat_table, date) if date else None
    if row is None and vat_table and date:
        row = max((r for r in vat_table if r['dsd'] <= date), key=lambda r: r['dsd'], default=None)
    if row is None:
        return OLD_VAT_PERCENTAGE, OLD_SURCHARGE_PERCENTAGE
    return Decimal(str(row['ti'])).normalize(), Decimal(str(row['re'])).normalize()


def invoice_amounts(note_sums, taxed, surcharged, retention, vat_rate=OLD_VAT_RATE, surcharge_rate=OLD_SURCHARGE_RATE):
    """The figures the old invoice printed, computed like the fahi view and the fah reports did."""
    base = plain_sum(note_sums)
    vat = base * vat_rate if taxed else 0.0
    surcharge = base * surcharge_rate if taxed and surcharged else 0.0
    return {
        'subtotal': printed(base),
        'vat_amount': printed(vat),
        'surcharge_amount': printed(surcharge),
        'total': printed(base + vat + surcharge),
        'retention_amount': printed(base * (retention or 0) / 100),
    }


def clean_text(raw):
    """Trims and collapses the padding of the old CHAR columns; a blank value is no value."""
    return ' '.join((raw or '').split()) or None


def clean_nif(raw):
    return re.sub(r'[^A-Z0-9]', '', (raw or '').upper()) or None


def issuer_nif(raw):
    return clean_nif(re.sub(r'^\s*NIF\s*:', '', raw or '', flags=re.IGNORECASE))


def catalog_nif(nif, taxed, ghost_nif):
    """The S series never asked for a NIF, and issuing needs one: those clients share a ghost one."""
    return nif or (None if taxed else ghost_nif)


def split_location(postal_code, city, province):
    """The three old columns were typed in any order: 'PALMA' in the postal code, the code in the province."""
    values = [v for v in map(clean_text, (postal_code, city, province)) if v]
    postal = next((v for v in values if re.fullmatch(r'\d{5}', v)), None)
    region = next((v for v in values if 'BALEAR' in v.upper()), None)
    rest = [v for v in values if v not in (postal, region)]
    return {'postal_code': postal, 'city': ' '.join(rest) or None, 'province': region}


def clean_lot(raw):
    """Lots are digits. Everything else is a stray key ('.', '+', 'ç'), and digits are never dropped."""
    return re.sub(r'\D', '', raw or '') or None


def lot_slots(*raws):
    """Lots in the first free slots. The first column is NOT NULL, so no lot at all leaves it empty."""
    lots = [lot for lot in map(clean_lot, raws) if lot]
    first = lots[0] if lots else ''
    return (first, *(lots[1:] + [None] * 2)[:2])


def search_text(*parts):
    """SearchText.normalize of the API: no accents, lower case."""
    joined = ' '.join(part for part in parts if part is not None)
    decomposed = unicodedata.normalize('NFD', joined)
    return ''.join(c for c in decomposed if not unicodedata.category(c).startswith('M')).lower().strip()


def comparable_name(name):
    return re.sub(r'[^A-Z0-9]', '', (name or '').upper())


def same_party(name_a, nif_a, name_b, nif_b):
    """Same client: same NIF, or the same name with the spelling normalized."""
    if nif_a and nif_a == nif_b:
        return True
    return comparable_name(name_a) == comparable_name(name_b)


def split_after_silence(dates, min_days=MIN_SILENCE_DAYS):
    """The first date after the longest silence between dates, if it is long enough; otherwise None."""
    ordered = sorted(dates)
    best = None
    best_gap = min_days - 1
    for earlier, later in zip(ordered, ordered[1:]):
        gap = (later - earlier).days
        if gap > best_gap:
            best, best_gap = later, gap
    return best
