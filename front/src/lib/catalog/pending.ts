/** "1 albarán" / "3 albaranes" */
export function count(n: number, one: string, many: string): string {
	return `${n} ${n === 1 ? one : many}`;
}

/** The warning in the delete question: what still waits to be invoiced. Null when nothing does. */
export function articlePendingMessage(notes: number): string | null {
	if (notes <= 0) return null;
	return `Está en ${count(notes, 'albarán', 'albaranes')} sin facturar. Si lo borras no se podrán volver a guardar.`;
}

function pendingParts(notes: number, drafts: number, noteSuffix: string): string {
	const parts: string[] = [];
	if (notes > 0) parts.push(`${count(notes, 'albarán', 'albaranes')} ${noteSuffix}`);
	if (drafts > 0) parts.push(`${count(drafts, 'factura', 'facturas')} en borrador`);
	return parts.join(' y ');
}

/** The warning in the client's delete question. Null when nothing is pending. */
export function clientPendingMessage(notes: number, drafts: number): string | null {
	if (notes <= 0 && drafts <= 0) return null;
	return `Tiene ${pendingParts(notes, drafts, 'sin facturar')}. Si lo borras no se podrán editar ni facturar.`;
}

/** The question before changing VAT or surcharge. Null when nothing is pending. */
export function regimeChangeMessage(notes: number, drafts: number): string | null {
	if (notes <= 0 && drafts <= 0) return null;
	return `Cambiar el IVA o el recargo recalculará ${pendingParts(notes, drafts, 'sin facturar')} de este cliente.`;
}
