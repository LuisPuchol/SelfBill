import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { clientValues } from '#lib/catalog/client-form.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
	const id = Number(params.id);
	const client = await loaded(request(api.GET('/api/clients/{id}', { params: { path: { id } } })));
	return { id, initial: clientValues(client) };
};
