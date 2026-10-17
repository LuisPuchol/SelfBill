import { describe, expect, it } from 'vitest';
import { withParams } from './url';

describe('withParams', () => {
	const u = new URL('http://x/clients?page=2&q=pe');

	it('sets a parameter and keeps the rest', () => {
		expect(withParams(u, { page: 3 })).toBe('/clients?page=3&q=pe');
	});

	it('removes a parameter with null', () => {
		expect(withParams(u, { q: null })).toBe('/clients?page=2');
	});

	it('keeps page 0 and does not touch the original url', () => {
		expect(withParams(new URL('http://x/clients'), { page: 0 })).toBe('/clients?page=0');
		expect(u.search).toBe('?page=2&q=pe');
	});
});
