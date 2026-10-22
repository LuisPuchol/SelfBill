import { expect, it, vi } from 'vitest';
import type { api } from '../api/client';
import { createLookups } from './lookups';

it('answers a seeded client and article from the cache, without asking the API', async () => {
	const GET = vi.fn();
	const lookups = createLookups({ GET } as unknown as typeof api);
	lookups.seed({
		clients: [{ id: 7, code: 3, name: 'Frutería García' }],
		articles: [{ id: 40, code: 4, name: 'Tomate' }]
	});
	await expect(lookups.client(3)).resolves.toMatchObject({ id: 7 });
	await expect(lookups.article(4)).resolves.toMatchObject({ id: 40 });
	expect(lookups.cachedArticle(4)?.name).toBe('Tomate');
	expect(GET).not.toHaveBeenCalled();
});

const notFound = () => ({ error: {}, response: new Response(null, { status: 404 }) });
const found = (data: object) => ({ data, response: new Response(null, { status: 200 }) });

it.each([
	['client', 'cachedClient'],
	['article', 'cachedArticle']
] as const)(
	'keeps only what it found: a %s created meanwhile is found on the next try',
	async (lookup, cachedLookup) => {
		const GET = vi
			.fn()
			.mockResolvedValueOnce(notFound())
			.mockResolvedValueOnce(found({ id: 41, code: 9, name: 'Calabacín' }));
		const lookups = createLookups({ GET } as unknown as typeof api);
		await expect(lookups[lookup](9)).resolves.toBeNull();
		expect(lookups[cachedLookup](9)).toBeNull();
		await expect(lookups[lookup](9)).resolves.toMatchObject({ id: 41 });
		expect(lookups[cachedLookup](9)?.name).toBe('Calabacín');
		// Found: kept, never asked for again.
		await expect(lookups[lookup](9)).resolves.toMatchObject({ id: 41 });
		expect(GET).toHaveBeenCalledTimes(2);
	}
);
