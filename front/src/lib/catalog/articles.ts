import { api, request } from '../api/client';
import type { DeleteOptions } from '../records/delete-flow.svelte';
import { articlePendingMessage } from './pending';

/** Deleting an article: shared by its record and its list. */
export const articleDeletion: DeleteOptions = {
	entity: { one: 'artículo', Label: 'Artículo' },
	remove: async (articleId) => {
		await request(api.DELETE('/api/articles/{id}', { params: { path: { id: articleId } } }));
	},
	pendingForDelete: async (articleId) => {
		const notes = await request(
			api.GET('/api/delivery-notes', {
				params: { query: { articleId, invoiced: false, size: 1 } }
			})
		);
		return articlePendingMessage(notes.page?.totalElements ?? 0);
	}
};
