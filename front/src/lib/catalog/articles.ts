import { api, request } from '../api/client';
import type { DeleteOptions } from '../records/delete-flow.svelte';
import type { RestoreOptions } from '../records/restore-flow.svelte';
import { articlePendingMessage } from './pending';

/** Deleting an article: shared by its record and its list. */
export const articleDeletion: DeleteOptions = {
	entity: { one: 'artículo', Label: 'Artículo' },
	remove: async (articleId) => {
		await request(api.DELETE('/api/articles/{id}', { params: { path: { id: articleId } } }));
	},
	pendingForDelete: async (articleId) => {
		const [notes, notesInDrafts] = await Promise.all([
			request(
				api.GET('/api/delivery-notes', {
					params: { query: { articleId, invoiced: false, size: 1 } }
				})
			),
			request(
				api.GET('/api/delivery-notes', {
					params: { query: { articleId, invoiceStatus: 'DRAFT', size: 1 } }
				})
			)
		]);
		return articlePendingMessage(
			notes.page?.totalElements ?? 0,
			notesInDrafts.page?.totalElements ?? 0
		);
	}
};

/** Restoring a deleted article: its list of deleted ones. */
export const articleRestoration: RestoreOptions = {
	entity: articleDeletion.entity,
	restore: async (articleId) => {
		await request(api.POST('/api/articles/{id}/restore', { params: { path: { id: articleId } } }));
	}
};
