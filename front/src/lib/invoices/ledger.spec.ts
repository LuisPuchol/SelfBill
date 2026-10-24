import { describe, expect, it } from 'vitest';
import { periodTotals } from './ledger';

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
