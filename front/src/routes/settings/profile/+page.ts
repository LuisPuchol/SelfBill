import { api, request } from '#lib/api/client.js';
import { ApiError } from '#lib/api/errors.js';
import { loaded } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => ({
	// A 404 means there is no profile yet: the form opens empty.
	profile: await loaded(
		request(api.GET('/api/user-profile')).catch((e: unknown) => {
			if (e instanceof ApiError && e.status === 404) return null;
			throw e;
		})
	)
});
