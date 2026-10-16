import { describe, expect, it } from 'vitest';
import { classifyError } from './classify';
import { ApiError, networkError } from './errors';
import { errorMessages } from './messages';

describe('classifyError', () => {
	it('classifies network failures as connection', () => {
		expect(classifyError(networkError(new TypeError()))).toEqual({ kind: 'connection' });
	});

	it('classifies a known code as business with its Spanish text', () => {
		expect(classifyError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}))).toEqual({
			kind: 'business',
			message: errorMessages.INVOICE_MIXED_MONTHS
		});
		expect(classifyError(new ApiError(500, 'EMAIL_SEND_FAILED', 'x', {})).kind).toBe('business');
	});

	it('classifies INTERNAL_ERROR as unexpected', () => {
		expect(classifyError(new ApiError(500, 'INTERNAL_ERROR', 'Internal server error', {}))).toEqual(
			{
				kind: 'unexpected',
				status: 500,
				code: 'INTERNAL_ERROR',
				detail: 'Internal server error'
			}
		);
	});

	it('classifies an unknown code as unexpected', () => {
		expect(classifyError(new ApiError(404, 'UNKNOWN', '', {})).kind).toBe('unexpected');
	});

	it('classifies a plain Error as unexpected', () => {
		expect(classifyError(new Error('boom'))).toEqual({
			kind: 'unexpected',
			status: 0,
			code: '',
			detail: 'boom'
		});
	});
});
