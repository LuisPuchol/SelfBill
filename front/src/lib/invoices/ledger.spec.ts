import { describe, expect, it } from 'vitest';
import { defaultRange, periodTotals } from './ledger';

describe('defaultRange', () => {
	it.each([
		['2026-10-23', '2026-09-23'],
		['2026-03-30', '2026-02-28'],
		['2026-10-31', '2026-10-01'],
		['2026-01-15', '2025-12-16']
	])('%s starts 30 days earlier, on %s', (today, from) => {
		expect(defaultRange(today)).toEqual({ from, to: today });
	});
});

describe('periodTotals', () => {
	it('sums in cents', () => {
		const t = periodTotals([
			{ subtotal: 0.1, vatAmount: 0.1, surchargeAmount: 0.1, total: 0.1 },
			{ subtotal: 0.2, vatAmount: 0.2, surchargeAmount: 0.2, total: 0.2 }
		]);
		expect(t).toEqual({
			count: 2,
			subtotal: 0.3,
			vatAmount: 0.3,
			surchargeAmount: 0.3,
			total: 0.3
		});
	});
	it('is zero for none', () => {
		expect(periodTotals([]).total).toBe(0);
	});
});
