import { expect, it } from 'vitest';
import { articlePayload } from './article-form';

it('trims the name for the API', () => {
	expect(articlePayload({ code: 101, name: '  Tomate pera ' })).toEqual({
		code: 101,
		name: 'Tomate pera'
	});
});
