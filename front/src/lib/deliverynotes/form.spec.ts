import { expect, it } from 'vitest';
import type { components } from '../api/schema';
import { blankNote, emptyLine, isEmptyLine, noteValues, notePayload, timeOf } from './form';

type Response = components['schemas']['DeliveryNoteResponse'];

const ids = { clientId: 7, articleId: (code: number) => code * 10 };

it('starts a blank note with one empty line', () => {
	expect(blankNote('2026-10-18')).toEqual({
		code: null,
		clientCode: null,
		date: '2026-10-18',
		lines: [emptyLine()]
	});
});

it('treats a fresh line and one with only lot2 as empty', () => {
	expect(isEmptyLine(emptyLine())).toBe(true);
	expect(isEmptyLine({ ...emptyLine(), lot2: 'x' })).toBe(true);
	expect(isEmptyLine({ ...emptyLine(), quantity: 1 })).toBe(false);
});

it('builds the payload without empty lines, with ids and null optional lots', () => {
	const values = {
		...blankNote('2026-10-18'),
		clientCode: 3,
		lines: [
			{ articleCode: 4, lot1: '01210426', lot2: '', lot3: '', quantity: 2.5, price: 1.2 },
			emptyLine()
		]
	};
	const payload = notePayload(values, ids, '09:15:00');
	expect(payload).toEqual({
		date: '2026-10-18T09:15:00',
		clientId: 7,
		lines: [
			{
				articleId: 40,
				traceabilityCode1: '01210426',
				traceabilityCode2: null,
				traceabilityCode3: null,
				quantity: 2.5,
				price: 1.2
			}
		]
	});
	expect('code' in payload).toBe(false);
	expect(notePayload({ ...values, code: 12 }, ids, '09:15:00').code).toBe(12);
});

it('reads values and time back from a response', () => {
	const response: Response = {
		code: 5,
		date: '2026-10-17T08:30:00',
		lines: [
			{
				articleId: 2,
				traceabilityCode1: 'L1',
				traceabilityCode2: undefined,
				quantity: 3,
				price: 0.5
			}
		]
	};
	expect(noteValues(response)).toMatchObject({ code: 5, date: '2026-10-17' });
	expect(noteValues(response).lines[0]).toMatchObject({
		lot1: 'L1',
		lot2: '',
		quantity: 3,
		price: 0.5
	});
	expect(timeOf(response)).toBe('08:30:00');
});
