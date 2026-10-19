import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { blankNote } from '#lib/deliverynotes/form.js';
import { createLookups } from '#lib/deliverynotes/lookups.js';
import { todayIso } from '#lib/format/date.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const periods = await loaded(request(api.GET('/api/tax-config/history')));
	return { periods, initial: blankNote(todayIso()), lookups: createLookups(api) };
};
