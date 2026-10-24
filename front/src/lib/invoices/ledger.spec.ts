import { describe, expect, it } from 'vitest';
import { defaultRange, periodTotals } from './ledger';

describe('defaultRange', () => {
	it('goes back a month', () => {
		expect(defaultRange('2026-10-23')).toEqual({ from: '2026-09-23', to: '2026-10-23' });
	});
	it('clamps to the last day of the shorter month', () => {
		expect(defaultRange('2026-03-30').from).toBe('2026-02-28');
		expect(defaultRange('2026-10-31').from).toBe('2026-09-30');
	});
	it('crosses the year', () => {
		expect(defaultRange('2026-01-15').from).toBe('2025-12-15');
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
