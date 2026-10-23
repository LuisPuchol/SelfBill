import { api, request } from '#lib/api/client.js';
import { loaded } from '#lib/api/load.js';
import { noteValues } from '#lib/deliverynotes/form.js';
import { createLookups, orNull } from '#lib/deliverynotes/lookups.js';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }) => {
	const id = Number(params.id);
	const [saved, periods, profile] = await loaded(
		Promise.all([
			request(api.GET('/api/delivery-notes/{id}', { params: { path: { id } } })),
			request(api.GET('/api/tax-config/history')),
			// Issuing an invoice needs the owner's data: none yet is a 404.
			orNull(request(api.GET('/api/user-profile')))
		])
	);
	// The note has ids and the form codes: its client and articles, by id. One deleted since is
	// null, and its code stays empty.
	const articleIds = [...new Set((saved.lines ?? []).map((line) => line.articleId!))];
	const [client, articles] = await loaded(
		Promise.all([
			orNull(request(api.GET('/api/clients/{id}', { params: { path: { id: saved.clientId! } } }))),
			Promise.all(
				articleIds.map((articleId) =>
					orNull(request(api.GET('/api/articles/{id}', { params: { path: { id: articleId } } })))
				)
			)
		])
	);
	const found = articles.filter((article) => article !== null);
	// The fields look their codes up on opening: they find them here.
	const lookups = createLookups(api);
	lookups.seed({ clients: client ? [client] : [], articles: found });
	const initial = noteValues(saved, {
		client: client?.code ?? null,
		article: (articleId) => found.find((article) => article.id === articleId)?.code ?? null
	});
	return { id, saved, periods, hasProfile: profile !== null, lookups, initial };
};
