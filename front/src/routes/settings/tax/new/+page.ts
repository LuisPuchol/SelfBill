import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { todayIso } from '#lib/format/date.js';
import { proposedValidFrom } from '#lib/settings/status.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => {
	const periods = await loaded(request(api.GET('/api/tax-config/history')));
	const validFrom = proposedValidFrom(periods, todayIso());
	return { initial: { validFrom, vatPercentage: null, surchargePercentage: null } };
};
