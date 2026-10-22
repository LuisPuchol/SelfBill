export interface InvoiceBlockerInput {
	client: {
		nif?: string | null;
		address?: string | null;
		postalCode?: string | null;
		city?: string | null;
	} | null;
	hasProfile: boolean;
	inInvoice: boolean;
}

const CLIENT_FIELDS = [
	['nif', 'NIF'],
	['address', 'dirección'],
	['postalCode', 'código postal'],
	['city', 'población']
] as const;

/** "NIF", "NIF y código postal", "NIF, dirección y población" */
function joinNames(names: string[]): string {
	if (names.length < 2) return names.join('');
	return `${names.slice(0, -1).join(', ')} y ${names[names.length - 1]}`;
}

/** Whether "Emitir factura" can be ticked, and when it cannot, why (null: nothing to say). */
export function invoiceBlocker({ client, hasProfile, inInvoice }: InvoiceBlockerInput): {
	enabled: boolean;
	reason: string | null;
} {
	if (!client) return { enabled: false, reason: null };
	if (inInvoice) return { enabled: false, reason: 'Ya está en una factura' };
	const missing = CLIENT_FIELDS.filter(([key]) => !client[key]?.trim()).map(([, name]) => name);
	if (missing.length > 0) {
		return {
			enabled: false,
			reason: `Faltan datos del cliente para facturar: ${joinNames(missing)}`
		};
	}
	if (!hasProfile) return { enabled: false, reason: 'Faltan los datos del titular (Ajustes)' };
	return { enabled: true, reason: null };
}
