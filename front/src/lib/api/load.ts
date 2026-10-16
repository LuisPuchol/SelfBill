import { error } from '@sveltejs/kit';
import { ApiError } from './errors';
import { errorMessages, isErrorCode } from './messages';

/** For `load` functions: turns an `ApiError` into a SvelteKit error page. */
export async function loaded<T>(call: Promise<T>): Promise<T> {
	try {
		return await call;
	} catch (e) {
		if (!(e instanceof ApiError)) throw e;
		if (e.code === 'NETWORK') error(503, 'No se puede conectar con el servidor.');
		error(
			e.status,
			isErrorCode(e.code) ? errorMessages[e.code] : 'Ha ocurrido un error inesperado.'
		);
	}
}
