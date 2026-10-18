import { describe, expect, it } from 'vitest';
import { setupNotice, taxSummary, type SetupStatus } from './status';

const tax = { id: 1, validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 };
const profile = { id: 1, fiscalName: 'Frutas Luis SL' };
const go = 'Pulsa 6 para ir a Ajustes.';

const none: SetupStatus = { tax: null, anyTax: false, profile: null };
const complete: SetupStatus = { tax, anyTax: true, profile };
const futureOnly: SetupStatus = { tax: null, anyTax: true, profile };

describe('setupNotice', () => {
	it('says nothing when everything is configured', () => {
		expect(setupNotice(complete)).toBeNull();
	});

	it('names what is missing', () => {
		expect(setupNotice(none)).toBe(`Falta configurar el IVA y los datos del titular. ${go}`);
		expect(setupNotice({ ...complete, tax: null, anyTax: false })).toBe(
			`Falta configurar el IVA. ${go}`
		);
		expect(setupNotice({ ...complete, profile: null })).toBe(`Faltan los datos del titular. ${go}`);
	});

	it('tells apart a period that only starts in the future', () => {
		expect(setupNotice(futureOnly)).toBe(`No hay IVA vigente hoy. ${go}`);
		expect(setupNotice({ ...futureOnly, profile: null })).toBe(
			`No hay IVA vigente hoy y faltan los datos del titular. ${go}`
		);
	});
});

describe('taxSummary', () => {
	it('shows the rates in force, the lack of a current period, or nothing configured', () => {
		expect(taxSummary(complete)).toBe('IVA 4 % · Recargo 0,5 %');
		expect(taxSummary(futureOnly)).toBe('No hay IVA vigente hoy');
		expect(taxSummary(none)).toBe('Sin configurar');
	});
});
