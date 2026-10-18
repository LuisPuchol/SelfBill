import { loaded } from '#lib/api/load.js';
import { loadSetupStatus } from '#lib/settings/status.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => ({ status: await loaded(loadSetupStatus()) });
