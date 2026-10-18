import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { todayIso } from '#lib/format/date.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const periods = await loaded(request(api.GET('/api/tax-config/history')));
	const today = todayIso();
	// The first period usually starts with the year.
	const validFrom = periods.length === 0 ? `${today.slice(0, 4)}-01-01` : today;
	return { initial: { validFrom, vatPercentage: null, surchargePercentage: null } };
};
