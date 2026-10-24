import type { components } from '../api/schema';

type Invoice = components['schemas']['InvoiceResponse'];

/** How many invoices the ledger loads in one go; beyond it the dates must be narrowed. */
export const LEDGER_LIMIT = 2000;

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
