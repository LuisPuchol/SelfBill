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
	import { formatMoney } from '../format/money';
	import type { FormNav } from '../keyboard/form-nav';
	import { isEditable, type KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { periodTotals } from './ledger';
	import { sectionColumns } from './sections';

	type ClientTotals = components['schemas']['ClientSummaryResponse'];
	type Invoice = components['schemas']['InvoiceResponse'];

	let {
		today,
		loadSummary,
		loadInvoices,
		loadInvoice,
		fetchPdf,
		print,
		onExit
	}: {
		/** ISO date: the period on entry ends here */
		today: string;
		/** What each client with issued invoices was invoiced between two dates, inclusive, by code */
		loadSummary: (from: string, to: string) => Promise<ClientTotals[]>;
		/** The issued invoices of a client in the period, oldest first */
		loadInvoices: (clientId: number, from: string, to: string) => Promise<Invoice[]>;
		/** One invoice with its sections */
		loadInvoice: (id: number) => Promise<Invoice>;
		fetchPdf: (id: number) => Promise<Blob>;
		/** Opens the browser's print dialog for the PDF */
		print: (pdf: Blob) => Promise<void>;
		onExit: () => void;
	} = $props();

	const range = untrack(() => defaultRange(today));
	const NONE: never[] = [];

	let nav = $state<FormNav>();
	let from = $state(range.from);
	let to = $state(range.to);
	/** The clients for `shownFrom`–`shownTo`: null while the dates are being edited. */
	let clients = $state.raw<ClientTotals[] | null>(null);
	let shownFrom = $state('');
	let shownTo = $state('');
	let selectedClient = $state(0);
	let selectedInvoice = $state(0);
	/** The column that answers the arrows */
	let column = $state<'clients' | 'invoices'>('clients');
	/** Counts loads and edits of the dates: a list coming for an older one is dropped. */
	let loadId = 0;
	let busy = false;

	const markedClient = $derived(clients?.[selectedClient]);

	const money = (pick: (c: ClientTotals) => number | undefined) => (c: ClientTotals) =>
		formatMoney(pick(c) ?? 0);
	const clientColumns: Column<ClientTotals>[] = [
		{ label: 'Cód.', value: (c) => String(c.clientCode ?? ''), align: 'right' },
		{ label: 'Cliente', value: (c) => c.clientName ?? '' },
		{ label: 'Fact.', value: (c) => String(c.invoiceCount ?? 0), align: 'right' },
		{ label: 'Base', value: money((c) => c.subtotal), align: 'right' },
		{ label: 'IVA', value: money((c) => c.vatAmount), align: 'right' },
		{ label: 'Recargo', value: money((c) => c.surchargeAmount), align: 'right' },
		{ label: 'Total', value: money((c) => c.total), align: 'right' }
	];
	const invoiceColumns: Column<Invoice>[] = [
		{ label: 'Fecha', value: (i) => formatDate(i.date ?? '') },
		{ label: 'Nº', value: (i) => i.number ?? '', align: 'right' },
		{ label: 'Base', value: (i) => formatMoney(i.subtotal ?? 0), align: 'right' },
		{ label: 'IVA', value: (i) => formatMoney(i.vatAmount ?? 0), align: 'right' },
		{ label: 'Recargo', value: (i) => formatMoney(i.surchargeAmount ?? 0), align: 'right' },
		{ label: 'Total', value: (i) => formatMoney(i.total ?? 0), align: 'right' }
	];

	const plural = (n: number, one: string, many: string) => (n === 1 ? `1 ${one}` : `${n} ${many}`);
	const cents = (amount: number | undefined) => Math.round((amount ?? 0) * 100);

	// The fields register before this runs: the cursor starts in "Desde", selected.
	onMount(() => {
		nav?.focus('from');
		void load(from, to);
	});

	// The invoices by client and the sections by invoice: cached until the clients load again.
	const invoicesLoader = new DetailLoader<number, Invoice[]>(
		(clientId) => loadInvoices(clientId, shownFrom, shownTo),
		showError
	);
	const notesLoader = new DetailLoader<number, Invoice>((id) => loadInvoice(id), showError);

	const invoices = $derived(invoicesLoader.get(markedClient?.clientId) ?? NONE);
	const markedInvoice = $derived(invoices[selectedInvoice]);
	const notes = $derived(notesLoader.get(markedInvoice?.id)?.sections ?? NONE);
	// Summed in cents, as floats drift
	const clientsTotal = $derived(
		(clients ?? NONE).reduce((acc, c) => acc + cents(c.total), 0) / 100
	);
	const clientsInvoices = $derived(
		(clients ?? NONE).reduce((acc, c) => acc + (c.invoiceCount ?? 0), 0)
	);
	const invoicesTotal = $derived(periodTotals(invoices).total);
	const notesTotal = $derived(notes.reduce((acc, s) => acc + cents(s.subtotal), 0) / 100);

	// The mark goes to the first invoice with the invoices of another client
	$effect(() => {
		void markedClient;
		untrack(() => {
			selectedInvoice = 0;
		});
	});

	// Both read `clients` too: after a load the same key is asked for again.
	$effect(() => {
		void clients;
		const id = markedClient?.clientId;
		untrack(() => invoicesLoader.select(id));
	});

	$effect(() => {
		void clients;
		const id = markedInvoice?.id;
		untrack(() => notesLoader.select(id));
	});

	/** True once the clients for these dates are on screen. */
	async function load(dateFrom: string, dateTo: string): Promise<boolean> {
		const id = ++loadId;
		let result: ClientTotals[];
		try {
			result = await loadSummary(dateFrom, dateTo);
		} catch (err) {
			if (id !== loadId) return false;
			// What is on screen may be out of date by now.
			clients = null;
			showError(err);
			return false;
		}
		if (id !== loadId) return false;
		invoicesLoader.clear();
		notesLoader.clear();
		shownFrom = dateFrom;
		shownTo = dateTo;
		selectedClient = 0;
		column = 'clients';
		clients = result;
		return true;
	}

	/**
	 * Enter in "Hasta": the clients, and the cursor to them, whose keys need it out of the field.
	 * With none there is no table to go to: the cursor stays, ready for other dates.
	 */
	async function enter(): Promise<void> {
		if (!(await load(from, to)) || !nav?.hasFocus()) return;
		const field = document.activeElement as HTMLInputElement;
		if (clients !== null && clients.length > 0) field.blur();
		else field.select();
	}

	/** So that nothing is shown or printed for dates other than the ones written. */
	function datesEdited(): void {
		loadId++;
		clients = null;
	}

	async function printMarked(): Promise<void> {
		const row = markedInvoice;
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

	/** Invoices to clients, clients to "Desde", and from the dates, out. */
	function back(): void {
		if (nav?.hasFocus()) onExit();
		else if (column === 'invoices') column = 'clients';
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
		label: 'Imprimir factura',
		when: () => markedInvoice !== undefined,
		run: () => void printMarked()
	};
	const backAction: KeyAction = { key: 'Escape', label: 'Volver', run: back };
	const toInvoicesAction: KeyAction = {
		key: 'Enter',
		label: 'Facturas',
		when: () => column === 'clients' && markedClient !== undefined && notTyping(),
		run: () => {
			column = 'invoices';
		}
	};

	useKeys([printAction, toInvoicesAction, backAction]);
</script>

<ScreenHeader title="Resumen por cliente">
	<ActionButton action={printAction} />
	<ActionButton action={backAction} />
</ScreenHeader>
<Form bind:nav onLast={() => void enter()}>
	<div class="flex gap-4" oninput={datesEdited}>
		<div class="w-44"><DateField label="Desde" name="from" required bind:value={from} /></div>
		<div class="w-44"><DateField label="Hasta" name="to" required bind:value={to} /></div>
	</div>
</Form>
{#if clients !== null}
	{#if clients.length === 0}
		<p class="p-6 text-muted">
			No hay facturas entre el {formatDate(shownFrom)} y el {formatDate(shownTo)}.
		</p>
	{:else}
		<!-- As tall as the window allows: each table scrolls inside its column -->
		<div class="grid grid-cols-[minmax(0,6fr)_minmax(0,5fr)_minmax(0,3fr)] items-start gap-6">
			<!-- A click inside a column makes it the active one -->
			<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
			<div class="flex flex-col" use:fillViewport onclick={() => (column = 'clients')}>
				<DataTable
					columns={clientColumns}
					rows={clients}
					scroll
					active={column === 'clients'}
					bind:selected={selectedClient}
				/>
				<p class="tabular mt-2 shrink-0 px-3 text-lg">
					{plural(clients.length, 'cliente', 'clientes')} · {plural(
						clientsInvoices,
						'factura',
						'facturas'
					)} · {formatMoney(clientsTotal)}
				</p>
			</div>
			<!-- svelte-ignore a11y_click_events_have_key_events, a11y_no_static_element_interactions -->
			<div
				class="flex flex-col border-l border-line pl-6"
				use:fillViewport
				onclick={() => (column = 'invoices')}
			>
				<!-- No empty text: until the answer arrives there are no invoices yet -->
				<DataTable
					columns={invoiceColumns}
					rows={invoices}
					scroll
					active={column === 'invoices'}
					bind:selected={selectedInvoice}
					emptyText=""
				/>
				{#if invoices.length > 0}
					<p class="tabular mt-2 shrink-0 px-3 text-lg">
						{plural(invoices.length, 'factura', 'facturas')} · {formatMoney(invoicesTotal)}
					</p>
				{/if}
			</div>
			<div class="flex flex-col border-l border-line pl-6" use:fillViewport>
				<StaticTable columns={sectionColumns} rows={notes} emptyText="" scroll />
				{#if notes.length > 0}
					<p class="tabular mt-2 shrink-0 px-3 text-lg">
						{plural(notes.length, 'albarán', 'albaranes')} · {formatMoney(notesTotal)}
					</p>
				{/if}
			</div>
		</div>
	{/if}
{/if}
