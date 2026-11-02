"""Run with: python -m unittest discover tools/legacy-import"""
import struct
import unittest
from datetime import datetime
from decimal import Decimal

import legacy_rules as rules
import sql2000
import sqlserver


class AmountsTest(unittest.TestCase):

    def test_a_line_keeps_the_rounding_of_real_arithmetic(self):
        # Value given by SQL Server for CAST(.05 AS real) * CAST(3.5 AS money). Exact arithmetic would print 0.18
        self.assertEqual(rules.line_amount(0.05, Decimal('3.5')), 0.17499999701976776)
        self.assertEqual(rules.printed(rules.line_amount(0.05, Decimal('3.5'))), Decimal('0.17'))

    def test_a_tie_that_survives_real_arithmetic_rounds_up(self):
        self.assertEqual(rules.printed(rules.line_amount(2.5, Decimal('0.25'))), Decimal('0.63'))

    def test_a_negative_tie_rounds_away_from_zero(self):
        self.assertEqual(rules.printed(rules.line_amount(-2.5, Decimal('0.25'))), Decimal('-0.63'))

    def test_a_quantity_is_printed_with_its_two_decimals(self):
        self.assertEqual(rules.printed(rules.as_real(7.2)), Decimal('7.20'))

    def test_the_total_rounds_the_unrounded_sum_not_the_rounded_parts(self):
        amounts = rules.invoice_amounts([100.125], taxed=True, surcharged=False, retention=0)
        self.assertEqual(amounts['subtotal'], Decimal('100.13'))
        self.assertEqual(amounts['vat_amount'], Decimal('4.01'))
        self.assertEqual(amounts['total'], Decimal('104.13'))

    def test_an_untaxed_invoice_has_no_vat_nor_surcharge_even_if_its_client_has_surcharge(self):
        amounts = rules.invoice_amounts([50.0], taxed=False, surcharged=True, retention=0)
        self.assertEqual(amounts['vat_amount'], Decimal('0.00'))
        self.assertEqual(amounts['surcharge_amount'], Decimal('0.00'))
        self.assertEqual(amounts['total'], Decimal('50.00'))

    def test_surcharge_and_retention(self):
        amounts = rules.invoice_amounts([200.0], taxed=True, surcharged=True, retention=1)
        self.assertEqual(amounts['surcharge_amount'], Decimal('1.00'))
        self.assertEqual(amounts['total'], Decimal('209.00'))
        self.assertEqual(amounts['retention_amount'], Decimal('2.00'))

    def test_before_the_vat_table_the_literals_print(self):
        table = [{'dsd': datetime(2000, 1, 1), 'hst': datetime(2022, 12, 31), 'ti': 4.0, 're': 0.5}]
        self.assertEqual(rules.printed_rates(table, datetime(2015, 3, 31)), (.04, .005, Decimal('4'), Decimal('0.5')))

    def test_since_the_vat_table_its_rate_is_a_real(self):
        table = [{'dsd': datetime(2025, 1, 1), 'hst': datetime(2025, 12, 31), 'ti': 4.0, 're': 0.0}]
        vat, surcharge, vat_percentage, surcharge_percentage = rules.printed_rates(table, datetime(2025, 12, 31))
        # Value given by SQL Server for CAST(4.0 AS real) / 100
        self.assertEqual(vat, 0.039999999105930328)
        self.assertEqual((surcharge, vat_percentage, surcharge_percentage), (0.0, Decimal('4'), Decimal('0')))

    def test_the_real_rate_can_print_a_cent_less_than_the_literal(self):
        literal = rules.invoice_amounts([100.125], True, False, 0)
        real = rules.invoice_amounts([100.125], True, False, 0, vat_rate=rules.as_real(rules.as_real(4.0) / 100))
        self.assertEqual((literal['vat_amount'], real['vat_amount']), (Decimal('4.01'), Decimal('4.00')))

    def test_no_row_for_the_date_prints_no_rates(self):
        table = [{'dsd': datetime(2025, 1, 1), 'hst': datetime(2025, 12, 31), 'ti': 4.0, 're': 0.0}]
        self.assertIsNone(rules.printed_rates(table, datetime(2027, 1, 31)))

    def test_a_note_takes_the_percentages_in_force_on_its_date(self):
        table = [{'dsd': datetime(2000, 1, 1), 'hst': datetime(2022, 12, 31), 'ti': 4.0, 're': 0.5},
                 {'dsd': datetime(2024, 10, 1), 'hst': datetime(2024, 12, 31), 'ti': 2.0, 're': 0.0}]
        self.assertEqual(rules.percentages_at(table, datetime(2024, 11, 5)), (Decimal('2'), Decimal('0')))
        self.assertEqual(rules.percentages_at(table, datetime(2030, 1, 1)), (Decimal('2'), Decimal('0')))
        self.assertEqual(rules.percentages_at([], datetime(2030, 1, 1)), (Decimal('4'), Decimal('0.5')))

    def test_a_missing_retention_is_none(self):
        self.assertEqual(rules.invoice_amounts([200.0], True, False, None)['retention_amount'], Decimal('0.00'))


