const DECIMAL = /^\d+(?:[.,]\d+)?$/;

/** Accepts "," or "." as decimal separator, never as thousands separator. No negatives. */
export function parseDecimal(text: string): number | null {
	const trimmed = text.trim();
	if (!DECIMAL.test(trimmed)) return null;
	return Number(trimmed.replace(',', '.'));
}

export function formatDecimal(value: number, decimals = 2): string {
	return new Intl.NumberFormat('es-ES', {
		minimumFractionDigits: decimals,
		maximumFractionDigits: decimals,
		useGrouping: false
	}).format(value);
}

const NBSP = String.fromCharCode(160);

/** "4 %", "0,5 %", "5,25 %": up to 2 decimals, no trailing zeros, non-breaking space before "%". */
export function formatPercent(value: number): string {
	const text = new Intl.NumberFormat('es-ES', {
		minimumFractionDigits: 0,
		maximumFractionDigits: 2,
		useGrouping: false
	}).format(value);
	return `${text}${NBSP}%`;
}
