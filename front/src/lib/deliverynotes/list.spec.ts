import { describe, expect, it } from 'vitest';
import { stateText } from './list';

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
