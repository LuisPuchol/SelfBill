import type { components } from '../api/schema';

type Invoice = components['schemas']['InvoiceResponse'];

/** How many invoices the ledger loads in one go; beyond it the dates must be narrowed. */
export const LEDGER_LIMIT = 2000;

/** The ledger's period on entry: today minus 30 days, up to today. */
export function defaultRange(today: string): { from: string; to: string } {
	const [year, month, day] = today.split('-').map(Number);
	// UTC arithmetic: no daylight-saving shift
	const from = new Date(Date.UTC(year, month - 1, day - 30));
	return { from: from.toISOString().slice(0, 10), to: today };
}

const cents = (amount: number | undefined) => Math.round((amount ?? 0) * 100);

/** The totals under the ledger, summed in integer cents, as floats drift. */
export function periodTotals(invoices: Invoice[]): {
	count: number;
	subtotal: number;
	vatAmount: number;
	surchargeAmount: number;
	total: number;
} {
	const sum = (pick: (i: Invoice) => number | undefined) =>
		invoices.reduce((acc, i) => acc + cents(pick(i)), 0) / 100;
	return {
		count: invoices.length,
		subtotal: sum((i) => i.subtotal),
		vatAmount: sum((i) => i.vatAmount),
		surchargeAmount: sum((i) => i.surchargeAmount),
		total: sum((i) => i.total)
	};
}
