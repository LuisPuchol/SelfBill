<script lang="ts">
	import { onMount, untrack } from 'svelte';
	import { SvelteMap } from 'svelte/reactivity';
	import type { components } from '../api/schema';
	import ActionButton from '../components/ActionButton.svelte';
	import DataTable from '../components/DataTable.svelte';
	import DateField from '../components/DateField.svelte';
	import DeleteDialog from '../components/DeleteDialog.svelte';
	import { showError, showMessage } from '../components/errors.svelte';
	import { fillViewport } from '../components/fill-viewport';
	import Form from '../components/Form.svelte';
	import ScreenHeader from '../components/ScreenHeader.svelte';
	import StaticTable from '../components/StaticTable.svelte';
	import type { Column } from '../components/table';
	import { formatDate } from '../format/date';
	import { formatMoney } from '../format/money';
	import type { FormNav } from '../keyboard/form-nav';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { DeleteFlow, type DeleteOptions } from '../records/delete-flow.svelte';
	import { defaultRange, LEDGER_LIMIT, periodTotals } from './ledger';

	type Invoice = components['schemas']['InvoiceResponse'];
	type Section = NonNullable<Invoice['sections']>[number];

	let {
		today,
		loadInvoices,
		loadInvoice,
		fetchPdf,
		print,
		sendEmail,
		deletion,
		toast,
		onExit
	}: {
		/** ISO date: the period on entry ends here */
		today: string;
		/** The issued invoices between two dates, inclusive, oldest first */
		loadInvoices: (
			from: string,
			to: string
		) => Promise<{ invoices: Invoice[]; truncated: boolean }>;
		/** One invoice with its sections */
		loadInvoice: (id: number) => Promise<Invoice>;
		fetchPdf: (id: number) => Promise<Blob>;
		/** Opens the browser's print dialog for the PDF */
		print: (pdf: Blob) => Promise<void>;
		sendEmail: (id: number) => Promise<void>;
		deletion: DeleteOptions;
		toast: (message: string) => void;
		onExit: () => void;
	} = $props();

	const uid = $props.id();
	const range = untrack(() => defaultRange(today));

	let nav = $state<FormNav>();
	let from = $state(range.from);
	let to = $state(range.to);
	/** The list for `shownFrom`–`shownTo`: null while the dates are being edited. */
	let rows = $state.raw<Invoice[] | null>(null);
	let truncated = $state(false);
	let shownFrom = $state('');
	let shownTo = $state('');
	let selected = $state(0);
	/** Counts loads and edits of the dates: a list coming for an older one is dropped. */
	let loadId = 0;
	/** Where the mark goes when the rows come back after a delete. */
	let keepIndex: number | null = null;
	let busy = false;

	const marked = $derived(rows?.[selected]);
	const totals = $derived(periodTotals(rows ?? []));

	const columns: Column<Invoice>[] = [
		{ label: 'Fecha', value: (i) => formatDate(i.date ?? '') },
		{ label: 'Nº', value: (i) => i.number ?? '', align: 'right' },
		{ label: 'Cód.', value: (i) => String(i.clientCode ?? ''), align: 'right' },
		{ label: 'Cliente', value: (i) => i.clientName ?? '' },
		{ label: 'Base', value: (i) => formatMoney(i.subtotal ?? 0), align: 'right' },
		{ label: 'IVA', value: (i) => formatMoney(i.vatAmount ?? 0), align: 'right' },
		{ label: 'Rec.', value: (i) => formatMoney(i.surchargeAmount ?? 0), align: 'right' },
		{ label: 'Total', value: (i) => formatMoney(i.total ?? 0), align: 'right' }
	];
	const noteColumns: Column<Section>[] = [
		{ label: 'Fecha', value: (s) => formatDate(s.deliveryNoteDate ?? '') },
		{ label: 'Alb.', value: (s) => String(s.deliveryNoteCode ?? ''), align: 'right' },
		{ label: 'Importe', value: (s) => formatMoney(s.subtotal ?? 0), align: 'right' }
	];

	const invoices = (n: number) => (n === 1 ? '1 factura' : `${n} facturas`);

	// The fields register before this runs: the cursor starts in "Desde", selected.
	onMount(() => {
		nav?.focus('from');
		void load(from, to);
	});

	// DataTable goes back to the first row when the rows change (its effect runs first): after
	// a delete, the mark stays where it was.
	$effect(() => {
		void rows;
		untrack(() => {
			if (keepIndex === null || rows === null) return;
			if (rows.length > 0) selected = Math.min(keepIndex, rows.length - 1);
			keepIndex = null;
		});
	});

	// The panel: answers cached by invoice id, and only the latest request is shown.
	const cache = new SvelteMap<number, Invoice>();
	let detail = $state.raw<Invoice | null>(null);
	let detailId = 0;
	$effect(() => {
		const id = marked?.id;
		untrack(() => {
			const seq = ++detailId;
			if (id === undefined) {
				detail = null;
				return;
			}
			const hit = cache.get(id);
			if (hit) {
				detail = hit;
				return;
			}
			loadInvoice(id).then(
				(invoice) => {
					cache.set(id, invoice);
					if (seq === detailId) detail = invoice;
				},
				(err: unknown) => {
					if (seq === detailId) showError(err);
				}
			);
		});
	});
	const sections = $derived(detail && detail.id === marked?.id ? (detail.sections ?? []) : []);

	/** True once the list for these dates is on screen. */
	async function load(
		dateFrom: string,
		dateTo: string,
		keep: number | null = null
	): Promise<boolean> {
		const id = ++loadId;
		let result: { invoices: Invoice[]; truncated: boolean };
		try {
			result = await loadInvoices(dateFrom, dateTo);
		} catch (err) {
			if (id !== loadId) return false;
			// The list on screen may be out of date by now.
			rows = null;
			showError(err);
			return false;
		}
		if (id !== loadId) return false;
		cache.clear();
		keepIndex = keep;
		rows = result.invoices;
		truncated = result.truncated;
		shownFrom = dateFrom;
		shownTo = dateTo;
		return true;
	}

	/** Enter in "Hasta": the list, and the cursor to the table, whose keys need it out of the field. */
	async function enter(): Promise<void> {
		if ((await load(from, to)) && nav?.hasFocus()) {
			(document.activeElement as HTMLElement).blur();
		}
	}

	/** So that nothing is printed or deleted for dates other than the ones written. */
	function datesEdited(): void {
		loadId++;
		rows = null;
	}

	const flow = new DeleteFlow({
		...untrack(() => deletion),
		toast: (message) => toast(message),
		onDeleted: async () => {
			if (rows !== null) await load(shownFrom, shownTo, selected);
		},
		// It may be gone already (deleted elsewhere): the list shows what is left.
		onFailed: async () => {
			if (rows !== null) await load(shownFrom, shownTo, selected);
		}
	});

	async function printMarked(): Promise<void> {
		const row = marked;
		if (busy || row?.id === undefined) return;
		busy = true;
		try {
			await print(await fetchPdf(row.id));
		} catch {
			showMessage('No se ha podido imprimir la factura.');
		} finally {
			busy = false;
		}
	}

	async function emailMarked(): Promise<void> {
		const row = marked;
		if (busy || row?.id === undefined) return;
		busy = true;
		try {
			await sendEmail(row.id);
			toast(`Factura ${row.number} enviada por correo`);
		} catch (err) {
			showError(err);
		} finally {
			busy = false;
		}
	}

	function askDelete(): void {
		const row = marked;
		if (row?.id === undefined) return;
		void flow.ask({ id: row.id, code: row.number ?? null, name: row.clientName ?? '' });
	}

	/** From the table back to "Desde"; from the dates, out. */
	function back(): void {
		if (nav?.hasFocus()) onExit();
		else nav?.focus('from');
	}

	// The keys and the title-line buttons share these actions.
	const printAction: KeyAction = {
		key: 'F9',
		label: 'Imprimir',
		when: () => marked !== undefined,
		run: () => void printMarked()
	};
	const emailAction: KeyAction = {
		key: 'F4',
		label: 'Enviar por correo',
		when: () => marked !== undefined,
		run: () => void emailMarked()
	};
	const deleteAction: KeyAction = {
		key: 'F8',
		label: 'Borrar',
		when: () => marked !== undefined,
		run: askDelete
	};
	const backAction: KeyAction = { key: 'Escape', label: 'Volver', run: back };

	useKeys([printAction, emailAction, deleteAction, backAction]);
