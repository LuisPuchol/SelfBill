import type { components } from '../api/schema';

type DeliveryNoteResponse = components['schemas']['DeliveryNoteResponse'];
type DeliveryNoteRequest = components['schemas']['DeliveryNoteRequest'];

export interface LineValues {
	articleCode: number | null;
	lot1: string;
	lot2: string;
	lot3: string;
	quantity: number | null;
	price: number | null;
}

export interface NoteValues {
	code: number | null;
	clientCode: number | null;
	/** yyyy-mm-dd */
	date: string;
	lines: LineValues[];
}

export function emptyLine(): LineValues {
	return { articleCode: null, lot1: '', lot2: '', lot3: '', quantity: null, price: null };
}

/** lot2 and lot3 are optional extras: they alone do not make a line. */
export function isEmptyLine(line: LineValues): boolean {
	return (
		line.articleCode === null &&
		line.lot1.trim() === '' &&
		line.quantity === null &&
		line.price === null
	);
}

export function blankNote(date: string): NoteValues {
	return { code: null, clientCode: null, date, lines: [emptyLine()] };
}

/** Client and article codes are resolved later, once the lookups answer. */
export function noteValues(response: DeliveryNoteResponse): NoteValues {
	return {
		code: response.code ?? null,
		clientCode: null,
		date: (response.date ?? '').slice(0, 10),
		lines: (response.lines ?? []).map((line) => ({
			articleCode: null,
			lot1: line.traceabilityCode1 ?? '',
			lot2: line.traceabilityCode2 ?? '',
			lot3: line.traceabilityCode3 ?? '',
			quantity: line.quantity ?? null,
			price: line.price ?? null
		}))
	};
}

/** The time of the saved note, kept on edit because the form only shows the date. */
export function timeOf(response: DeliveryNoteResponse): string {
	return (response.date ?? '').slice(11, 19);
}

function lotOrNull(lot: string): string | null {
	const trimmed = lot.trim();
	return trimmed === '' ? null : trimmed;
}

export function notePayload(
	values: NoteValues,
	ids: { clientId: number; articleId(code: number): number },
	time: string
): DeliveryNoteRequest {
	return {
		...(values.code !== null && { code: values.code }),
		clientId: ids.clientId,
		date: `${values.date}T${time}`,
		lines: values.lines
			.filter((line) => !isEmptyLine(line))
			.map((line) => ({
				articleId: ids.articleId(line.articleCode as number),
				traceabilityCode1: line.lot1.trim(),
				// The API takes null for "nothing"; the generated type only models absence.
				traceabilityCode2: lotOrNull(line.lot2) as string,
				traceabilityCode3: lotOrNull(line.lot3) as string,
				quantity: line.quantity as number,
				price: line.price as number
			}))
	};
}
