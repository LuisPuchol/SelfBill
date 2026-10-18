import { error } from '@sveltejs/kit';
import { ApiError } from './errors';

/** For `load` functions: turns an `ApiError` into a SvelteKit error page. */
export async function loaded<T>(call: Promise<T>): Promise<T> {
	try {
		return await call;
	} catch (e) {
		if (!(e instanceof ApiError)) throw e;
		if (e.code === 'NETWORK') error(503, 'Sin conexión');
		error(e.status || 500, 'Error');
	}
}
