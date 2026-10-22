<script lang="ts">
	import { afterNavigate, goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ListScreen from '#lib/components/ListScreen.svelte';
	import type { Column } from '#lib/components/table.js';
	import { toast } from '#lib/components/toast.svelte.js';
	import { deliveryNoteDeletion } from '#lib/deliverynotes/deletion.js';
	import { emptyHint, stateText } from '#lib/deliverynotes/list.js';
	import { formatDate } from '#lib/format/date.js';
	import { formatMoney } from '#lib/format/money.js';
	import {
		forgetOpened,
		recallList,
		rememberList,
		rememberListUrl
	} from '#lib/records/list-memory.js';
	import { withParams } from '#lib/url.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	// The list as it is on screen, also after a search, the filter or a page turn: Esc from a
	// note, new or opened, comes back to it.
	afterNavigate(() => rememberListUrl('delivery-notes', page.url.pathname + page.url.search));

	interface Row {
		id: number;
		code: number;
		date: string;
		clientName: string;
		total: number;
		invoiceId: number | null;
		state: string;
	}

	const rows = $derived<Row[]>(
		(data.result.content ?? []).map((n) => ({
			id: n.id!,
			code: n.code!,
			date: n.date ?? '',
			clientName: n.clientName ?? '',
			total: n.total ?? 0,
			invoiceId: n.invoiceId ?? null,
			state: stateText(n)
		}))
	);
	// Only the pending notes, or all of them: the filter lives in the URL.
	function togglePending() {
		const href = withParams(new URL(page.url.href), {
			pending: data.pending ? null : 1,
			page: null
		});
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- same page, only the query changes
		void goto(href, { replace: true, reset: false });
	}
	const columns: Column<Row>[] = [
		{ label: 'Nº', value: (r) => String(r.code), align: 'right' },
		{ label: 'Fecha', value: (r) => formatDate(r.date) },
		{ label: 'Cliente', value: (r) => r.clientName },
		{ label: 'Importe', value: (r) => formatMoney(r.total), align: 'right' },
		{ label: 'Estado', value: (r) => r.state }
	];
</script>

<ListScreen
	title="Albaranes"
	entity={{ one: 'albarán', many: 'albaranes' }}
	{columns}
	{rows}
	page={{
		number: data.result.page?.number ?? 0,
		totalPages: data.result.page?.totalPages ?? 1
	}}
	search={data.q}
	selectId={recallList('delivery-notes')?.openedId}
	rowId={(row) => row.id}
	emptyHint={emptyHint(data.pending)}
	onSearch={(text) =>
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- same page, only the query changes
		goto(withParams(new URL(page.url.href), { q: text || null, page: null }), {
			replace: true,
			reset: false
		})}
	onPage={(n) =>
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- same page, only the query changes
		goto(withParams(new URL(page.url.href), { page: n || null }), { reset: false })}
	onOpen={(row) => {
		rememberList('delivery-notes', {
			href: page.url.pathname + page.url.search,
			openedId: row.id
		});
		void goto(resolve('/delivery-notes/[id]', { id: String(row.id) }));
	}}
	deletion={{
		options: deliveryNoteDeletion,
		describe: (row) => row.clientName,
		code: (row) => row.code,
		toast,
		reload: () => invalidateAll(),
		onDeleted: (id) => forgetOpened('delivery-notes', id),
		canDelete: (row) => row.invoiceId == null
	}}
	extraActions={[
		{
			action: {
				key: 'F3',
				label: data.pending ? 'Ver todos' : 'Solo pendientes',
				run: togglePending
			}
		}
	]}
	onNew={() => void goto(resolve('/delivery-notes/new'))}
	onExit={() => void goto(resolve('/'))}
/>
