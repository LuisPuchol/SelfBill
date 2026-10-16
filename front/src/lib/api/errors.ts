import type { components } from './schema';
import { isErrorCode } from './messages';

export type ErrorCode = NonNullable<components['schemas']['Problem']['errorCode']>;

export class ApiError extends Error {
	constructor(
		readonly status: number,
		readonly code: ErrorCode | 'NETWORK' | 'UNKNOWN',
		readonly detail: string,
		readonly fieldErrors: Record<string, string>
	) {
		super(`${status} ${code}: ${detail}`);
		this.name = 'ApiError';
	}
}

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === 'object' && value !== null;
}

export function toApiError(status: number, body: unknown): ApiError {
	const problem = isRecord(body) ? body : {};
	const code = isErrorCode(problem.errorCode) ? problem.errorCode : 'UNKNOWN';
	const detail = typeof problem.detail === 'string' ? problem.detail : '';
	const fieldErrors = isRecord(problem.errors) ? (problem.errors as Record<string, string>) : {};
	return new ApiError(status, code, detail, fieldErrors);
}

export function networkError(cause: unknown): ApiError {
	return new ApiError(0, 'NETWORK', cause instanceof Error ? cause.message : '', {});
}
