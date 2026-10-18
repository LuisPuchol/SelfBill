import { api, request } from '../api/client';
import { ApiError } from '../api/errors';
import type { components } from '../api/schema';
import { formatPercent } from '../format/number';

type TaxConfigResponse = components['schemas']['TaxConfigResponse'];
type UserProfileResponse = components['schemas']['UserProfileResponse'];

export interface SetupStatus {
	/** The period in force today, or null */
	tax: TaxConfigResponse | null;
	/** Whether any period exists (a future one only: tax null, anyTax true) */
	anyTax: boolean;
	profile: UserProfileResponse | null;
}

/** A 404 means "not configured yet"; any other failure propagates. */
async function orNull<T>(call: Promise<T>): Promise<T | null> {
	try {
		return await call;
	} catch (e) {
		if (e instanceof ApiError && e.status === 404) return null;
		throw e;
	}
}

export async function loadSetupStatus(): Promise<SetupStatus> {
	const [tax, profile] = await Promise.all([
		orNull(request(api.GET('/api/tax-config'))),
		orNull(request(api.GET('/api/user-profile')))
	]);
	const anyTax = tax !== null || (await request(api.GET('/api/tax-config/history'))).length > 0;
	return { tax, anyTax, profile };
}

/** What the home screen says is missing, or null when everything is configured. */
export function setupNotice(status: SetupStatus): string | null {
	const taxMissing = status.tax === null;
	const profileMissing = status.profile === null;
	if (!taxMissing && !profileMissing) return null;
	const go = 'Pulsa 6 para ir a Ajustes.';
	if (taxMissing && profileMissing) {
		return status.anyTax
			? `No hay IVA vigente hoy y faltan los datos del titular. ${go}`
			: `Falta configurar el IVA y los datos del titular. ${go}`;
	}
	if (taxMissing) {
		return status.anyTax ? `No hay IVA vigente hoy. ${go}` : `Falta configurar el IVA. ${go}`;
	}
	return `Faltan los datos del titular. ${go}`;
}

export function taxSummary(status: SetupStatus): string {
	if (status.tax) {
		return `IVA ${formatPercent(status.tax.vatPercentage ?? 0)} · Recargo ${formatPercent(status.tax.surchargePercentage ?? 0)}`;
	}
	return status.anyTax ? 'No hay IVA vigente hoy' : 'Sin configurar';
}