class TextTest(unittest.TestCase):

    def test_padding_is_collapsed_and_blank_is_none(self):
        self.assertEqual(rules.clean_text('  SOME   SHOP  8   '), 'SOME SHOP 8')
        self.assertIsNone(rules.clean_text('    '))
        self.assertIsNone(rules.clean_text(None))

    def test_nif_keeps_only_letters_and_digits(self):
        self.assertEqual(rules.clean_nif('b/07 000-000'), 'B07000000')
        self.assertIsNone(rules.clean_nif('   '))

    def test_issuer_nif_drops_the_printed_label(self):
        self.assertEqual(rules.issuer_nif('NIF : 12345678 Z'), '12345678Z')

    def test_a_client_of_the_s_series_without_nif_gets_the_ghost_one(self):
        self.assertEqual(rules.catalog_nif(None, taxed=False, ghost_nif='12345678Z'), '12345678Z')

    def test_a_real_nif_is_never_replaced(self):
        self.assertEqual(rules.catalog_nif('B07000000', taxed=False, ghost_nif='12345678Z'), 'B07000000')

    def test_a_client_with_vat_without_nif_stays_without(self):
        self.assertIsNone(rules.catalog_nif(None, taxed=True, ghost_nif='12345678Z'))

    def test_without_a_ghost_nif_nothing_changes(self):
        self.assertIsNone(rules.catalog_nif(None, taxed=False, ghost_nif=None))

    def test_search_text_matches_the_api(self):
        self.assertEqual(rules.search_text('PEÑA', None, 'Calçots'), 'pena calcots')


class LocationTest(unittest.TestCase):

    def test_columns_in_order(self):
        self.assertEqual(rules.split_location('07007', 'PALMA', 'ILLES BALEARS'),
                         {'postal_code': '07007', 'city': 'PALMA', 'province': 'ILLES BALEARS'})

    def test_the_town_typed_in_the_postal_code_column(self):
        self.assertEqual(rules.split_location('PALMA', 'ILLES BALEARS', '07007'),
                         {'postal_code': '07007', 'city': 'PALMA', 'province': 'ILLES BALEARS'})

    def test_province_and_town_swapped(self):
        self.assertEqual(rules.split_location('07610', 'ILLES  BALEARS', 'PALMA'),
                         {'postal_code': '07610', 'city': 'PALMA', 'province': 'ILLES BALEARS'})

    def test_blanks(self):
        self.assertEqual(rules.split_location('', '', ''), {'postal_code': None, 'city': None, 'province': None})

    def test_a_short_postal_code_is_not_lost(self):
        self.assertEqual(rules.split_location('0700', 'PALMA', '')['city'], '0700 PALMA')


class LotTest(unittest.TestCase):

    def test_stray_keys_are_removed_and_digits_kept(self):
        self.assertEqual(rules.clean_lot('+02160511'), '02160511')
        self.assertEqual(rules.clean_lot('02140+611'), '02140611')
        self.assertEqual(rules.clean_lot('ç010804'), '010804')

    def test_only_stray_keys_is_no_lot(self):
        for raw in ('.', '´', '+', '..', 'sp', '', None):
            self.assertIsNone(rules.clean_lot(raw))

    def test_slots_are_squeezed_with_the_first_never_null(self):
        self.assertEqual(rules.lot_slots('.', '02160511', ''), ('02160511', None, None))
        self.assertEqual(rules.lot_slots('', '', ''), ('', None, None))
        self.assertEqual(rules.lot_slots('1', '2', '3'), ('1', '2', '3'))


class IdentityTest(unittest.TestCase):

    def test_same_nif_different_name(self):
        self.assertTrue(rules.same_party('OLD NAME', 'B1', 'NEW NAME', 'B1'))

    def test_same_name_after_a_nif_correction(self):
        self.assertTrue(rules.same_party('A SCHOOL', 'Q1', 'A  SCHOOL', 'R1'))

    def test_spelling_of_the_name_without_nif(self):
        self.assertTrue(rules.same_party('BOBS', None, "BOB'S", None))

    def test_a_reused_code(self):
        self.assertFalse(rules.same_party('A SHOP', 'B1', 'ANOTHER SHOP', 'B2'))

    def test_two_clients_without_nif(self):
        self.assertFalse(rules.same_party('A BAR', None, 'A CAFE', None))