</script>

<ScreenHeader title="Libro de facturas">
	<ActionButton action={printAction} />
	<ActionButton action={emailAction} />
	<ActionButton action={deleteAction} />
	<ActionButton action={backAction} />
</ScreenHeader>
<Form bind:nav onLast={() => void enter()}>
	<div class="flex gap-4" oninput={datesEdited}>
		<div class="w-44"><DateField label="Desde" name="from" required bind:value={from} /></div>
		<div class="w-44"><DateField label="Hasta" name="to" required bind:value={to} /></div>
	</div>
</Form>
{#if rows !== null}
	<div class="grid grid-cols-[minmax(0,3fr)_minmax(0,2fr)] items-start gap-6">
		<!-- As tall as the window allows: the table scrolls inside it and the totals stay in sight -->
		<div class="flex flex-col" use:fillViewport>
			{#if truncated}
				<p class="shrink-0 px-3 pb-2 text-lg text-danger">
					Solo se muestran las {LEDGER_LIMIT} primeras: acorta las fechas
				</p>
			{/if}
			<DataTable
				{columns}
				{rows}
				scroll
				bind:selected
				emptyText="No hay facturas entre el {formatDate(shownFrom)} y el {formatDate(shownTo)}."
			/>
			{#if rows.length > 0}
				<p class="tabular mt-2 shrink-0 px-3 text-lg">
					{invoices(totals.count)} · Base {formatMoney(totals.subtotal)} · IVA {formatMoney(
						totals.vatAmount
					)} · Recargo {formatMoney(totals.surchargeAmount)} · Total {formatMoney(totals.total)}
				</p>
			{/if}
		</div>
		{#if marked}
			<section aria-labelledby="{uid}-marked" class="flex flex-col gap-2 border-l border-line pl-6">
				<h2 id="{uid}-marked" class="text-xl font-semibold">
					Factura {marked.number} · {formatDate(marked.date ?? '')}
				</h2>
				<p>{marked.clientName}</p>
				<StaticTable columns={noteColumns} rows={sections} scroll />
			</section>
		{/if}
	</div>
{/if}

<DeleteDialog {flow} />
