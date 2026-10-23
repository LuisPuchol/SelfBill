import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { orNull } from '#lib/deliverynotes/lookups.js';
import { todayIso } from '#lib/format/date.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	// Issuing needs the owner's data: none yet is a 404.
	const profile = await loaded(orNull(request(api.GET('/api/user-profile'))));
	return { hasProfile: profile !== null, today: todayIso() };
};
