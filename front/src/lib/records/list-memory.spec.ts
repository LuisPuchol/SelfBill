import { describe, expect, it } from 'vitest';
import { recallList, rememberList } from './list-memory';

describe('list memory', () => {
	it('remembers the last list of each section', () => {
		expect(recallList('clients')).toBeUndefined();
		rememberList('clients', { href: '/clients?q=g', openedId: 3 });
		expect(recallList('clients')).toEqual({ href: '/clients?q=g', openedId: 3 });
		expect(recallList('articles')).toBeUndefined();
	});

	it('a newer memory replaces the older one', () => {
		rememberList('articles', { href: '/articles?page=2', openedId: 1 });
		rememberList('articles', { href: '/articles' });
		expect(recallList('articles')).toEqual({ href: '/articles' });
	});
});
