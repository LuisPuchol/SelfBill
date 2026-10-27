<script lang="ts">
	import { onMount, untrack } from 'svelte';
	import { on } from 'svelte/events';
	import { createSubscriber } from 'svelte/reactivity';
	import type { components } from '../api/schema';
	import ActionButton from '../components/ActionButton.svelte';
	import DataTable from '../components/DataTable.svelte';
	import DateField from '../components/DateField.svelte';
	import { DetailLoader } from '../components/detail-loader.svelte';
	import { showError, showMessage } from '../components/errors.svelte';
	import { fillViewport } from '../components/fill-viewport';
	import Form from '../components/Form.svelte';
	import ScreenHeader from '../components/ScreenHeader.svelte';
	import StaticTable from '../components/StaticTable.svelte';
	import type { Column } from '../components/table';
	import { defaultRange, formatDate } from '../format/date';
	import { formatDecimal } from '../format/number';
	import type { FormNav } from '../keyboard/form-nav';
	import { isEditable, type KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { traceabilityReport } from './report';

	type Article = components['schemas']['TraceabilityArticleResponse'];
	type Lot = components['schemas']['TraceabilityLotResponse'];
	type Delivery = components['schemas']['TraceabilityDeliveryResponse'];

	let {
		today,
		loadArticles,
		loadLots,
		loadDeliveries,
		print,
		onExit
	}: {
		/** ISO date: the period on entry ends here */
		today: string;
		/** The articles sold between two dates, inclusive, by code */
		loadArticles: (from: string, to: string) => Promise<Article[]>;
		/** The lots of an article in the period, by code */
		loadLots: (articleId: number, from: string, to: string) => Promise<Lot[]>;
		/** The delivery notes with that article and lot in the period, by date */
		loadDeliveries: (
			articleId: number,
			code: string,
			from: string,
			to: string
		) => Promise<Delivery[]>;
		/** Opens the browser's print dialog for the listing */
		print: (doc: Blob) => Promise<void>;
		onExit: () => void;
	} = $props();

	const range = untrack(() => defaultRange(today));
	const NONE: never[] = [];

	let nav = $state<FormNav>();
	let from = $state(range.from);
	let to = $state(range.to);
	/** The articles for `shownFrom`–`shownTo`: null while the dates are being edited. */
	let articles = $state.raw<Article[] | null>(null);
	let shownFrom = $state('');
	let shownTo = $state('');
	let selectedArticle = $state(0);
	let selectedLot = $state(0);
	/** The column that answers the arrows */
	let column = $state<'articles' | 'lots'>('articles');
	/** Counts loads and edits of the dates: a list coming for an older one is dropped. */
	let loadId = 0;
	let busy = false;

	const markedArticle = $derived(articles?.[selectedArticle]);

	const articleColumns: Column<Article>[] = [
		{ label: 'Cód.', value: (a) => String(a.articleCode ?? ''), align: 'right' },
		{ label: 'Artículo', value: (a) => a.articleName ?? '' }
	];
	const lotColumns: Column<Lot>[] = [
		{ label: 'Lote', value: (l) => l.code ?? '' },
		{ label: 'Cantidad', value: (l) => formatDecimal(l.quantity ?? 0, 2), align: 'right' }
	];
	const deliveryColumns: Column<Delivery>[] = [
		{ label: 'Fecha', value: (d) => formatDate(d.date ?? '') },
		{ label: 'Cód.', value: (d) => String(d.clientCode ?? ''), align: 'right' },
		{ label: 'Cliente', value: (d) => d.clientName ?? '' },
		{ label: 'Alb.', value: (d) => String(d.deliveryNoteCode ?? ''), align: 'right' },
		{ label: 'Cantidad', value: (d) => formatDecimal(d.quantity ?? 0, 2), align: 'right' }
	];

	const notes = (n: number) => (n === 1 ? '1 albarán' : `${n} albaranes`);

	// The fields register before this runs: the cursor starts in "Desde", selected.
	onMount(() => {
		nav?.focus('from');
		void load(from, to);
	});

	// The lots by article and the notes by article and lot: cached until the articles load again.
	const lotsLoader = new DetailLoader<number, Lot[]>(
		(articleId) => loadLots(articleId, shownFrom, shownTo),
		showError
	);
	const deliveriesLoader = new DetailLoader<string, Delivery[]>((key) => {
		const cut = key.indexOf('|');
		return loadDeliveries(Number(key.slice(0, cut)), key.slice(cut + 1), shownFrom, shownTo);
	}, showError);

	const lots = $derived(lotsLoader.get(markedArticle?.articleId) ?? NONE);
	const markedLot = $derived(lots[selectedLot]);
	const markedKey = $derived(
		markedArticle?.articleId !== undefined && markedLot?.code !== undefined
			? `${markedArticle.articleId}|${markedLot.code}`
			: undefined
	);
	const deliveriesShown = $derived(deliveriesLoader.get(markedKey));
	const deliveries = $derived(deliveriesShown ?? NONE);
	/** The notes of the marked lot are on screen: what F9 prints. */
	const ready = $derived(markedLot !== undefined && deliveriesShown !== undefined);
	// Summed in hundredths, as floats drift
	const total = $derived(
		deliveries.reduce((acc, d) => acc + Math.round((d.quantity ?? 0) * 100), 0) / 100
	);

	// The mark goes to the first lot with the lots of another article
	$effect(() => {
		void markedArticle;
		untrack(() => {
			selectedLot = 0;
		});
	});

	// Both read `articles` too: after a load the same key is asked for again.
	$effect(() => {
		void articles;
		const id = markedArticle?.articleId;
		untrack(() => lotsLoader.select(id));
	});

	$effect(() => {
		void articles;
		const key = markedKey;
		untrack(() => deliveriesLoader.select(key));
	});

	/** True once the articles for these dates are on screen. */
	async function load(dateFrom: string, dateTo: string): Promise<boolean> {
		const id = ++loadId;
		let result: Article[];
		try {
			result = await loadArticles(dateFrom, dateTo);
		} catch (err) {
			if (id !== loadId) return false;
			// What is on screen may be out of date by now.
			articles = null;
			showError(err);
			return false;
		}
		if (id !== loadId) return false;
		lotsLoader.clear();
		deliveriesLoader.clear();
		shownFrom = dateFrom;
		shownTo = dateTo;
		selectedArticle = 0;
		column = 'articles';
		articles = result;
		return true;
	}

	/**
	 * Enter in "Hasta": the articles, and the cursor to them, whose keys need it out of the field.
	 * With none there is no table to go to: the cursor stays, ready for other dates.
	 */
	async function enter(): Promise<void> {
		if (!(await load(from, to)) || !nav?.hasFocus()) return;
		const field = document.activeElement as HTMLInputElement;
		if (articles !== null && articles.length > 0) field.blur();
		else field.select();
	}

	/** So that nothing is shown or printed for dates other than the ones written. */
	function datesEdited(): void {
		loadId++;
		articles = null;
	}

	async function printMarked(): Promise<void> {
		const article = markedArticle;
		const lot = markedLot;
		if (busy || !ready || !article || !lot) return;
		busy = true;
		try {
			const html = traceabilityReport({
				code: lot.code ?? '',
				articleName: article.articleName ?? '',
				from: shownFrom,
				to: shownTo,
				rows: deliveries.map((d) => ({
					date: d.date ?? '',
					clientCode: d.clientCode ?? 0,
					clientName: d.clientName ?? '',
					deliveryNoteCode: d.deliveryNoteCode ?? 0,
					quantity: d.quantity ?? 0
				}))
			});
			await print(new Blob([html], { type: 'text/html' }));
		} catch {
			showMessage('No se ha podido imprimir el listado.');
		} finally {
			busy = false;
		}
	}

	/** Lots to articles, articles to "Desde", and from the dates, out. */
	function back(): void {
		if (nav?.hasFocus()) onExit();
		else if (column === 'lots') column = 'articles';
		else nav?.focus('from');
	}

	// `when` runs inside a $derived in the key bar: re-evaluate it as the focus moves.
	const subscribe = createSubscriber((update) => {
		const off = [on(document, 'focusin', update), on(document, 'focusout', update)];
		return () => off.forEach((remove) => remove());
	});
	/** Enter belongs to a focused field, button or link. */
	const notTyping = () => {
		subscribe();
		const focused = document.activeElement;
		return !isEditable(focused) && focused?.tagName !== 'BUTTON' && focused?.tagName !== 'A';
	};

	// The keys and the title-line buttons share these actions.
	const printAction: KeyAction = {
		key: 'F9',
		label: 'Imprimir',
		when: () => ready,
		run: () => void printMarked()
	};
	const backAction: KeyAction = { key: 'Escape', label: 'Volver', run: back };
	const toLotsAction: KeyAction = {
		key: 'Enter',
		label: 'Lotes',
		when: () => column === 'articles' && markedArticle !== undefined && notTyping(),
		run: () => {
			column = 'lots';
		}
	};

	useKeys([printAction, toLotsAction, backAction]);
</script>

<ScreenHeader title="Trazabilidad">
	<ActionButton action={printAction} />
	<ActionButton action={backAction} />
</ScreenHeader>
<Form bind:nav onLast={() => void enter()}>
	<div class="flex gap-4" oninput={datesEdited}>
		<div class="w-44"><DateField label="Desde" name="from" required bind:value={from} /></div>
		<div class="w-44"><DateField label="Hasta" name="to" required bind:value={to} /></div>
	</div>
</Form>
{#if articles !== null}
	{#if articles.length === 0}
		<p class="p-6 text-muted">
			No se ha vendido nada entre el {formatDate(shownFrom)} y el {formatDate(shownTo)}.
		</p>
	{:else}
		<!-- As tall as the window allows: each table scrolls inside its column -->
		<div class="grid grid-cols-[minmax(0,4fr)_minmax(0,3fr)_minmax(0,6fr)] items-start gap-6">
			<!-- A click inside a column makes it the active one -->
			<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
			<div class="flex flex-col" use:fillViewport onclick={() => (column = 'articles')}>
				<DataTable
					columns={articleColumns}
					rows={articles}
					scroll
					active={column === 'articles'}
					bind:selected={selectedArticle}
				/>
			</div>
			<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
			<div
				class="flex flex-col border-l border-line pl-6"
				use:fillViewport
				onclick={() => (column = 'lots')}
			>
				<!-- No empty text: until the answer arrives there are no lots yet -->
				<DataTable
					columns={lotColumns}
					rows={lots}
					scroll
					active={column === 'lots'}
					bind:selected={selectedLot}
					emptyText=""
				/>
			</div>
			<div class="flex flex-col border-l border-line pl-6" use:fillViewport>
				<StaticTable columns={deliveryColumns} rows={deliveries} emptyText="" scroll />
				{#if deliveries.length > 0}
					<p class="tabular mt-2 shrink-0 px-3 text-lg">
						{notes(deliveries.length)} · {formatDecimal(total, 2)}
					</p>
				{/if}
			</div>
		</div>
	{/if}
{/if}
