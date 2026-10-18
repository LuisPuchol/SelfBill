import { loaded } from '#lib/api/load.js';
import { homeStatus, loadSetupStatus } from '#lib/settings/status.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async () => ({ status: await loaded(homeStatus(loadSetupStatus)) });
