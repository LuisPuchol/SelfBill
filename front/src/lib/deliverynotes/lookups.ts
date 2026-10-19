import type { components } from '../api/schema';
import type { api as Api } from '../api/client';
import { request } from '../api/client';
import { ApiError } from '../api/errors';

type ClientResponse = components['schemas']['ClientResponse'];
type ArticleResponse = components['schemas']['ArticleResponse'];

async function orNull<T>(call: Promise<T>): Promise<T | null> {
	try {
		return await call;
	} catch (error) {
		if (error instanceof ApiError && error.status === 404) return null;
		throw error;
	}
}

/** Client and article lookups by code, cached per code (misses too) for the life of the editor. */
export function createLookups(client: typeof Api) {
	const clients = new Map<number, ClientResponse | null>();
	const articles = new Map<number, ArticleResponse | null>();

	async function cached<T>(
		cache: Map<number, T | null>,
		code: number,
		fetch: () => Promise<T>
	): Promise<T | null> {
		if (cache.has(code)) return cache.get(code) ?? null;
		const found = await orNull(fetch());
		cache.set(code, found);
		return found;
	}

	return {
		client: (code: number) =>
			cached(clients, code, () =>
				request(client.GET('/api/clients/by-code/{code}', { params: { path: { code } } }))
			),
		article: (code: number) =>
			cached(articles, code, () =>
				request(client.GET('/api/articles/by-code/{code}', { params: { path: { code } } }))
			),
		cachedClient: (code: number) => clients.get(code) ?? null,
		cachedArticle: (code: number) => articles.get(code) ?? null
	};
}
