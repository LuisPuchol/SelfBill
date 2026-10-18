/** The data an invoice needs from the client and the form still lacks, in print order. */
export function missingBillingData(v: {
	nif: string;
	address: string;
	postalCode: string;
	city: string;
}): string[] {
	const missing: string[] = [];
	if (v.nif.trim() === '') missing.push('NIF');
	if (v.address.trim() === '') missing.push('dirección');
	if (v.postalCode.trim() === '') missing.push('código postal');
	if (v.city.trim() === '') missing.push('población');
	return missing;
}

export function missingBillingMessage(missing: string[]): string | null {
	if (missing.length === 0) return null;
	const list =
		missing.length === 1
			? missing[0]
			: `${missing.slice(0, -1).join(', ')} y ${missing[missing.length - 1]}`;
	return `Para poder facturarle faltan: ${list}.`;
}
