import type { components } from '../api/schema';
import type { api as Api } from '../api/client';
import { request } from '../api/client';
import { ApiError } from '../api/errors';

type ClientResponse = components['schemas']['ClientResponse'];
type ArticleResponse = components['schemas']['ArticleResponse'];

/** A 404 is an answer: null. */
export async function orNull<T>(call: Promise<T>): Promise<T | null> {
	try {
		return await call;
	} catch (error) {
		if (error instanceof ApiError && error.status === 404) return null;
		throw error;
	}
}

/**
 * Client and article lookups by code, cached per code for the life of the editor. Only what is found:
 * a code missing now may be created meanwhile, in another tab.
 */
export function createLookups(client: typeof Api) {
	const clients = new Map<number, ClientResponse>();
	const articles = new Map<number, ArticleResponse>();

	async function cached<T>(
		cache: Map<number, T>,
		code: number,
		fetch: () => Promise<T>
	): Promise<T | null> {
		const known = cache.get(code);
		if (known !== undefined) return known;
		const found = await orNull(fetch());
		if (found !== null) cache.set(code, found);
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
		cachedArticle: (code: number) => articles.get(code) ?? null,
		/** Already known, e.g. a saved note's client and articles: never asked for again. */
		seed(found: { clients?: ClientResponse[]; articles?: ArticleResponse[] }): void {
			for (const c of found.clients ?? []) if (c.code !== undefined) clients.set(c.code, c);
			for (const a of found.articles ?? []) if (a.code !== undefined) articles.set(a.code, a);
		}
	};
}

export type Lookups = ReturnType<typeof createLookups>;
