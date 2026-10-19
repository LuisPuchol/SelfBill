import { totalCents } from '../format/money';

export interface Rates {
	vat: number;
	surcharge: number;
}

/** The rates of the period with the client's regime applied; null when no period covers the date. */
export function ratesFor(
	period: { vatPercentage?: number; surchargePercentage?: number } | null,
	client: {
		vatType: 'WITH_VAT' | 'WITHOUT_VAT';
		surchargeType: 'WITH_SURCHARGE' | 'WITHOUT_SURCHARGE';
	}
): Rates | null {
	if (!period) return null;
	return {
		vat: client.vatType === 'WITH_VAT' ? (period.vatPercentage ?? 0) : 0,
		surcharge: client.surchargeType === 'WITH_SURCHARGE' ? (period.surchargePercentage ?? 0) : 0
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
