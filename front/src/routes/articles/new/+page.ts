import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const next = await loaded(request(api.GET('/api/articles/next-code')));
	return { initial: { code: next.code ?? null, name: '' } };
};
