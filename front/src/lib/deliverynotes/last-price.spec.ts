import { expect, it } from 'vitest';
import type { components } from '../api/schema';
import { lastPrice, lastPriceQuery } from './last-price';

type Note = components['schemas']['DeliveryNoteResponse'];

const notes: Note[] = [
	{ lines: [{ articleId: 1, price: 9 }] },
	{
		lines: [
			{ articleId: 2, price: 1 },
			{ articleId: 2, price: 1.5 },
			{ articleId: 3, price: 7 }
		]
	},
	{ lines: [{ articleId: 2, price: 99 }] }
];

it('takes the last matching line of the first note that has the article', () => {
	expect(lastPrice(notes, 2)).toBe(1.5);
});

it('is null without a match or notes', () => {
	expect(lastPrice(notes, 4)).toBeNull();
	expect(lastPrice([], 2)).toBeNull();
});

it('queries the newest note of the client with the article', () => {
	expect(lastPriceQuery(7, 2)).toEqual({
		clientId: 7,
		articleId: 2,
		size: 1,
		sort: ['date,desc', 'code,desc']
	});
});
