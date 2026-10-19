import { expect, it } from 'vitest';
import { periodOn, ratesFor, totals } from './totals';

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
