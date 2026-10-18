import { api, request } from '../api/client';
import type { DeleteOptions } from '../records/delete-flow.svelte';
import { clientPendingMessage } from './pending';

/** What still waits on the client: unbilled delivery notes and draft invoices. */
export async function clientPending(clientId: number): Promise<[number, number]> {
	const [notes, drafts] = await Promise.all([
		request(
			api.GET('/api/delivery-notes', {
				params: { query: { clientId, invoiced: false, size: 1 } }
			})
		),
		request(api.GET('/api/invoices', { params: { query: { clientId, status: 'DRAFT', size: 1 } } }))
	]);
	return [notes.page?.totalElements ?? 0, drafts.page?.totalElements ?? 0];
}

/** Deleting a client: shared by its record and its list. */
export const clientDeletion: DeleteOptions = {
	entity: { one: 'cliente', Label: 'Cliente' },
	remove: async (clientId) => {
		await request(api.DELETE('/api/clients/{id}', { params: { path: { id: clientId } } }));
	},
	pendingForDelete: async (clientId) => {
		const [notes, drafts] = await clientPending(clientId);
		return clientPendingMessage(notes, drafts);
	}
};
