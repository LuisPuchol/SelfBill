import { describe, expect, it } from 'vitest';
import { traceabilityReport } from './report';

const input = {
	code: '02200524',
	articleName: 'ESPINACA BOLSA 250GR',
	from: '2026-09-24',
	to: '2026-10-24',
	rows: [
		{
			date: '2026-05-20',
			clientCode: 6,
			clientName: 'Frutas Paco',
			deliveryNoteCode: 31594,
			quantity: 20
		},
		{
			date: '2026-06-01',
			clientCode: 7,
			clientName: 'Bar Pepe',
			deliveryNoteCode: 31600,
			quantity: 131
		}
	]
};

describe('traceabilityReport', () => {
	it('builds the listing', () => {
		const html = traceabilityReport(input);
		expect(html).toContain('<!doctype html>');
		expect(html).toContain('<title>Trazabilidad 02200524</title>');
		expect(html).toContain('Trazabilidad 02200524 ESPINACA BOLSA 250GR');
		expect(html).toContain('Total cantidad 151,00');
		expect(html).toContain('Del 24/09/2026 al 24/10/2026');
		expect(html.match(/<tr>/g)?.length).toBe(3); // header + 2 rows
		for (const text of ['20/05/2026', '6', 'Frutas Paco', '31594', '20,00'])
			expect(html).toContain(text);
	});

	it('escapes names', () => {
		const html = traceabilityReport({
			...input,
			articleName: '<i>X</i>',
			rows: [{ ...input.rows[0], clientName: '<b>A&B</b>' }]
		});
		expect(html).toContain('&lt;b&gt;A&amp;B&lt;/b&gt;');
		expect(html).toContain('&lt;i&gt;X&lt;/i&gt;');
		expect(html).not.toContain('<b>');
		expect(html).not.toContain('<i>');
	});

	it('zero rows', () => {
		expect(traceabilityReport({ ...input, rows: [] })).toContain('Total cantidad 0,00');
	});
});
