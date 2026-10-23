<script lang="ts">
	import { onMount, untrack } from 'svelte';
	import { classifyError } from '../api/classify';
	import ActionButton from '../components/ActionButton.svelte';
	import DataTable from '../components/DataTable.svelte';
	import DateField from '../components/DateField.svelte';
	import Dialog from '../components/Dialog.svelte';
	import { showError } from '../components/errors.svelte';
	import { fillViewport } from '../components/fill-viewport';
	import Form from '../components/Form.svelte';
	import ScreenHeader from '../components/ScreenHeader.svelte';
	import StaticTable from '../components/StaticTable.svelte';
	import type { Column } from '../components/table';
	import { formatDate } from '../format/date';
	import { formatMoney } from '../format/money';
	import { formatPercent } from '../format/number';
	import type { FormNav } from '../keyboard/form-nav';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import {
		blockNotice,
		blockReason,
		issuedSummary,
		monthLong,
		monthShort,
		type PendingInvoice
	} from './billing';
	import IssueConfirm, { type IssueQuestion } from './IssueConfirm.svelte';
	import IssueSummary, { type IssueReport } from './IssueSummary.svelte';

	type PendingNote = NonNullable<PendingInvoice['deliveryNotes']>[number];

	let {
		today,
		hasProfile,
		loadPending,
		issue,
		toast,
		onExit
	}: {
		/** ISO date: the default "Facturar hasta", and the date the invoices get */
		today: string;
		/** Whether the owner's data is filled in: issuing needs it */
		hasProfile: boolean;
		/** The future invoices of the notes pending up to `dateTo`, inclusive */
		loadPending: (dateTo: string) => Promise<PendingInvoice[]>;
		/** Creates and issues one invoice with these notes */
		issue: (clientId: number, deliveryNoteIds: number[]) => Promise<{ number: string }>;
		toast: (message: string) => void;
		onExit: () => void;
	} = $props();

	const uid = $props.id();

	let nav = $state<FormNav>();
	let dateTo = $state(untrack(() => today));
	/** The list for `shownFor`: null until Enter, and again as soon as the date is edited. */
	let pending = $state.raw<PendingInvoice[] | null>(null);
	let shownFor = $state('');
	let selected = $state(0);
	/** Counts loads and edits of the date: a list coming for an older one is dropped. */
	let loadId = 0;
	/** Where the mark goes when the rows come back after F3. */
	let keepIndex: number | null = null;
	/** An invoice is on its way: "+" and F3 do nothing until it is back. */
	let busy = false;
	let question = $state<IssueQuestion | null>(null);
	let progress = $state<{ current: number; total: number } | null>(null);
	let report = $state<IssueReport | null>(null);

	const issuable = (p: PendingInvoice) => blockReason(p) === null;
	const marked = $derived(pending?.[selected]);
	const markedReason = $derived(marked && blockReason(marked));

	const columns: Column<PendingInvoice>[] = [
		{ label: 'Cód.', value: (p) => String(p.clientCode ?? ''), align: 'right' },
		{ label: 'Cliente', value: (p) => p.clientName ?? '' },
		{ label: 'Mes', value: (p) => monthShort(p.month ?? '') },
		{ label: 'Alb.', value: (p) => String(p.deliveryNotes?.length ?? 0), align: 'right' },
		{ label: 'Importe', value: (p) => formatMoney(p.subtotal ?? 0), align: 'right' },
		{ label: 'Aviso', value: blockNotice }
	];
	const noteColumns: Column<PendingNote>[] = [
		{ label: 'Fecha', value: (n) => formatDate(n.date ?? '') },
		{ label: 'Alb.', value: (n) => String(n.code ?? ''), align: 'right' },
		{ label: 'Importe', value: (n) => formatMoney(n.total ?? 0), align: 'right' }
	];

	const invoices = (n: number) => (n === 1 ? '1 factura' : `${n} facturas`);
	/** Every row, also those that cannot be issued; summed in cents, as floats drift. */
	const sum = $derived(
		(pending ?? []).reduce((cents, p) => cents + Math.round((p.subtotal ?? 0) * 100), 0) / 100
	);

	/** "Base … · IVA 4 % … · Recargo 0,5 % … · Total …", the surcharge only when it has one. */
	function amounts(p: PendingInvoice): string {
		const surcharge = p.surchargePercentage ?? 0;
		return [
			`Base ${formatMoney(p.subtotal ?? 0)}`,
			`IVA ${formatPercent(p.vatPercentage ?? 0)} ${formatMoney(p.vatAmount ?? 0)}`,
			...(surcharge > 0
				? [`Recargo ${formatPercent(surcharge)} ${formatMoney(p.surchargeAmount ?? 0)}`]
				: []),
			`Total ${formatMoney(p.total ?? 0)}`
		].join(' · ');
	}

	// The fields register before this runs: the cursor starts in the date, selected.
	onMount(() => nav?.focus('dateTo'));

	// DataTable goes back to the first row when the rows change (its effect runs first): after
	// F3, the mark stays where it was.
	$effect(() => {
		void pending;
		untrack(() => {
			if (keepIndex === null || pending === null) return;
			if (pending.length > 0) selected = Math.min(keepIndex, pending.length - 1);
			keepIndex = null;
		});
	});

	/** True once the list for `date` is on screen. */
	async function load(date: string, keep: number | null = null): Promise<boolean> {
		const id = ++loadId;
		let rows: PendingInvoice[];
		try {
			rows = await loadPending(date);
		} catch (err) {
			if (id !== loadId) return false;
			// The list on screen may be out of date by now.
			pending = null;
			showError(err);
			return false;
		}
		if (id !== loadId) return false;
		keepIndex = keep;
		pending = rows;
		shownFor = date;
		return true;
	}

	/** Enter in the date: the list, and the cursor to the table, whose keys need it out of the field. */
	async function enter(): Promise<void> {
		if ((await load(dateTo)) && nav?.hasFocus()) {
			(document.activeElement as HTMLElement).blur();
		}
	}

	/** So that no list is issued for a date other than the one written. */
	function dateEdited(): void {
		loadId++;
		pending = null;
	}

	/** After issuing: the list again for its date, unless the date was edited meanwhile. */
	async function reload(keep: number | null = null): Promise<void> {
		if (pending !== null) await load(shownFor, keep);
	}

	/** What was issued may be on screen still: the list waits for the next Enter. */
	function connectionLost(err: unknown): void {
		loadId++;
		pending = null;
		busy = false;
		showError(err);
	}

	const issueRow = (row: PendingInvoice) =>
		issue(
			row.clientId!,
			(row.deliveryNotes ?? []).map((n) => n.id!)
		);

	function askAll(): void {
		if (busy || pending === null) return;
		const rows = pending;
		const count = rows.filter(issuable).length;
		const left = rows.length - count;
		question = {
			key: 'NumpadAdd',
			title: count === 1 ? '¿Emitir 1 factura?' : `¿Emitir ${count} facturas?`,
			lines: [
				`Con fecha de hoy, ${formatDate(today)}.`,
				...(left === 0
					? []
					: left === 1
						? ['Se deja sin facturar 1 fila con aviso.']
						: [`Se dejan sin facturar ${left} filas con aviso.`])
			],
			confirm: () => {
				question = null;
				void issueAll(rows);
			}
		};
	}

	/** One by one in the list's order: a refused row does not stop the rest, a lost connection does. */
	async function issueAll(rows: PendingInvoice[]): Promise<void> {
		busy = true;
		/** Why each row is left without its invoice, by position; null for the ones to issue. */
		const reasons = rows.map(blockReason);
		const total = reasons.filter((reason) => reason === null).length;
		const numbers: string[] = [];
		let current = 0;
		let lost: { err: unknown } | undefined;
		for (const [i, row] of rows.entries()) {
			if (reasons[i] !== null) continue;
			progress = { current: ++current, total };
			try {
				numbers.push((await issueRow(row)).number);
			} catch (err) {
				const view = classifyError(err);
				if (view.kind === 'connection') {
					lost = { err };
					break;
				}
				reasons[i] = view.kind === 'business' ? view.message : 'Error inesperado';
			}
		}
		progress = null;
		if (lost) return connectionLost(lost.err);
		await reload();
		busy = false;
		const notIssued = rows.flatMap((row, i) => {
			const reason = reasons[i];
			return reason === null
				? []
				: [{ name: row.clientName ?? '', month: monthShort(row.month ?? ''), reason }];
		});
		const issued = issuedSummary(numbers);
		if (notIssued.length === 0) toast(issued);
		else report = { issued, notIssued };
	}

	function askOne(): void {
		const row = marked;
		if (busy || row === undefined) return;
		const index = selected;
		const notes = row.deliveryNotes?.length ?? 0;
		question = {
			key: 'F3',
			title: `¿Emitir la factura de ${row.clientName}?`,
			lines: [
				[
					monthLong(row.month ?? ''),
					notes === 1 ? '1 albarán' : `${notes} albaranes`,
					formatMoney(row.total ?? 0)
				].join(' · ')
			],
			confirm: () => {
				question = null;
				void issueOne(row, index);
			}
		};
	}

	async function issueOne(row: PendingInvoice, index: number): Promise<void> {
		busy = true;
		try {
			toast(`Factura ${(await issueRow(row)).number} emitida`);
		} catch (err) {
			if (classifyError(err).kind === 'connection') return connectionLost(err);
			showError(err);
		}
		// Also after a refusal: the row may have changed meanwhile, in another tab.
		await reload(index);
		busy = false;
	}

	/** From the table back to the date; from the date, out. */
	function back(): void {
		if (nav?.hasFocus()) onExit();
		else nav?.focus('dateTo');
	}

	// The keys and the title-line buttons share these actions.
	const issueAllAction: KeyAction = {
		key: 'NumpadAdd',
		label: 'Facturar todos',
		when: () => hasProfile && (pending ?? []).some(issuable),
		run: askAll
	};
	const issueOneAction: KeyAction = {
		key: 'F3',
		label: 'Facturar este',
		when: () => hasProfile && marked !== undefined && issuable(marked),
		run: askOne
	};
	const backAction: KeyAction = { key: 'Escape', label: 'Volver', run: back };

	useKeys([
		issueAllAction,
		// Greyed out, "+" must not end up typed in the date.
		{ key: 'NumpadAdd', label: '', hidden: true, run: () => {} },
		issueOneAction,
		backAction
	]);
