import { api, request } from '../api/client';
import type { DeleteOptions } from '../records/delete-flow.svelte';

/** Deleting an issued invoice: its notes go back to pending and its number is not reused. */
export const invoiceDeletion: DeleteOptions = {
	entity: { one: 'factura', Label: 'Factura', feminine: true },
	remove: async (id) => {
		await request(api.DELETE('/api/invoices/{id}', { params: { path: { id } } }));
	},
	pendingForDelete: async () =>
		'Sus albaranes vuelven a quedar pendientes de facturar. Su número no se vuelve a usar.'
};
