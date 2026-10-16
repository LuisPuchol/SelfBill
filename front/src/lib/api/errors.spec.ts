import { describe, expect, it } from 'vitest';
import { networkError, toApiError } from './errors';

describe('toApiError', () => {
	it('reads the error code from the body', () => {
		const dup = toApiError(409, {
			status: 409,
			detail: 'Already exists a client with code: 15',
			errorCode: 'CLIENT_DUPLICATE_CODE'
		});
		expect(dup).toMatchObject({ status: 409, code: 'CLIENT_DUPLICATE_CODE', fieldErrors: {} });
	});

	it('keeps the per-field validation errors', () => {
		const invalid = toApiError(400, {
			errorCode: 'VALIDATION_ERROR',
			errors: { nif: 'NIF cannot be blank', 'lines[2].quantity': 'Quantity cannot be null' }
		});
		expect(invalid.fieldErrors).toEqual({
			nif: 'NIF cannot be blank',
			'lines[2].quantity': 'Quantity cannot be null'
		});
	});

	it('is UNKNOWN for a Spring response without errorCode', () => {
		expect(
			toApiError(404, { timestamp: 't', status: 404, error: 'Not Found', path: '/api/nope' })
		).toMatchObject({ code: 'UNKNOWN', status: 404 });
	});

	it('is UNKNOWN for a code it does not know', () => {
		expect(toApiError(409, { errorCode: 'SOMETHING_NEW' }).code).toBe('UNKNOWN');
	});

	it('survives a body that is not an object', () => {
		expect(toApiError(500, 'not json')).toMatchObject({ code: 'UNKNOWN', detail: '' });
	});
});

describe('networkError', () => {
	it('has status 0 and code NETWORK', () => {
		expect(networkError(new TypeError('Failed to fetch'))).toMatchObject({
			code: 'NETWORK',
			status: 0
		});
	});
});
