import { describe, expect, it } from 'vitest';
import { formatDate, parseDateDigits, timeNow, todayIso } from './date';

describe('formatDate', () => {
	it('prints dd/mm/yyyy from date or datetime', () => {
		expect(formatDate('2026-10-12')).toBe('12/10/2026');
		expect(formatDate('2026-10-12T08:00:00')).toBe('12/10/2026');
	});
});

describe('parseDateDigits', () => {
	it('parses ddmmyy, ddmmyyyy and ddmm', () => {
		expect(parseDateDigits('121026', '2026-10-16')).toBe('2026-10-12');
		expect(parseDateDigits('12102026', '2026-10-16')).toBe('2026-10-12');
		expect(parseDateDigits('12/10/26', '2026-10-16')).toBe('2026-10-12');
		expect(parseDateDigits('1210', '2026-10-16')).toBe('2026-10-12');
		expect(parseDateDigits('290228', '2026-10-16')).toBe('2028-02-29');
	});

	it('rejects invalid dates and lengths', () => {
		for (const bad of ['310226', '12345', ''])
			expect(parseDateDigits(bad, '2026-10-16')).toBeNull();
	});
});

describe('todayIso', () => {
	it('uses the local day, not UTC', () => {
		expect(todayIso(new Date(2026, 9, 13, 0, 30))).toBe('2026-10-13');
	});
});

describe('timeNow', () => {
	it('gives the local time as hh:mm:ss', () => {
		expect(timeNow(new Date(2026, 9, 13, 7, 5, 9))).toBe('07:05:09');
	});
});
