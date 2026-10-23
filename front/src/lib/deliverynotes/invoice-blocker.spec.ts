import { describe, expect, it } from 'vitest';
import { invoiceBlocker, missingDataText } from './invoice-blocker';

const full = { nif: 'B1', address: 'Calle 1', postalCode: '46001', city: 'Valencia' };
const ok = { client: full, hasProfile: true, inInvoice: false };

describe('invoiceBlocker', () => {
	it('is enabled when everything is there', () => {
		expect(invoiceBlocker(ok)).toEqual({ enabled: true, reason: null });
	});
	it('without client: greyed with no reason', () => {
		expect(invoiceBlocker({ ...ok, client: null })).toEqual({ enabled: false, reason: null });
	});
	it('names the missing client data', () => {
		const r = (client: object) => invoiceBlocker({ ...ok, client }).reason;
		expect(r({ ...full, nif: null, postalCode: ' ' })).toBe(
			'Faltan datos del cliente para facturar: NIF y código postal'
		);
		expect(r({ nif: '', address: null, postalCode: '1', city: undefined })).toBe(
			'Faltan datos del cliente para facturar: NIF, dirección y población'
		);
		expect(r({ ...full, city: '' })).toBe('Faltan datos del cliente para facturar: población');
	});
	it('without profile', () => {
		expect(invoiceBlocker({ ...ok, hasProfile: false })).toEqual({
			enabled: false,
			reason: 'Faltan los datos del titular (Ajustes)'
		});
	});
	it('already invoiced wins, then client data, then profile', () => {
		const bad = { client: { ...full, nif: null }, hasProfile: false };
		expect(invoiceBlocker({ ...bad, inInvoice: true }).reason).toBe('Ya está en una factura');
		expect(invoiceBlocker({ ...bad, inInvoice: false }).reason).toMatch(
			/^Faltan datos del cliente/
		);
	});
});

describe('missingDataText', () => {
	it('names the fields in order, whatever the input order', () => {
		expect(missingDataText(['postalCode', 'nif'])).toBe('NIF y código postal');
		expect(missingDataText(['nif', 'address', 'city'])).toBe('NIF, dirección y población');
		expect(missingDataText(['city'])).toBe('población');
		expect(missingDataText([])).toBe('');
		expect(missingDataText(['other', 'city'])).toBe('población');
	});
});
