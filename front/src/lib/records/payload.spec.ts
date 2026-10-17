import { describe, expect, it } from 'vitest';
import { trimOrNull } from './payload';

describe('trimOrNull', () => {
	it('trims, and an empty text travels as null', () => {
		expect(trimOrNull('  a ')).toBe('a');
		expect(trimOrNull('   ')).toBeNull();
		expect(trimOrNull('')).toBeNull();
		expect(trimOrNull(null)).toBeNull();
		expect(trimOrNull(undefined)).toBeNull();
	});
});
