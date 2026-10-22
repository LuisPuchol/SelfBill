<script lang="ts">
	import { tick } from 'svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import ActionButton from '../components/ActionButton.svelte';
	import CodeField from '../components/CodeField.svelte';
	import DigitsField from '../components/DigitsField.svelte';
	import NumberField from '../components/NumberField.svelte';
	import { lineTotalCents } from '../format/money';
	import { formatDecimal } from '../format/number';
	import { getFormNav } from '../keyboard/form-nav';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { emptyLine, isEmptyLine, type LineValues } from './form';
	import LotsDialog from './LotsDialog.svelte';

	let {
		lines = $bindable(),
		readOnly = false,
		articleName,
		lookupArticle,
		proposePrice,
		errorFor
	}: {
		lines: LineValues[];
		/** Only looking: no typing, no lots dialog, no removing nor adding lines */
		readOnly?: boolean;
		/** The name of an article already looked up */
		articleName: (code: number) => string | undefined;
		/** Resolves to the article's name, or null when the code does not exist */
		lookupArticle: (code: number) => Promise<string | null>;
		/** The price to offer for a found article, or null for none */
		proposePrice: (code: number) => Promise<number | null>;
		/** The API's error for a field path such as `lines[2].quantity` */
		errorFor: (name: string) => string | undefined;
	} = $props();

	type Column = 'articleCode' | 'lot1' | 'quantity' | 'price';

	const nav = getFormNav();
	const CELL = /^lines\[(\d+)\]\.(articleCode|lot1|quantity|price)$/;

	/** The cell an element is, by its id (the field's name). */
	function cellOf(el: Element | null): { row: number; column: Column } | undefined {
		const match = el?.id.match(CELL);
		return match ? { row: Number(match[1]), column: match[2] as Column } : undefined;
	}

	function focusCell(row: number, column: Column): void {
		nav?.focus(`lines[${row}].${column}`);
	}

	/** Where the cursor is, or was last: F4, F7 and the buttons act on that line. */
	let cursor = $state.raw<{ line: LineValues; column: Column }>();
	/** The line under the cursor while it is still in the grid's lines (a new note has other lines). */
	const current = $derived(cursor && lines.includes(cursor.line) ? cursor.line : undefined);

	function onfocusin(event: FocusEvent): void {
		const cell = cellOf(event.target as Element);
		if (cell) cursor = { line: lines[cell.row], column: cell.column };
	}

	/**
	 * Lines whose code holds typed text not looked up yet. They count as written: "+" right after
	 * typing a code checks the other cells before the lookup has filled the article in.
	 */
	const typedCode = new SvelteSet<LineValues>();

	function oncodeinput(line: LineValues, event: Event): void {
		// Another article now: the name of the deleted one goes, and so does the price offered for
		// the one replaced, at once: "+" before the new code is found must not save it.
		delete line.savedArticleName;
		if (untouchedOffer(line)) {
			offers.delete(line);
			line.price = null;
		}
		if ((event.target as HTMLInputElement).value === '') typedCode.delete(line);
		else typedCode.add(line);
	}

	/** An empty line is left out of the note, so none of its cells is required. */
	function written(line: LineValues): boolean {
		return !isEmptyLine(line) || typedCode.has(line);
	}

	/** A note needs a line: while none is written, the first one is required. */
	const noneWritten = $derived(!lines.some(written));

	/** Only looking, nothing is asked for: Enter walks on even past a deleted article's empty code. */
	function cellsRequired(line: LineValues, row: number): boolean {
		return !readOnly && (written(line) || (row === 0 && noneWritten));
	}

	function nameOf(line: LineValues): string {
		if (line.articleCode === null) return line.savedArticleName ?? '';
		return articleName(line.articleCode) ?? '';
	}

	/** The last price the grid offered a line: for which article, and what it put in (null for none). */
	interface Offer {
		code: number;
		price: number | null;
	}

	/** Per line object, its latest offer; typing in the price cell makes the price the user's. */
	const offers = new WeakMap<LineValues, Offer>();

	/**
	 * The price is still the grid's to change: as offered, untouched. Also compared by value, as the
	 * numpad's comma is written without an input event.
	 */
	function untouchedOffer(line: LineValues): Offer | undefined {
		const offer = offers.get(line);
		return offer && line.price === offer.price ? offer : undefined;
	}

	/**
	 * An article found: its last price, only into an empty price. A typed or saved one is never
	 * replaced; an offer for the article replaced was emptied when its code was typed over.
	 */
	function onArticleFound(line: LineValues, code: number): void {
		if (line.price === null) void propose(line, code);
	}

	/** Puts in the last price for the article; the price stays empty meanwhile and if there is none. */
	async function propose(line: LineValues, code: number): Promise<void> {
		const offer: Offer = { code, price: null };
		offers.set(line, offer);
		// Another article's or client's price must not be saved while this one is on its way.
		line.price = null;
		let price: number | null;
		try {
			price = await proposePrice(code);
		} catch {
			// Only a help: without it the price stays empty, to be typed.
			return;
		}
		// Typed meanwhile, another article by now, or a newer offer (another client): not this one.
		if (offers.get(line) !== offer || line.price !== null || line.articleCode !== code) return;
		offer.price = price;
		line.price = price;
	}

	/** Another client: every price still as the grid offered it gets that client's last price. */
	export function reproposePrices(): void {
		for (const line of lines) {
			const offer = untouchedOffer(line);
			if (offer) void propose(line, offer.code);
		}
	}

	function amount(line: LineValues): string {
		if (line.quantity === null || line.price === null) return '';
		return formatDecimal(lineTotalCents(line.quantity, line.price) / 100);
	}

	/** "+1" / "+2": the line's other lots, kept in the lots dialog. */
	function extraLots(line: LineValues): number {
		return [line.lot2, line.lot3].filter((lot) => lot.trim() !== '').length;
	}

	/** Enter past the last price: a new line, unless the last one is still empty. */
	export async function appendLine(): Promise<void> {
		if (readOnly) return;
		const last = lines.at(-1);
		if (last === undefined || !isEmptyLine(last)) lines.push(emptyLine());
		await tick();
		focusCell(lines.length - 1, 'articleCode');
	}

	/** The cursor goes to the same column of the next line, or of the previous one. */
	async function removeLine(): Promise<void> {
		const line = current;
		if (!line || !cursor) return;
		const { column } = cursor;
		const row = lines.indexOf(line);
		typedCode.delete(line);
		// A new object, not cleared fields: the cells start afresh with it.
		if (lines.length === 1) lines[0] = emptyLine();
		else lines.splice(row, 1);
		await tick();
		focusCell(Math.min(row, lines.length - 1), column);
	}

	function move(step: -1 | 1): void {
		const cell = cellOf(document.activeElement);
		if (cell && lines[cell.row + step]) focusCell(cell.row + step, cell.column);
	}

	let lotsOpen = $state(false);

	function acceptLots([lot1, lot2, lot3]: [string, string, string]): void {
		if (!current) return;
		Object.assign(current, { lot1, lot2, lot3 });
	}

	// The keys and the buttons above the table share these actions.
	const lots: KeyAction = {
		key: 'F4',
		label: 'Lotes',
		when: () => !readOnly && current !== undefined,
		run: () => {
			lotsOpen = true;
		}
	};
	const remove: KeyAction = {
		key: 'F7',
		label: 'Quitar línea',
		when: () => !readOnly && current !== undefined,
		run: removeLine
	};

	useKeys(() => [
		{
			key: 'ArrowUp',
			label: 'Línea anterior',
			hidden: true,
			when: () => cellOf(document.activeElement) !== undefined,
			run: () => move(-1)
		},
		{
			key: 'ArrowDown',
			label: 'Línea siguiente',
			hidden: true,
			when: () => cellOf(document.activeElement) !== undefined,
			run: () => move(1)
		},
		lots,
		remove
	]);
