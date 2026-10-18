import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, networkError } from '../api/errors';
import {
	homeStatus,
	loadSetupStatus,
	proposedValidFrom,
	setupNotice,
	taxSummary,
	type SetupStatus
} from './status';

const get = vi.fn<(path: string) => Promise<unknown>>();

vi.mock('../api/client', () => ({
	api: { GET: (path: string) => get(path) },
	request: <T>(call: Promise<T>) => call
}));

const period = { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 };
const profile = { id: 1, fullName: 'María López' };
const notFound = () => new ApiError(404, 'UNKNOWN', '', {});

function answers(routes: Record<string, unknown>) {
	get.mockImplementation(async (path) => {
		const answer = routes[path];
		if (answer instanceof Error) throw answer;
		return answer;
	});
}

beforeEach(() => get.mockReset());

describe('loadSetupStatus', () => {
	it('with a period in force it does not ask for the history', async () => {
		answers({ '/api/tax-config': period, '/api/user-profile': profile });
		await expect(loadSetupStatus()).resolves.toEqual({
			tax: period,
			anyTax: true,
			profile: profile
		});
		expect(get).not.toHaveBeenCalledWith('/api/tax-config/history');
	});

	it('nothing configured: no tax, no history, no profile', async () => {
		answers({
			'/api/tax-config': notFound(),
			'/api/tax-config/history': [],
			'/api/user-profile': notFound()
		});
		await expect(loadSetupStatus()).resolves.toEqual({ tax: null, anyTax: false, profile: null });
	});

	it('a future period only: no tax today but anyTax', async () => {
		answers({
			'/api/tax-config': notFound(),
			'/api/tax-config/history': [{ ...period, validFrom: '2099-01-01' }],
			'/api/user-profile': profile
		});
		await expect(loadSetupStatus()).resolves.toEqual({
			tax: null,
			anyTax: true,
			profile: profile
		});
	});

	it('anything but a 404 propagates', async () => {
		answers({
			'/api/tax-config': period,
			'/api/user-profile': new ApiError(500, 'UNKNOWN', '', {})
		});
		await expect(loadSetupStatus()).rejects.toMatchObject({ status: 500 });
	});
});

describe('proposedValidFrom', () => {
	it('the first period starts with the year', () => {
		expect(proposedValidFrom([], '2026-10-18')).toBe('2026-01-01');
	});

	it('later ones start today', () => {
		expect(proposedValidFrom([{ validFrom: '2026-01-01' }], '2026-10-18')).toBe('2026-10-18');
	});
});

describe('homeStatus', () => {
	const status = { tax: null, anyTax: false, profile: null };

	it('passes the status through', async () => {
		await expect(homeStatus(async () => status)).resolves.toBe(status);
	});

	it('a network error propagates, for the connection screen', async () => {
		const down = networkError(new TypeError());
		await expect(
			homeStatus(async () => {
				throw down;
			})
		).rejects.toBe(down);
	});

	it('any other error is logged and the home loads without a status', async () => {
		const log = vi.spyOn(console, 'error').mockImplementation(() => {});
		const failure = new ApiError(500, 'UNKNOWN', '', {});
		await expect(
			homeStatus(async () => {
				throw failure;
			})
		).resolves.toBeNull();
		expect(log).toHaveBeenCalledWith(expect.any(String), failure);
		log.mockRestore();
	});
});

const go = 'Pulsa 6 para ir a Ajustes.';

const none: SetupStatus = { tax: null, anyTax: false, profile: null };
const complete: SetupStatus = { tax: period, anyTax: true, profile };
const futureOnly: SetupStatus = { tax: null, anyTax: true, profile };

describe('setupNotice', () => {
	it('says nothing when everything is configured', () => {
		expect(setupNotice(complete)).toBeNull();
	});

	it('names what is missing', () => {
		expect(setupNotice(none)).toBe(`Falta configurar el IVA y los datos del titular. ${go}`);
		expect(setupNotice({ ...complete, tax: null, anyTax: false })).toBe(
			`Falta configurar el IVA. ${go}`
		);
		expect(setupNotice({ ...complete, profile: null })).toBe(`Faltan los datos del titular. ${go}`);
	});

	it('tells apart a period that only starts in the future', () => {
		expect(setupNotice(futureOnly)).toBe(`No hay IVA vigente hoy. ${go}`);
		expect(setupNotice({ ...futureOnly, profile: null })).toBe(
			`No hay IVA vigente hoy y faltan los datos del titular. ${go}`
		);
	});
});

describe('taxSummary', () => {
	it('shows the rates in force, the lack of a current period, or nothing configured', () => {
		expect(taxSummary(complete)).toBe('IVA 4 % · Recargo 0,5 %');
		expect(taxSummary(futureOnly)).toBe('No hay IVA vigente hoy');
		expect(taxSummary(none)).toBe('Sin configurar');
	});
});
