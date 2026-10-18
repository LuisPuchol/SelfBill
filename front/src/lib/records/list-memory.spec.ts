import { describe, expect, it } from 'vitest';
import { forgetOpened, recallList, rememberList, rememberListUrl } from './list-memory';

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

	it('a new URL keeps the last record opened', () => {
		rememberListUrl('invoices', '/invoices');
		expect(recallList('invoices')).toEqual({ href: '/invoices' });
		rememberList('invoices', { href: '/invoices?q=g', openedId: 4 });
		rememberListUrl('invoices', '/invoices?page=1');
		expect(recallList('invoices')).toEqual({ href: '/invoices?page=1', openedId: 4 });
	});

	it('forgetOpened drops only that record', () => {
		rememberList('articles', { href: '/articles?q=x', openedId: 7 });
		forgetOpened('articles', 8);
		expect(recallList('articles')).toEqual({ href: '/articles?q=x', openedId: 7 });
		forgetOpened('articles', 7);
		expect(recallList('articles')).toEqual({ href: '/articles?q=x', openedId: undefined });
	});
});
