import createClient from 'openapi-fetch';
import { ApiError, networkError, toApiError } from './errors';
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
		// Only real transport failures mean "connection lost"; e.g. a JSON parse error must not.
		if (
			cause instanceof TypeError ||
			(cause instanceof DOMException &&
				(cause.name === 'AbortError' || cause.name === 'TimeoutError'))
		) {
			throw networkError(cause);
		}
		throw new ApiError(0, 'UNKNOWN', cause instanceof Error ? cause.message : String(cause), {});
	}
	if (result.error !== undefined) throw toApiError(result.response.status, result.error);
	return result.data as Exclude<R['data'], undefined>;
}
