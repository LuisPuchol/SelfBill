import { ApiError } from './errors';
import { errorMessages, isErrorCode } from './messages';

export type ErrorView =
	| { kind: 'connection' }
	| { kind: 'business'; message: string }
	| { kind: 'unexpected'; status: number; code: string; detail: string };

export function classifyError(err: unknown): ErrorView {
	if (err instanceof ApiError) {
		if (err.code === 'NETWORK') return { kind: 'connection' };
		if (isErrorCode(err.code) && err.code !== 'INTERNAL_ERROR') {
			return { kind: 'business', message: errorMessages[err.code] };
		}
		return { kind: 'unexpected', status: err.status, code: err.code, detail: err.detail };
	}
	return {
		kind: 'unexpected',
		status: 0,
		code: '',
		detail: err instanceof Error ? err.message : String(err)
	};
}
