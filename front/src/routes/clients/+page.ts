import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { listQuery } from '#lib/records/search.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url }) => {
	const q = url.searchParams.get('q') ?? '';
	const page = Math.max(0, Number(url.searchParams.get('page')) || 0);
	// `?deleted=1`: the deleted ones instead of the live ones.
	const deleted = url.searchParams.get('deleted') === '1';
	const result = await loaded(
		request(
			api.GET('/api/clients', {
				params: {
					query: {
						...listQuery(q, 'q'),
						...(deleted ? { deleted: true } : {}),
						page,
						size: 20,
						sort: ['code', 'id']
					}
				}
			})
		)
	);
	return { q, deleted, result };
};
