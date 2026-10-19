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
