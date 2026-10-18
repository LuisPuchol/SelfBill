import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => ({
	periods: await loaded(request(api.GET('/api/tax-config/history')))
});
