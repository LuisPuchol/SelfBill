import { isHttpError } from '@sveltejs/kit';
import { describe, expect, it } from 'vitest';
import { ApiError, networkError } from './errors';
import { loaded } from './load';

describe('loaded', () => {
	it('turns an ApiError into a SvelteKit error with its status', async () => {
		await expect(
			loaded(Promise.reject(new ApiError(404, 'CLIENT_NOT_FOUND', '', {})))
		).rejects.toSatisfy((e) => isHttpError(e, 404));
	});

	it('turns a network error into a 503', async () => {
		await expect(loaded(Promise.reject(networkError(new TypeError())))).rejects.toSatisfy((e) =>
			isHttpError(e, 503)
		);
	});

	it('passes the value through', async () => {
		await expect(loaded(Promise.resolve(7))).resolves.toBe(7);
	});
});
