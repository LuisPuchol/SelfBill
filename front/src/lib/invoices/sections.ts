import type { components } from '../api/schema';
import type { Column } from '../components/table';
import { formatDate } from '../format/date';
import { formatMoney } from '../format/money';

export type Section = NonNullable<components['schemas']['InvoiceResponse']['sections']>[number];

/** The notes of an invoice: date, code and amount. */
export const sectionColumns: Column<Section>[] = [
	{ label: 'Fecha', value: (s) => formatDate(s.deliveryNoteDate ?? '') },
	{ label: 'Alb.', value: (s) => String(s.deliveryNoteCode ?? ''), align: 'right' },
	{ label: 'Importe', value: (s) => formatMoney(s.subtotal ?? 0), align: 'right' }
];
