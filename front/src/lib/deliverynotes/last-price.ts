import type { components } from '../api/schema';

type DeliveryNoteResponse = components['schemas']['DeliveryNoteResponse'];

/** `notes` newest first: the price of the last line of the article in the first note that has one. */
export function lastPrice(
	notes: readonly DeliveryNoteResponse[],
	articleId: number
): number | null {
	for (const note of notes) {
		const matching = (note.lines ?? []).filter((line) => line.articleId === articleId);
		if (matching.length > 0) return matching[matching.length - 1].price ?? null;
	}
	return null;
}

/** The newest note of the client that has the article. */
export function lastPriceQuery(clientId: number, articleId: number) {
	return { clientId, articleId, size: 1, sort: ['date,desc', 'code,desc'] };
}
