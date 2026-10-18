import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { listQuery } from '#lib/records/search.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url }) => {
	const q = url.searchParams.get('q') ?? '';
	const page = Math.max(0, Number(url.searchParams.get('page')) || 0);
	const result = await loaded(
		request(
			api.GET('/api/articles', {
				params: { query: { ...listQuery(q, 'name'), page, size: 20, sort: ['code'] } }
			})
		)
	);
	return { q, result };
};
