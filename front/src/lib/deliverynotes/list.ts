import type { components } from '../api/schema';

type Note = components['schemas']['DeliveryNoteResponse'];

/** Where a note stands in the list: in no invoice, in a draft one, or in an issued one. */
export function stateText(note: {
	invoiceStatus?: Note['invoiceStatus'] | null;
	invoiceNumber?: string | null;
}): string {
	if (note.invoiceStatus === 'ISSUED') return `Facturado nº ${note.invoiceNumber}`;
	if (note.invoiceStatus === 'DRAFT') return 'En factura borrador';
	return 'Pendiente';
}

/** The text of a list with no note and no search: with the pending filter, none is left to invoice. */
export function emptyHint(pending: boolean): string {
	return pending
		? 'No hay albaranes pendientes de facturar.'
		: 'Todavía no hay albaranes. Pulsa F2 para hacer el primero.';
}