class SilenceTest(unittest.TestCase):

    def test_the_longest_silence_splits(self):
        dates = [datetime(2007, 1, 1), datetime(2007, 2, 1), datetime(2010, 5, 1), datetime(2010, 6, 1)]
        self.assertEqual(rules.split_after_silence(dates), datetime(2010, 5, 1))

    def test_a_steady_client_does_not_split(self):
        dates = [datetime(2011, 7, 1), datetime(2011, 8, 1), datetime(2011, 9, 1)]
        self.assertIsNone(rules.split_after_silence(dates))

    def test_too_few_dates(self):
        self.assertIsNone(rules.split_after_silence([datetime(2011, 7, 1)]))
        self.assertIsNone(rules.split_after_silence([]))


class DecodeTest(unittest.TestCase):

    def test_money_is_a_little_endian_int64_of_ten_thousandths(self):
        column = {'xtype': 60}
        self.assertEqual(sql2000.decode_value(column, struct.pack('<q', 21000)), Decimal('2.1'))
        self.assertEqual(sql2000.decode_value(column, struct.pack('<q', -270000)), Decimal('-27'))

    def test_datetime_is_ticks_then_days(self):
        column = {'xtype': 61}
        self.assertEqual(sql2000.decode_value(column, struct.pack('<ii', 0, 40000)), datetime(2009, 7, 8))

    def test_char_is_ansi(self):
        self.assertEqual(sql2000.decode_value({'xtype': 175}, 'CALÇOTS'.encode('cp1252')), 'CALÇOTS')

    def test_null_stays_null(self):
        self.assertIsNone(sql2000.decode_value({'xtype': 56}, None))

    def test_a_record_with_fixed_variable_and_null_columns(self):
        columns = [
            {'name': 'id', 'xtype': 56, 'length': 4, 'colid': 1, 'xoffset': 4, 'scale': 0, 'bitpos': 0},
            {'name': 'gone', 'xtype': 56, 'length': 4, 'colid': 2, 'xoffset': 8, 'scale': 0, 'bitpos': 0},
            {'name': 'name', 'xtype': 231, 'length': 20, 'colid': 3, 'xoffset': -1, 'scale': 0, 'bitpos': 0},
        ]
        name = 'abc'.encode('utf-16-le')
        fixed_end = 12
        header_after_fixed = struct.pack('<H', 3) + bytes([0b010]) + struct.pack('<H', 1)
        variable_start = fixed_end + len(header_after_fixed) + 2
        record = (bytes([sql2000.STATUS_NULL_BITMAP | sql2000.STATUS_VARIABLE, 0])
                  + struct.pack('<H', fixed_end) + struct.pack('<ii', 7, 0)
                  + header_after_fixed + struct.pack('<H', variable_start + len(name)) + name)
        self.assertEqual(sql2000._decode_record(record, 0, columns), {'id': 7, 'gone': None, 'name': 'abc'})


class RestoredValuesTest(unittest.TestCase):
    """Values as LocalDB exports them must equal what the page reader decodes from the same bytes."""

    def test_a_real_comes_back_as_the_same_real(self):
        # Text given by SQL Server for CONVERT(varchar(40), CAST(3.3 AS real), 3)
        self.assertEqual(sqlserver._imported('3.2999999523162842e+000', 'real'), struct.unpack('<f', struct.pack('<f', 3.3))[0])

    def test_money_keeps_its_four_decimals(self):
        self.assertEqual(sqlserver._imported('1.2500', 'money'), sql2000.decode_value({'xtype': 60}, struct.pack('<q', 12500)))

    def test_a_date(self):
        self.assertEqual(sqlserver._imported('2026-10-28T00:00:00', 'datetime'), datetime(2026, 10, 28))

    def test_null_and_text_pass_through(self):
        self.assertIsNone(sqlserver._imported(None, 'real'))
        self.assertEqual(sqlserver._imported('PATATAS   ', 'char'), 'PATATAS   ')

    def test_floats_money_and_dates_are_exported_as_text_with_all_their_digits(self):
        self.assertEqual(sqlserver._exported('ctd', 'real'), 'CONVERT(varchar(40), [ctd], 3) AS [ctd]')
        self.assertEqual(sqlserver._exported('prv', 'money'), 'CONVERT(varchar(40), [prv], 2) AS [prv]')
        self.assertEqual(sqlserver._exported('fec', 'datetime'), 'CONVERT(varchar(23), [fec], 126) AS [fec]')
        self.assertEqual(sqlserver._exported('ind', 'int'), '[ind]')


if __name__ == '__main__':
    unittest.main()
