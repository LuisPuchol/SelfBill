import { describe, expect, it } from 'vitest';
import { formatMoney, lineTotalCents, totalCents } from './money';

describe('formatMoney', () => {
	it('formats euros with grouping and a non-breaking space', () => {
		expect(formatMoney(1234.56)).toBe('1.234,56\u00a0€');
		expect(formatMoney(84)).toBe('84,00\u00a0€');
	});
});

describe('lineTotalCents', () => {
	it('rounds half up in integer cents', () => {
		expect(lineTotalCents(10, 1.2)).toBe(1200);
		expect(lineTotalCents(0.33, 1.5)).toBe(50);
		expect(lineTotalCents(1.25, 0.99)).toBe(124);
		expect(lineTotalCents(3, 0.1)).toBe(30);
	});
});

describe('totalCents', () => {
	it('rounds each line before summing', () => {
		expect(
			totalCents([
				{ quantity: 0.33, price: 1.5 },
				{ quantity: 0.33, price: 1.5 }
			])
		).toBe(100);
	});
});
