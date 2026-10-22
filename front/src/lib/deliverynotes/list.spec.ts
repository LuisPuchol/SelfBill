import { describe, expect, it } from 'vitest';
import { emptyHint, stateText } from './list';

describe('emptyHint', () => {
	it('invites to make the first note when the list shows them all', () => {
		expect(emptyHint(false)).toBe('Todavía no hay albaranes. Pulsa F2 para hacer el primero.');
	});

	it('says there is none to invoice when only the pending ones are shown', () => {
		expect(emptyHint(true)).toBe('No hay albaranes pendientes de facturar.');
	});
});

describe('stateText', () => {
	it('says pending when the note is in no invoice', () => {
		expect(stateText({ invoiceStatus: null, invoiceNumber: null })).toBe('Pendiente');
		expect(stateText({})).toBe('Pendiente');
	});

	it('says draft invoice while the invoice is a draft', () => {
		expect(stateText({ invoiceStatus: 'DRAFT', invoiceNumber: null })).toBe('En factura borrador');
	});

	it('gives the invoice number once issued', () => {
		expect(stateText({ invoiceStatus: 'ISSUED', invoiceNumber: '23' })).toBe('Facturado nº 23');
	});
});
