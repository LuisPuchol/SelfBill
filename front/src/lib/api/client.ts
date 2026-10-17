import createClient from 'openapi-fetch';
import { networkError, toApiError } from './errors';
import type { paths } from './schema';

export const api = createClient<paths>({ baseUrl: '' });

/** Unwraps an openapi-fetch call: the data on 2xx, an `ApiError` on anything else. */
export async function request<R extends { data?: unknown; error?: unknown; response: Response }>(
	call: Promise<R>
): Promise<Exclude<R['data'], undefined>> {
	let result: R;
	try {
		result = await call;
	} catch (cause) {
		throw networkError(cause);
	}
	if (result.error !== undefined) throw toApiError(result.response.status, result.error);
	return result.data as Exclude<R['data'], undefined>;
}
