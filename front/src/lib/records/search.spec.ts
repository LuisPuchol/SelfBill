import { describe, expect, it } from 'vitest';
import { isCodeSearch, listQuery } from './search';

describe('isCodeSearch', () => {
	it('is a code search only with digits, once trimmed', () => {
		expect(isCodeSearch('7')).toBe(true);
		expect(isCodeSearch(' 12 ')).toBe(true);
		expect(isCodeSearch('7a')).toBe(false);
		expect(isCodeSearch('')).toBe(false);
		expect(isCodeSearch('   ')).toBe(false);
	});
});

describe('listQuery', () => {
	it('digits ask for that exact code', () => {
		expect(listQuery('7', 'q')).toEqual({ codeFrom: 7, codeTo: 7 });
		expect(listQuery(' 12 ', 'name')).toEqual({ codeFrom: 12, codeTo: 12 });
	});

	it('anything else filters by text, with the given parameter', () => {
		expect(listQuery('garcía', 'q')).toEqual({ q: 'garcía' });
		expect(listQuery('tom', 'name')).toEqual({ name: 'tom' });
		expect(listQuery('  tom ', 'name')).toEqual({ name: 'tom' });
	});

	it('an empty search filters nothing', () => {
		expect(listQuery('  ', 'q')).toEqual({});
		expect(listQuery('', 'name')).toEqual({});
	});
});
