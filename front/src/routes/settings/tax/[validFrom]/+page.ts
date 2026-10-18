import { error } from '@sveltejs/kit';
import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
	const periods = await loaded(request(api.GET('/api/tax-config/history')));
	const period = periods.find((p) => p.validFrom === params.validFrom);
	if (!period) error(404, 'No existe');
	return {
		initial: {
			validFrom: period.validFrom!,
			vatPercentage: period.vatPercentage ?? null,
			surchargePercentage: period.surchargePercentage ?? null
		}
	};
};
