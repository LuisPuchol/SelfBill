import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { blankNote } from '#lib/deliverynotes/form.js';
import { createLookups, orNull } from '#lib/deliverynotes/lookups.js';
import { todayIso } from '#lib/format/date.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const [periods, profile] = await loaded(
		Promise.all([
			request(api.GET('/api/tax-config/history')),
			// Issuing an invoice needs the owner's data: none yet is a 404.
			orNull(request(api.GET('/api/user-profile')))
		])
	);
	return {
		periods,
		hasProfile: profile !== null,
		initial: blankNote(todayIso()),
		lookups: createLookups(api)
	};
};
