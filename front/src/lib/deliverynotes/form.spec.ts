import { expect, it } from 'vitest';
import type { components } from '../api/schema';
import {
	blankNote,
	comparableNote,
	emptyLine,
	formFieldPath,
	isEmptyLine,
	noteValues,
	notePayload,
	timeOf
} from './form';

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
	const codes = { client: 3, article: (articleId: number) => articleId * 10 };
	expect(noteValues(response, codes)).toMatchObject({ code: 5, clientCode: 3, date: '2026-10-17' });
	expect(noteValues(response, codes).lines[0]).toEqual({
		articleCode: 20,
		lot1: 'L1',
		lot2: '',
		lot3: '',
		quantity: 3,
		price: 0.5
	});
	expect(timeOf(response)).toBe('08:30:00');
});

it('leaves the codes of a deleted client or article empty, keeping the name the note has', () => {
	const response: Response = {
		code: 5,
		date: '2026-10-17T08:30:00',
		lines: [{ articleId: 2, articleName: 'Tomate' }]
	};
	const values = noteValues(response, { client: null, article: () => null });
	expect(values.clientCode).toBeNull();
	expect(values.lines[0].articleCode).toBeNull();
	expect(values.lines[0].savedArticleName).toBe('Tomate');
});

it('compares only what is sent: no empty lines, no saved names', () => {
	const line = { articleCode: 4, lot1: '01210426', lot2: '', lot3: '', quantity: 1, price: 1 };
	const values = {
		...blankNote('2026-10-18'),
		lines: [{ ...line, articleCode: null, savedArticleName: 'Tomate' }, emptyLine()]
	};
	expect(comparableNote(values)).toEqual({
		...blankNote('2026-10-18'),
		lines: [{ ...line, articleCode: null }]
	});
});

it("puts the API's line errors on the grid row and field they came from", () => {
	const line = { articleCode: 4, lot1: '01210426', lot2: '', lot3: '', quantity: 1, price: 1 };
	// Rows 0 and 2 were sent, as lines[0] and lines[1]; row 1 was empty.
	const sent = { ...blankNote('2026-10-18'), lines: [line, emptyLine(), line] };
	expect(formFieldPath('lines[1].quantity', sent)).toBe('lines[2].quantity');
	expect(formFieldPath('lines[1].articleId', sent)).toBe('lines[2].articleCode');
	expect(formFieldPath('lines[0].traceabilityCode1', sent)).toBe('lines[0].lot1');
	expect(formFieldPath('lines[1].traceabilityCode3', sent)).toBe('lines[2].lot1');
	expect(formFieldPath('clientId', sent)).toBe('clientCode');
	expect(formFieldPath('date', sent)).toBe('date');
});
