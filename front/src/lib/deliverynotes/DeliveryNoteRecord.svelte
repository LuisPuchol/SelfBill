<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { components } from '../api/schema';
	import CheckAction from '../components/CheckAction.svelte';
	import CodeField from '../components/CodeField.svelte';
	import DateField from '../components/DateField.svelte';
	import { showError, showMessage } from '../components/errors.svelte';
	import RecordScreen from '../components/RecordScreen.svelte';
	import { timeNow } from '../format/date';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';
	import { RecordForm, type Saved } from '../records/record-form.svelte';
	import { deliveryNoteDeletion } from './deletion';
	import {
		blankNote,
		comparableNote,
		formFieldPath,
		notePayload,
		timeOf,
		type NoteValues
	} from './form';
	import { invoiceBlocker } from './invoice-blocker';
	import LinesGrid from './LinesGrid.svelte';
	import type { Lookups } from './lookups';
	import Totals from './Totals.svelte';
	import { noteRates, periodOn, type Regime } from './totals';

	type ClientResponse = components['schemas']['ClientResponse'];
	type DeliveryNoteResponse = components['schemas']['DeliveryNoteResponse'];
	type DeliveryNoteRequest = components['schemas']['DeliveryNoteRequest'];
	type TaxConfigResponse = components['schemas']['TaxConfigResponse'];

	let {
		id,
		initial,
		saved,
		periods,
		lookups,
		create,
		update,
		remove,
		proposePrice,
		goToList,
		toast,
		hasProfile,
		issueInvoice,
		fetchPdf,
		print,
		guard
	}: {
		/** Null for a new note */
		id: number | null;
		/** With the codes of its client and articles already filled in */
		initial: NoteValues;
		/** The note as the API has it, for an existing one: invoice, frozen rates and format, time */
		saved?: DeliveryNoteResponse;
		/** Every VAT period, for the totals */
		periods: TaxConfigResponse[];
		/** Already holding the client and articles of `initial` */
		lookups: Lookups;
		create: (body: DeliveryNoteRequest) => Promise<DeliveryNoteResponse>;
		update: (id: number, body: DeliveryNoteRequest) => Promise<DeliveryNoteResponse>;
		remove: (id: number) => Promise<void>;
		/** The price of the client's last line of the article, or null for none */
		proposePrice: (clientId: number, articleId: number) => Promise<number | null>;
		goToList: (savedId?: number) => void | Promise<void>;
		toast: (message: string) => void;
		/** Whether the owner's data is filled in: issuing an invoice needs it */
		hasProfile: boolean;
		/** Creates and issues an invoice with just this note */
		issueInvoice: (clientId: number, noteId: number) => Promise<{ number: string }>;
		fetchPdf: (noteId: number) => Promise<Blob>;
		/** Opens the browser's print dialog for the PDF */
		print: (pdf: Blob) => Promise<void>;
		/** The page's unsaved-changes guard, which needs the form */
		guard?: Snippet<[RecordForm<NoteValues>]>;
	} = $props();

	const FORMATS = { PLAIN: 'Albarán simple', VALUED: 'Albarán valorado' } as const;

	const toSaved = (note: DeliveryNoteResponse): Saved => ({ id: note.id!, code: note.code! });

	/** The values the last payload was built from: the API numbers its line errors after them. */
	let sent: NoteValues | undefined;

	function toPayload(values: NoteValues): DeliveryNoteRequest {
		sent = values;
		// Every code has been found by now: saving checks every field first.
		const ids = {
			clientId: lookups.cachedClient(values.clientCode!)!.id!,
			articleId: (code: number) => lookups.cachedArticle(code)!.id!
		};
		// A new note takes the time it is saved at; an edit keeps its own.
		return notePayload(values, ids, saved ? timeOf(saved) : timeNow());
	}

	/** "Al guardar": unticked on every note opened or started, also the next one of a chain. */
	let printOnSave = $state(false);
	let issueOnSave = $state(false);

	// What a page does: the options are read once, when the record opens.
	// svelte-ignore state_referenced_locally
	const form: RecordForm<NoteValues> = new RecordForm<NoteValues>({
		// No duplicateCode: the number is no field to put it under, so the error dialog says it.
		entity: deliveryNoteDeletion.entity,
		id,
		initial,
		toPayload,
		describe: () => saved?.clientName ?? '',
		create: async (body) => toSaved(await create(body as DeliveryNoteRequest)),
		update: async (noteId, body) => toSaved(await update(noteId, body as DeliveryNoteRequest)),
		remove: (noteId) => remove(noteId),
		// The next note of the day's round: same date, nothing ticked.
		blank: async (): Promise<NoteValues> => {
			printOnSave = false;
			issueOnSave = false;
			return blankNote(form.values.date);
		},
		pendingForDelete: deliveryNoteDeletion.pendingForDelete,
		mapFieldPath: (path) => (sent ? formFieldPath(path, sent) : path),
		// An empty line is never sent: adding one is not a change.
		comparable: comparableNote,
		firstField: 'clientCode',
		afterSave,
		goToList,
		toast
	});

	/** In an issued invoice nothing changes: no field, no key. */
	const readOnly = $derived(saved?.invoiceStatus === 'ISSUED');

	const regimeOf = (c: ClientResponse): Regime => ({
		vatType: c.vatType!,
		surchargeType: c.surchargeType!
	});

	/** The client of the code in the field; its code is only set once it has been found. */
	const client = $derived(
		form.values.clientCode === null ? null : lookups.cachedClient(form.values.clientCode)
	);
	const sameClient = $derived(form.values.clientCode === form.saved.clientCode);
	/**
	 * The saved note's client, which the page fetched by id: its regime is the note's, as the API
	 * keeps an editable note's in step with its client. Null when it was deleted since.
	 */
	const savedClient = $derived(
		form.saved.clientCode === null ? null : lookups.cachedClient(form.saved.clientCode)
	);
	/** Something typed in Cliente: a deleted client's name no longer applies. */
	let clientTyped = $state(false);
	/** A client deleted since leaves its code empty: its name as the note has it. */
	const deletedClientName = $derived(
		saved && form.saved.clientCode === null && !clientTyped ? saved.clientName : undefined
	);
	const format = $derived(saved && sameClient ? saved.format : client?.defaultDeliveryNoteFormat);

	const period = $derived(periodOn(form.values.date, periods));
	const savedRates = $derived(
		saved && { vat: saved.vatPercentage ?? 0, surcharge: saved.surchargePercentage ?? 0 }
	);
	/**
	 * An issued note, or one still with its own client, has the rates it was saved with: a date alone
	 * changes none.
	 */
	const asSaved = $derived(saved !== undefined && (readOnly || sameClient));
	/** Otherwise the rates the API will give it for the client in the field. */
	const preview = $derived(
		asSaved || !client
			? null
			: noteRates(
					regimeOf(client),
					period,
					savedRates && { regime: savedClient && regimeOf(savedClient), rates: savedRates }
				)
	);
	const rates = $derived(asSaved ? savedRates! : (preview?.rates ?? null));
	/** A new note always takes its rates from the period. */
	const needsPeriod = $derived(saved ? (preview?.needsPeriod ?? false) : true);
	const notice = $derived(
		needsPeriod && form.values.date !== '' && period === null
			? 'No hay IVA configurado para esa fecha'
			: undefined
	);

	/** The last price, once the client is known. */
	async function offerPrice(articleCode: number): Promise<number | null> {
		const clientId = client?.id;
		const articleId = lookups.cachedArticle(articleCode)?.id;
		if (clientId === undefined || articleId === undefined) return null;
		return proposePrice(clientId, articleId);
	}

	/** Whether "Emitir factura" can be ticked, and why not. */
	const blocker = $derived(
		invoiceBlocker({ client, hasProfile, inInvoice: saved?.invoiceId != null })
	);
	/** A greyed box asks for nothing, also when it was ticked for another client. */
	const issuing = $derived(issueOnSave && blocker.enabled);

	// The keys and the boxes share these actions.
	const printBox: KeyAction = {
		key: 'F9',
		label: 'Imprimir albarán',
		when: () => !readOnly,
		run: () => {
			printOnSave = !printOnSave;
		}
	};
	const issueBox: KeyAction = {
		key: 'F3',
		label: 'Emitir factura',
		when: () => !readOnly && blocker.enabled,
		run: () => {
			issueOnSave = !issueOnSave;
		}
	};
	useKeys([printBox, issueBox]);

	/** What the boxes ask for, once the note is saved: the invoice issued goes in the toast. */
	async function afterSave(note: Saved): Promise<string | void> {
		// Read before waiting: a box ticked meanwhile is for the next save.
		const clientId = client?.id;
		const issue = issuing;
		const printNote = printOnSave;
		let issued: string | undefined;
		let issueFailure: { err: unknown } | undefined;
		if (issue && clientId !== undefined) {
			try {
				const invoice = await issueInvoice(clientId, note.id);
				issued = `Factura ${invoice.number} emitida`;
			} catch (err) {
				issueFailure = { err };
			}
		}
		let printFailed = false;
		if (printNote) {
			try {
				await print(await fetchPdf(note.id));
			} catch {
				printFailed = true;
			}
		}
		// One dialog for all that went wrong: a second one would only replace the first.
		const printMessage = 'No se ha podido imprimir el albarán.';
		if (issueFailure) {
			showError(issueFailure.err, {
				lead: 'El albarán se ha guardado, pero no se ha podido emitir la factura:',
				also: printFailed ? printMessage : undefined
			});
		} else if (printFailed) showMessage(printMessage);
		return issued;
	}

	let grid = $state<ReturnType<typeof LinesGrid>>();
	const banner = 'mb-4 rounded border-2 bg-surface p-3';
