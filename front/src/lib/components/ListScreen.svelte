<script lang="ts" generics="T">
	import { onMount, tick, untrack } from 'svelte';
	import { on } from 'svelte/events';
	import { createSubscriber } from 'svelte/reactivity';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { isCodeSearch } from '../records/search';
	import DataTable from './DataTable.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';
	import type { Column } from './table';

	let {
		title,
		entity,
		columns,
		rows,
		page,
		search,
		selectId,
		rowId,
		onSearch,
		onPage,
		onOpen,
		onNew,
		onExit
	}: {
		title: string;
		/** e.g. `{ one: 'cliente', many: 'clientes' }` */
		entity: { one: string; many: string };
		columns: Column<T>[];
		rows: T[];
		page: { number: number; totalPages: number };
		/** The search the rows answer to (from the URL) */
		search: string;
		/** Row to select whenever the rows change, if it is on the page: the last one opened */
		selectId?: number;
		rowId: (row: T) => number;
		onSearch: (text: string) => void;
		onPage: (n: number) => void;
		onOpen: (row: T) => void;
		onNew: () => void;
		onExit: () => void;
	} = $props();

	const PAUSE_MS = 300;

	let input: HTMLInputElement;
	let tableBox: HTMLElement;
	let text = $state(untrack(() => search));
	let selected = $state(0);
	let timer: ReturnType<typeof setTimeout> | undefined;
	/** The last text handed to onSearch: on its way while `search` has not caught up with it. */
	let sent = untrack(() => search);
	/** An Enter pressed before its rows came: they open as soon as they answer exactly this text. */
	let pendingOpen: string | null = null;

	const searched = $derived(search.trim());
	const emptyText = $derived(
		searched === ''
			? `Todavía no hay ${entity.many}. Pulsa F2 para dar de alta el primero.`
			: isCodeSearch(searched)
				? `No existe ningún ${entity.one} con el código ${searched}.`
				: `No hay ningún ${entity.one} que coincida con «${searched}».`
	);

	onMount(() => {
		input.focus();
		// A search still waiting for the pause is dropped when the list goes away.
		return () => clearTimeout(timer);
	});

	// A search we did not send (the top bar's link from `?q=gar`, the browser's back): the box
	// follows the URL, and whatever was waiting for the old text is dropped.
	$effect(() => {
		const value = search;
		untrack(() => {
			if (value.trim() === sent.trim()) return;
			cancelTimer();
			pendingOpen = null;
			text = sent = value;
		});
	});

	// DataTable goes back to the first row when the rows change (its effect runs first): then
	// the remembered row, if it is on this page.
	$effect(() => {
		const index = rows.findIndex((row) => rowId(row) === untrack(() => selectId));
		if (index >= 0) selected = index;
	});

	// After the selection above has settled: the rows an earlier Enter was waiting for.
	$effect(() => {
		void rows;
		const answered = searched;
		untrack(() => {
			if (pendingOpen === null || pendingOpen !== answered) return;
			pendingOpen = null;
			// None (the code does not exist): the Enter is spent and the empty message says why.
			const row = rows[selected];
			if (row !== undefined) onOpen(row);
		});
	});

	function schedule(): void {
		// Typing again changes what the earlier Enter was meant for.
		pendingOpen = null;
		clearTimeout(timer);
		timer = setTimeout(() => {
			timer = undefined;
			sent = text;
			onSearch(text);
		}, PAUSE_MS);
	}

	/** Drops a search still waiting for the pause, e.g. on leaving: it would bring the list back. */
	function cancelTimer(): void {
		clearTimeout(timer);
		timer = undefined;
	}

	/** Searches what is typed now, without waiting for the pause. */
	function searchNow(value: string): void {
		cancelTimer();
		sent = value;
		onSearch(value);
	}

	/** The rows on screen do not answer what is typed yet: a search is waiting or on its way. */
	const waiting = () => timer !== undefined || text.trim() !== searched;

	function openRow(row: T): void {
		cancelTimer();
		pendingOpen = null;
		onOpen(row);
	}

	function turnPage(n: number): void {
		pendingOpen = null;
		// The page goes out with the search on screen, and replaces one still on its way.
		sent = search;
		onPage(n);
	}

	function move(delta: number): void {
		pendingOpen = null;
		selected = Math.min(rows.length - 1, Math.max(0, selected + delta));
		void tick().then(() =>
			tableBox.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest' })
		);
	}

	function open(): void {
		const wanted = text.trim();
		if (wanted !== searched) {
			// Rows of an older search ("7" and Enter faster than the pause): search now, unless it is
			// already on its way, and open when the rows for this text come.
			pendingOpen = wanted;
			if (timer !== undefined || sent.trim() !== wanted) searchNow(text);
			return;
		}
		// The rows answer what is typed; a search still waiting would only repeat it.
		cancelTimer();
		const row = rows[selected];
		if (row !== undefined) openRow(row);
	}

	// `when` runs inside a $derived in the key bar: re-evaluate it as the focus moves.
	const subscribe = createSubscriber((update) => {
		const off = [on(document, 'focusin', update), on(document, 'focusout', update)];
		return () => off.forEach((remove) => remove());
	});
	/** DataTable leaves these keys to a focused text field: from the search box they are ours. */
	const inSearch = () => {
		subscribe();
		return document.activeElement === input;
	};
	const hasRows = () => rows.length > 0 && inSearch();
	// While a search is on its way, Enter waits for its rows even if none are on screen now.
	const canOpen = () => (rows.length > 0 || waiting()) && inSearch();

	useKeys(() => [
		{ key: 'ArrowUp', label: 'Anterior', hidden: true, when: hasRows, run: () => move(-1) },
		{ key: 'ArrowDown', label: 'Siguiente', hidden: true, when: hasRows, run: () => move(1) },
		{ key: 'Enter', label: 'Abrir', when: canOpen, run: open },
		{
			key: 'PageDown',
			label: 'Página siguiente',
			when: () => page.number + 1 < page.totalPages && inSearch(),
			run: () => turnPage(page.number + 1)
		},
		{
			key: 'PageUp',
			label: 'Página anterior',
			when: () => page.number > 0 && inSearch(),
			run: () => turnPage(page.number - 1)
		},
		{
			key: 'F2',
			label: 'Nuevo',
			run: () => {
				cancelTimer();
				pendingOpen = null;
				onNew();
			}
		},
		text === ''
			? {
					key: 'Escape',
					label: 'Salir',
					run: () => {
						cancelTimer();
						pendingOpen = null;
						onExit();
					}
				}
			: {
					key: 'Escape',
					label: 'Borrar búsqueda',
					run: () => {
						pendingOpen = null;
						text = '';
						searchNow('');
					}
				}
	]);
</script>

<h1 class="mb-4 text-2xl font-semibold">{title}</h1>
<div class="mb-4 flex max-w-xl flex-col gap-1">
	<label for="list-search" class="font-medium">Buscar (código o nombre)</label>
	<input
		bind:this={input}
		bind:value={text}
		id="list-search"
		type="text"
		class={inputClass}
		use:selectOnFocus
		autocomplete="off"
		oninput={schedule}
	/>
</div>
<div bind:this={tableBox}>
	<DataTable {columns} {rows} onOpen={openRow} {page} onPage={turnPage} {emptyText} bind:selected />
</div>
