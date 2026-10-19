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