</script>

<ScreenHeader title="Facturar">
	{#if !hasProfile}
		<span class="self-center text-lg text-danger">Faltan los datos del titular (Ajustes)</span>
	{/if}
	<ActionButton action={issueAllAction} variant="primary" />
	<ActionButton action={issueOneAction} />
	<ActionButton action={backAction} />
</ScreenHeader>
<Form bind:nav onLast={() => void enter()}>
	<div class="w-44" oninput={dateEdited}>
		<DateField label="Facturar hasta" name="dateTo" required bind:value={dateTo} />
	</div>
</Form>
{#if pending !== null}
	<div class="grid grid-cols-[minmax(0,3fr)_minmax(0,2fr)] items-start gap-6">
		<!-- As tall as the window allows: the table scrolls inside it and the count stays in sight -->
		<div class="flex flex-col" use:fillViewport>
			<DataTable
				{columns}
				rows={pending}
				scroll
				bind:selected
				emptyText="No hay albaranes pendientes hasta el {formatDate(shownFor)}."
			/>
			{#if pending.length > 0}
				<p class="tabular mt-2 shrink-0 px-3 text-lg">
					{invoices(pending.length)} · {formatMoney(sum)}
				</p>
			{/if}
		</div>
		{#if marked}
			<section aria-labelledby="{uid}-marked" class="flex flex-col gap-2 border-l border-line pl-6">
				<h2 id="{uid}-marked" class="text-xl font-semibold">
					{marked.clientName} · {monthLong(marked.month ?? '')}
				</h2>
				<p class="tabular">{amounts(marked)}</p>
				{#if markedReason}
					<p class="text-danger">{markedReason}</p>
				{/if}
				<StaticTable columns={noteColumns} rows={marked.deliveryNotes ?? []} scroll />
			</section>
		{/if}
	</div>
{/if}

<IssueConfirm {question} onCancel={() => (question = null)} />
<!-- No keys: the batch cannot be stopped halfway -->
<Dialog bind:open={() => progress !== null, () => {}} title="Emitiendo facturas…" actions={[]}>
	<p class="tabular">{progress?.current} de {progress?.total}</p>
</Dialog>
<IssueSummary {report} onClose={() => (report = null)} />