</script>

{#if !readOnly}
	<div class="mb-2 flex gap-3">
		<ActionButton action={lots} />
		<ActionButton action={remove} />
	</div>
{/if}
<table class="w-full border-collapse bg-surface text-ink">
	<thead>
		<tr class="border-b border-line text-muted">
			<th class="w-28 px-1 py-2 text-left font-medium">Código</th>
			<th class="px-2 py-2 text-left font-medium">Artículo</th>
			<th class="w-48 px-1 py-2 text-left font-medium">Lote</th>
			<th class="w-32 px-1 py-2 text-right font-medium">Cantidad</th>
			<th class="w-32 px-1 py-2 text-right font-medium">Precio</th>
			<th class="w-32 px-2 py-2 text-right font-medium">Importe</th>
		</tr>
	</thead>
	<tbody {onfocusin}>
		{#each lines as line, i (line)}
			<tr class="align-top">
				<td class="px-1 pt-1" oninput={(event) => oncodeinput(line, event)}>
					<CodeField
						label="Código"
						name="lines[{i}].articleCode"
						bind:value={line.articleCode}
						lookup={lookupArticle}
						notFoundMessage="No existe ningún artículo con ese código"
						required={cellsRequired(line, i)}
						error={errorFor(`lines[${i}].articleCode`)}
						compact
						{readOnly}
						onFound={(code) => onArticleFound(line, code)}
					/>
				</td>
				<td class="px-2 pt-4">
					{nameOf(line)}
				</td>
				<td class="px-1 pt-1">
					<div class="flex items-start gap-2">
						<DigitsField
							label="Lote"
							name="lines[{i}].lot1"
							length={8}
							bind:value={line.lot1}
							required={cellsRequired(line, i)}
							error={errorFor(`lines[${i}].lot1`)}
							compact
							{readOnly}
						/>
						{#if extraLots(line) > 0}
							<span class="flex h-12 items-center text-muted">+{extraLots(line)}</span>
						{/if}
					</div>
				</td>
				<td class="px-1 pt-1">
					<NumberField
						label="Cantidad"
						name="lines[{i}].quantity"
						bind:value={line.quantity}
						required={cellsRequired(line, i)}
						validate={(v) => (v > 0 ? undefined : 'Tiene que ser mayor que cero')}
						error={errorFor(`lines[${i}].quantity`)}
						compact
						{readOnly}
					/>
				</td>
				<td class="px-1 pt-1" oninput={() => offers.delete(line)}>
					<NumberField
						label="Precio"
						name="lines[{i}].price"
						bind:value={line.price}
						required={cellsRequired(line, i)}
						error={errorFor(`lines[${i}].price`)}
						compact
						{readOnly}
					/>
				</td>
				<td class="tabular px-2 pt-4 text-right">{amount(line)}</td>
			</tr>
		{/each}
	</tbody>
</table>

<LotsDialog
	bind:open={lotsOpen}
	lots={current ? [current.lot1, current.lot2, current.lot3] : ['', '', '']}
	onAccept={acceptLots}
/>
