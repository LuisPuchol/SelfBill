import { describe, expect, it } from 'vitest';
import { noteRates, periodOn, ratesFor, totals } from './totals';

it('sums rounded lines and applies VAT and surcharge half up', () => {
	const lines = [
		{ quantity: 25, price: 1.2 },
		{ quantity: 3, price: 0.33 }
	];
	expect(totals(lines, { vat: 4, surcharge: 0.5 })).toEqual({
		baseCents: 3099,
		vatCents: 124,
		surchargeCents: 15,
		totalCents: 3238
	});
});

it('resolves rates with the client regime', () => {
	const period = { vatPercentage: 4, surchargePercentage: 0.5 };
	const full = { vatType: 'WITH_VAT', surchargeType: 'WITH_SURCHARGE' } as const;
	expect(ratesFor(period, full)).toEqual({ vat: 4, surcharge: 0.5 });
	expect(ratesFor(period, { ...full, vatType: 'WITHOUT_VAT' })?.vat).toBe(0);
	expect(ratesFor(period, { ...full, surchargeType: 'WITHOUT_SURCHARGE' })?.surcharge).toBe(0);
	expect(ratesFor(null, full)).toBeNull();
});

it('picks the latest period started on the date', () => {
	const periods = [
		{ validFrom: '2023-01-01' },
		{ validFrom: '2026-01-01' },
		{ validFrom: '2027-01-01' }
	];
	expect(periodOn('2026-10-18', periods)?.validFrom).toBe('2026-01-01');
	expect(periodOn('2022-12-31', periods)).toBeNull();
});

describe('noteRates', () => {
	const period = { vatPercentage: 4, surchargePercentage: 0.5 };
	const full = { vatType: 'WITH_VAT', surchargeType: 'WITH_SURCHARGE' } as const;
	const noSurcharge = { ...full, surchargeType: 'WITHOUT_SURCHARGE' } as const;
	/** Saved with García's full regime, at rates of an older period. */
	const saved = { regime: full, rates: { vat: 10, surcharge: 1.4 } };

	it('a new note takes both rates from the period, which it needs', () => {
		expect(noteRates(full, period)).toEqual({
			rates: { vat: 4, surcharge: 0.5 },
			needsPeriod: true
		});
		expect(noteRates(full, null)).toEqual({ rates: null, needsPeriod: true });
	});

	it('a saved note keeps its rates for a client with its regime, with or without a period', () => {
		expect(noteRates(full, period, saved)).toEqual({ rates: saved.rates, needsPeriod: false });
		expect(noteRates(full, null, saved)).toEqual({ rates: saved.rates, needsPeriod: false });
	});

	it('only the component whose regime changed takes the period, or 0 without it', () => {
		expect(noteRates(noSurcharge, period, saved)).toEqual({
			rates: { vat: 10, surcharge: 0 },
			needsPeriod: true
		});
		const back = { regime: noSurcharge, rates: { vat: 10, surcharge: 0 } };
		expect(noteRates(full, period, back)).toEqual({
			rates: { vat: 10, surcharge: 0.5 },
			needsPeriod: true
		});
		// As the API: a regime change looks the period up, and fails without one.
		expect(noteRates(noSurcharge, null, saved)).toEqual({ rates: null, needsPeriod: true });
	});

	it("without the note's regime (its client deleted since) both rates are reapplied", () => {
		expect(noteRates(full, period, { ...saved, regime: null })).toEqual({
			rates: { vat: 4, surcharge: 0.5 },
			needsPeriod: true
		});
	});
});
