import { describe, expect, it } from 'vitest';
import {
	blockNotice,
	blockReason,
	issuedSummary,
	monthLong,
	monthShort,
	type PendingInvoice
} from './billing';

const row = (overrides: Partial<PendingInvoice> = {}): PendingInvoice => ({
	clientId: 7,
	clientCode: 5,
	clientName: 'Frutería García',
	month: '2026-10',
	clientDeleted: false,
	missingFiscalData: [],
	deliveryNotes: [],
	...overrides
});

describe('blockNotice and blockReason', () => {
	it('say nothing for a row that can be issued', () => {
		expect(blockNotice(row())).toBe('');
		expect(blockReason(row())).toBeNull();
	});

	it('name the missing fiscal data and where to fix it', () => {
		const missing = row({ missingFiscalData: ['nif', 'postalCode'] });
		expect(blockNotice(missing)).toBe('Faltan datos');
		expect(blockReason(missing)).toBe(
			'Faltan datos del cliente para facturar: NIF y código postal (se corrigen en Clientes)'
		);
	});

	it('say the client is deleted, even if data is missing too', () => {
		const deleted = row({ clientDeleted: true, missingFiscalData: ['nif'] });
		expect(blockNotice(deleted)).toBe('Cliente borrado');
		expect(blockReason(deleted)).toBe('El cliente está borrado');
	});
});

describe('monthShort and monthLong', () => {
	it('write the month as in the table and as in a title', () => {
		expect(monthShort('2026-10')).toBe('10/2026');
		expect(monthLong('2026-10')).toBe('octubre 2026');
		expect(monthShort('2027-01')).toBe('01/2027');
		expect(monthLong('2027-01')).toBe('enero 2027');
	});
});

describe('issuedSummary', () => {
	const range = (from: number, to: number, prefix = '') =>
		Array.from({ length: to - from + 1 }, (_, i) => `${prefix}${from + i}`);

	it('names a single invoice', () => {
		expect(issuedSummary(['101'])).toBe('Factura 101 emitida');
		expect(issuedSummary(['S-3'])).toBe('Factura S-3 emitida');
	});

	it('gives the first and the last number of a series', () => {
		expect(issuedSummary(range(101, 127))).toBe('27 facturas emitidas: 101 a 127');
	});

	it('gives each series, the general one first', () => {
		const numbers = [...range(5, 9, 'S-'), ...range(101, 120)];
		expect(issuedSummary(numbers)).toBe('25 facturas emitidas: 101 a 120, S-5 a S-9');
	});

	it('orders by number, not by text', () => {
		expect(issuedSummary(['100', '99', '101'])).toBe('3 facturas emitidas: 99 a 101');
	});

	it('shows a series with one number alone', () => {
		expect(issuedSummary(['101', '102', 'S-5'])).toBe('3 facturas emitidas: 101 a 102, S-5');
	});

	it('says when none was issued', () => {
		expect(issuedSummary([])).toBe('No se ha emitido ninguna factura');
	});
});
