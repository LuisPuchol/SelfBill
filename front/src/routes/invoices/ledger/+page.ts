import { todayIso } from '#lib/format/date.js';
import type { PageLoad } from './$types';

export const load: PageLoad = () => ({ today: todayIso() });
