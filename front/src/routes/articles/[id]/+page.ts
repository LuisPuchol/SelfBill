import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
	const id = Number(params.id);
	const article = await loaded(
		request(api.GET('/api/articles/{id}', { params: { path: { id } } }))
	);
	return { id, initial: { code: article.code ?? null, name: article.name ?? '' } };
};
