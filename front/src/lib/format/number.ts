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
