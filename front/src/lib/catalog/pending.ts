/** "1 albarán" / "3 albaranes" */
export function count(n: number, one: string, many: string): string {
	return `${n} ${n === 1 ? one : many}`;
}

/** The warning in the delete question: what still waits to be invoiced. Null when nothing does. */
export function articlePendingMessage(notes: number): string | null {
	if (notes <= 0) return null;
	return `Está en ${count(notes, 'albarán', 'albaranes')} sin facturar. Si lo borras no se podrán volver a guardar.`;
}
