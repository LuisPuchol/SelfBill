import { totalCents } from '../format/money';

export interface Rates {
	vat: number;
	surcharge: number;
}

export interface Regime {
	vatType: 'WITH_VAT' | 'WITHOUT_VAT';
	surchargeType: 'WITH_SURCHARGE' | 'WITHOUT_SURCHARGE';
}

type Period = { vatPercentage?: number; surchargePercentage?: number };

/** The rates of the period with the client's regime applied; null when no period covers the date. */
export function ratesFor(period: Period | null, client: Regime): Rates | null {
	if (!period) return null;
	return {
		vat: client.vatType === 'WITH_VAT' ? (period.vatPercentage ?? 0) : 0,
		surcharge: client.surchargeType === 'WITH_SURCHARGE' ? (period.surchargePercentage ?? 0) : 0
	};
}

/**
 * The rates the API gives a note for `client` (CONCEPTO §3, `DeliveryNoteTaxRegime`). A new note
 * (no `saved`) takes both from the period. A saved one keeps each frozen rate while the client's
 * regime for it is the note's, and the other comes from the period (0 if the client has none); a
 * null regime (its client deleted since) matches none. `needsPeriod`: some rate comes from the
 * period, which the API then looks up; without one, `rates` is null.
 */
export function noteRates(
	client: Regime,
	period: Period | null,
	saved?: { regime: Regime | null; rates: Rates }
): { rates: Rates | null; needsPeriod: boolean } {
	const vat = client.vatType !== saved?.regime?.vatType;
	const surcharge = client.surchargeType !== saved?.regime?.surchargeType;
	const needsPeriod = vat || surcharge;
	if (!needsPeriod) return { rates: saved!.rates, needsPeriod };
	const current = ratesFor(period, client);
	if (!current) return { rates: null, needsPeriod };
	return {
		rates: {
			vat: vat ? current.vat : saved!.rates.vat,
			surcharge: surcharge ? current.surcharge : saved!.rates.surcharge
		},
		needsPeriod
	};
}

/** The period in force on a yyyy-mm-dd date: the latest validFrom not after it. */
export function periodOn<T extends { validFrom?: string }>(date: string, periods: T[]): T | null {
	let found: T | null = null;
	for (const period of periods) {
		const from = period.validFrom;
		if (from !== undefined && from <= date && (found === null || from > (found.validFrom ?? ''))) {
			found = period;
		}
	}
	return found;
}

// Same as the API (InvoiceService): the tax is base × rate / 100 rounded half up to cents.
function taxCents(baseCents: number, rate: number): number {
	const hundredths = Math.round(rate * 100);
	return Math.floor((baseCents * hundredths + 5000) / 10000);
}

export function totals(
	lines: readonly { quantity: number; price: number }[],
	rates: Rates
): { baseCents: number; vatCents: number; surchargeCents: number; totalCents: number } {
	const baseCents = totalCents(lines);
	const vatCents = taxCents(baseCents, rates.vat);
	const surchargeCents = taxCents(baseCents, rates.surcharge);
	return { baseCents, vatCents, surchargeCents, totalCents: baseCents + vatCents + surchargeCents };
}
