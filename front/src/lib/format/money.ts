const euro = new Intl.NumberFormat('es-ES', {
	style: 'currency',
	currency: 'EUR',
	// es-ES skips the separator below five digits unless forced
	useGrouping: 'always'
});

export function formatMoney(euros: number): string {
	return euro.format(euros);
}

// Integer cents avoid float drift (3 * 0.1 = 0.30000000000000004); half up.
export function lineTotalCents(quantity: number, price: number): number {
	const q = Math.round(quantity * 100);
	const p = Math.round(price * 100);
	return Math.floor((q * p + 50) / 100);
}

/** Provisional total: each line is rounded to cents before summing. The API's totals are authoritative. */
export function totalCents(lines: readonly { quantity: number; price: number }[]): number {
	return lines.reduce((sum, line) => sum + lineTotalCents(line.quantity, line.price), 0);
}
