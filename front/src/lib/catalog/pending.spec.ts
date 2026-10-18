import { describe, expect, it } from 'vitest';
import { articlePendingMessage, clientPendingMessage, count, regimeChangeMessage } from './pending';

describe('count', () => {
	it('uses the singular for one and the plural otherwise', () => {
		expect(count(1, 'albarán', 'albaranes')).toBe('1 albarán');
		expect(count(3, 'albarán', 'albaranes')).toBe('3 albaranes');
	});
});

describe('articlePendingMessage', () => {
	it('is null when no delivery note is waiting', () => {
		expect(articlePendingMessage(0)).toBeNull();
	});

	it('warns about the uninvoiced delivery notes', () => {
		expect(articlePendingMessage(1)).toBe(
			'Está en 1 albarán sin facturar. Si lo borras no se podrán volver a guardar.'
		);
		expect(articlePendingMessage(3)).toBe(
			'Está en 3 albaranes sin facturar. Si lo borras no se podrán volver a guardar.'
		);
	});
});

describe('clientPendingMessage', () => {
	it('is null when nothing is pending', () => {
		expect(clientPendingMessage(0, 0)).toBeNull();
	});

	it('names the delivery notes and the draft invoices', () => {
		expect(clientPendingMessage(5, 1)).toBe(
			'Tiene 5 albaranes sin facturar y 1 factura en borrador. Si lo borras no se podrán editar ni facturar.'
		);
		expect(clientPendingMessage(1, 0)).toBe(
			'Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.'
		);
		expect(clientPendingMessage(0, 2)).toBe(
			'Tiene 2 facturas en borrador. Si lo borras no se podrán editar ni facturar.'
		);
	});
});

describe('regimeChangeMessage', () => {
	it('says what will be recalculated', () => {
		expect(regimeChangeMessage(5, 1)).toBe(
			'Cambiar el IVA o el recargo recalculará 5 albaranes sin facturar y 1 factura en borrador de este cliente.'
		);
		expect(regimeChangeMessage(1, 0)).toBe(
			'Cambiar el IVA o el recargo recalculará 1 albarán sin facturar de este cliente.'
		);
		expect(regimeChangeMessage(0, 0)).toBeNull();
	});
});
