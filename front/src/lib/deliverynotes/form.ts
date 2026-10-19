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
	/**
	 * The article's name as the saved note has it, while its code is empty because the article was
	 * deleted since. Only shown: never sent nor compared, and gone once a code is typed.
	 */
	savedArticleName?: string;
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

/**
 * The form of a saved note. The note has ids and the form codes: `codes` gives the code of its client
 * and of each article id, null when it was deleted since (a deleted line keeps its saved name).
 */
export function noteValues(
	response: DeliveryNoteResponse,
	codes: { client: number | null; article(articleId: number): number | null }
): NoteValues {
	return {
		code: response.code ?? null,
		clientCode: codes.client,
		date: (response.date ?? '').slice(0, 10),
		lines: (response.lines ?? []).map((line) => {
			const articleCode = line.articleId === undefined ? null : codes.article(line.articleId);
			return {
				articleCode,
				lot1: line.traceabilityCode1 ?? '',
				lot2: line.traceabilityCode2 ?? '',
				lot3: line.traceabilityCode3 ?? '',
				quantity: line.quantity ?? null,
				price: line.price ?? null,
				...(articleCode === null && { savedArticleName: line.articleName })
			};
		})
	};
}

/** What the unsaved-changes check compares: what would be sent, without empty lines nor saved names. */
export function comparableNote(values: NoteValues): NoteValues {
	return {
		...values,
		lines: values.lines
			.filter((line) => !isEmptyLine(line))
			.map((line) => ({
				articleCode: line.articleCode,
				lot1: line.lot1,
				lot2: line.lot2,
				lot3: line.lot3,
				quantity: line.quantity,
				price: line.price
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

/** The grid's cell for each field of a sent line; the extra lots are reached from Lote (F4). */
const CELLS: Record<string, keyof LineValues> = {
	articleId: 'articleCode',
	traceabilityCode1: 'lot1',
	traceabilityCode2: 'lot1',
	traceabilityCode3: 'lot1'
};

/**
 * The form's field for a path of the API's errors. The payload has ids instead of codes and leaves
 * the empty lines out, so its `lines[1]` may be the grid's third line; `sent` is what it was built from.
 */
export function formFieldPath(apiPath: string, sent: NoteValues): string {
	if (apiPath === 'clientId') return 'clientCode';
	const match = /^lines\[(\d+)\]\.(\w+)$/.exec(apiPath);
	if (!match) return apiPath;
	const sentRows = sent.lines.flatMap((line, row) => (isEmptyLine(line) ? [] : [row]));
	const row = sentRows[Number(match[1])] ?? Number(match[1]);
	return `lines[${row}].${CELLS[match[2]] ?? match[2]}`;
}