</script>

<RecordScreen
	{form}
	title={form.id === null ? 'Albarán nuevo' : `Albarán ${form.values.code}`}
	{readOnly}
	canDelete={() => saved?.invoiceId == null}
	onLast={() => (readOnly ? form.nav?.focusFirstField() : void grid?.appendLine())}
>
	{#if saved?.invoiceStatus === 'ISSUED'}
		<p class="{banner} border-issued text-issued">
			Está en la factura nº {saved.invoiceNumber}, emitida: no se puede modificar.
		</p>
	{:else if saved?.invoiceStatus === 'DRAFT'}
		<p class="{banner} border-draft text-draft">
			Está en una factura en borrador: si lo cambias, la factura se recalcula.
		</p>
	{/if}
	<!-- The totals up here, beside the note's data: the lines below grow without moving them -->
	<div class="mb-4 flex flex-wrap items-start justify-between gap-x-6 gap-y-4">
		<div>
			<div class="mb-2 flex flex-wrap items-start gap-x-6">
				<div class="flex w-96 items-start gap-3" oninput={() => (clientTyped = true)}>
					<div class="min-w-0 flex-1">
						<!-- The field reads its code only when created: a chained new note brings a new one -->
						{#key form.values}
							<!-- Only looking, nothing is asked for: Enter walks on past a deleted client's empty code -->
							<CodeField
								label="Cliente"
								name="clientCode"
								bind:value={form.values.clientCode}
								lookup={(code) => lookups.client(code).then((found) => found?.name ?? null)}
								notFoundMessage="No existe ningún cliente con ese código"
								required={!readOnly}
								error={form.errorFor('clientCode')}
								{readOnly}
								onFound={() => grid?.reproposePrices()}
							/>
						{/key}
					</div>
					{#if deletedClientName}
						<span class="mt-7 flex h-12 items-center">{deletedClientName}</span>
					{/if}
				</div>
				<div class="w-44">
					<DateField
						label="Fecha"
						name="date"
						required={!readOnly}
						bind:value={form.values.date}
						error={form.errorFor('date')}
						{readOnly}
					/>
				</div>
				{#if format}
					<p class="mt-7 flex h-12 items-center text-muted">{FORMATS[format]}</p>
				{/if}
			</div>
			<!-- Out of the Enter walk: a click or the key ticks them -->
			<div class="flex flex-wrap items-start gap-x-4 gap-y-2">
				<span class="flex h-12 items-center text-lg text-muted">Al guardar:</span>
				<CheckAction action={printBox} checked={printOnSave} />
				<!-- Room kept for the longest reason (two lines at this width): the header neither grows
					nor widens when one appears, and the lines below stay put -->
				<div class="w-96">
					<CheckAction
						action={issueBox}
						checked={issuing}
						reason={blocker.reason}
						reasonLines={2}
					/>
				</div>
			</div>
		</div>
		<Totals lines={form.values.lines} {rates} {notice} />
	</div>
	<LinesGrid
		bind:this={grid}
		bind:lines={form.values.lines}
		{readOnly}
		articleName={(code) => lookups.cachedArticle(code)?.name}
		lookupArticle={(code) => lookups.article(code).then((found) => found?.name ?? null)}
		proposePrice={offerPrice}
		errorFor={(name) => form.errorFor(name)}
	/>
</RecordScreen>
{@render guard?.(form)}
