import type { components } from '../api/schema';
import { missingDataText } from '../deliverynotes/invoice-blocker';

/** One future invoice: a client's pending notes of one month and one pair of rates. */
export type PendingInvoice = components['schemas']['PendingInvoiceResponse'];

/** The Aviso column: why the row cannot be issued, in a word; empty when it can. */
export function blockNotice(p: PendingInvoice): string {
	if (p.clientDeleted) return 'Cliente borrado';
	if ((p.missingFiscalData ?? []).length > 0) return 'Faltan datos';
	return '';
}

/** Why the row cannot be issued, in full; null when it can. A deleted client is beyond fixing. */
export function blockReason(p: PendingInvoice): string | null {
	if (p.clientDeleted) return 'El cliente está borrado';
	const missing = p.missingFiscalData ?? [];
	if (missing.length > 0) {
		return `Faltan datos del cliente para facturar: ${missingDataText(missing)} (se corrigen en Clientes)`;
	}
	return null;
}

const MONTHS = [
	'enero',
	'febrero',
	'marzo',
	'abril',
	'mayo',
	'junio',
	'julio',
	'agosto',
	'septiembre',
	'octubre',
	'noviembre',
	'diciembre'
];

/** "2026-10" → "10/2026" */
export function monthShort(month: string): string {
	const [year, number] = month.split('-');
	return `${number}/${year}`;
}

/** "2026-10" → "octubre 2026" */
export function monthLong(month: string): string {
	const [year, number] = month.split('-');
	return `${MONTHS[Number(number) - 1]} ${year}`;
}

/**
 * What a batch issued: "Factura 101 emitida", "27 facturas emitidas: 101 a 127", or with both
 * series "25 facturas emitidas: 101 a 120, S-5 a S-9" (the general one first).
 */
export function issuedSummary(numbers: string[]): string {
	if (numbers.length === 0) return 'No se ha emitido ninguna factura';
	if (numbers.length === 1) return `Factura ${numbers[0]} emitida`;
	// "S-5" → prefix "S-" and code 5; the general series has no prefix.
	const series = new Map<string, number[]>();
	for (const number of numbers) {
		const [, prefix, code] = /^(\D*)(\d+)$/.exec(number) ?? ['', number, '0'];
		series.set(prefix, [...(series.get(prefix) ?? []), Number(code)]);
	}
	const ranges = [...series.entries()]
		.sort(([a], [b]) => a.localeCompare(b))
		.map(([prefix, codes]) => {
			const first = Math.min(...codes);
			const last = Math.max(...codes);
			return first === last ? `${prefix}${first}` : `${prefix}${first} a ${prefix}${last}`;
		});
	return `${numbers.length} facturas emitidas: ${ranges.join(', ')}`;
}
