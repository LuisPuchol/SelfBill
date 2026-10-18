import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { blankClient } from '#lib/catalog/client-form.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const next = await loaded(request(api.GET('/api/clients/next-code')));
	return { initial: blankClient(next.code ?? null) };
};
