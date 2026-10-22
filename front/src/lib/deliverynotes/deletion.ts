import { api, request } from '../api/client';
import type { DeleteOptions } from '../records/delete-flow.svelte';

/** Deleting a delivery note: shared by its record and its list. Only a pending one, so nothing depends on it. */
export const deliveryNoteDeletion: DeleteOptions = {
	entity: { one: 'albarán', Label: 'Albarán' },
	remove: async (noteId) => {
		await request(api.DELETE('/api/delivery-notes/{id}', { params: { path: { id: noteId } } }));
	},
	pendingForDelete: async () => null
};
