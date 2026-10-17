import { describe, expect, it } from 'vitest';
import { request } from './client';

describe('request', () => {
	it('returns the data of a successful call', async () => {
		await expect(
			request(Promise.resolve({ data: { id: 1 }, response: new Response(null, { status: 200 }) }))
		).resolves.toEqual({ id: 1 });
	});

	it('turns an error response into an ApiError', async () => {
		await expect(
			request(
				Promise.resolve({
					error: { errorCode: 'INVOICE_ALREADY_ISSUED' },
					response: new Response(null, { status: 409 })
				})
			)
		).rejects.toMatchObject({ code: 'INVOICE_ALREADY_ISSUED', status: 409 });
	});

	it('turns a failed fetch into a NETWORK error', async () => {
		await expect(request(Promise.reject(new TypeError('Failed to fetch')))).rejects.toMatchObject({
			code: 'NETWORK'
		});
	});
});
