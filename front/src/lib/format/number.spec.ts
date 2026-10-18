import { describe, expect, it } from 'vitest';
import { formatDecimal, formatPercent, parseDecimal } from './number';

describe('parseDecimal', () => {
	it('accepts comma or dot as decimal separator', () => {
		expect(parseDecimal('10,5')).toBe(10.5);
		expect(parseDecimal('10.5')).toBe(10.5);
		expect(parseDecimal(' 7,25 ')).toBe(7.25);
		expect(parseDecimal('10')).toBe(10);
	});

	it('rejects invalid input', () => {
		for (const bad of ['', 'abc', '1,2,3', '1.234,56', '-3']) expect(parseDecimal(bad)).toBeNull();
	});
});

describe('formatDecimal', () => {
	it('formats es-ES without thousands separator', () => {
		expect(formatDecimal(10)).toBe('10,00');
		expect(formatDecimal(1234.5)).toBe('1234,50');
		expect(formatDecimal(3, 0)).toBe('3');
	});
});

describe('formatPercent', () => {
	it('trims zeros, uses a comma and a non-breaking space', () => {
		expect(formatPercent(4)).toBe('4 %');
		expect(formatPercent(0.5)).toBe('0,5 %');
		expect(formatPercent(5.25)).toBe('5,25 %');
		expect(formatPercent(21)).toBe('21 %');
		expect(formatPercent(0)).toBe('0 %');
	});
});
