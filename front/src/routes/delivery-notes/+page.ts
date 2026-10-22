import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { listQuery } from '#lib/records/search.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ url }) => {
	const q = url.searchParams.get('q') ?? '';
	const pending = url.searchParams.get('pending') === '1';
	const page = Math.max(0, Number(url.searchParams.get('page')) || 0);
	const result = await loaded(
		request(
			api.GET('/api/delivery-notes', {
				params: {
					query: {
						...listQuery(q, 'q'),
						invoiced: pending ? false : undefined,
						page,
						size: 20,
						sort: ['date,desc', 'code,desc']
					}
				}
			})
		)
	);
	return { q, pending, result };
};
