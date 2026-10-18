<script lang="ts">
	import { afterNavigate, goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { clientDeletion } from '#lib/catalog/clients.js';
	import ListScreen from '#lib/components/ListScreen.svelte';
	import type { Column } from '#lib/components/table.js';
	import { toast } from '#lib/components/toast.svelte.js';
	import {
		forgetOpened,
		recallList,
		rememberList,
		rememberListUrl
	} from '#lib/records/list-memory.js';
	import { withParams } from '#lib/url.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	// The list as it is on screen, also after a search or a page turn: Esc from a record, new or
	// opened, comes back to it.
	afterNavigate(() => rememberListUrl('clients', page.url.pathname + page.url.search));

	interface Row {
		businessName: string;
		city: string;
		nif: string;
		id: number;
		code: number;
		name: string;
	}

	const rows = $derived<Row[]>(
		(data.result.content ?? []).map((c) => ({
			id: c.id!,
			code: c.code!,
			name: c.name ?? '',
			businessName: c.businessName ?? '',
			city: c.city ?? '',
			nif: c.nif ?? ''
		}))
	);
	const columns: Column<Row>[] = [
		{ label: 'Código', value: (r) => String(r.code), align: 'right' },
		{ label: 'Nombre', value: (r) => r.name },
		{ label: 'Razón social', value: (r) => r.businessName },
		{ label: 'Población', value: (r) => r.city },
		{ label: 'NIF', value: (r) => r.nif }
	];
</script>

<ListScreen
	title="Clientes"
	entity={{ one: 'cliente', many: 'clientes' }}
	{columns}
	{rows}
	page={{
		number: data.result.page?.number ?? 0,
		totalPages: data.result.page?.totalPages ?? 1
	}}
	search={data.q}
	selectId={recallList('clients')?.openedId}
	rowId={(row) => row.id}
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
		rememberList('clients', { href: page.url.pathname + page.url.search, openedId: row.id });
		void goto(resolve('/clients/[id]', { id: String(row.id) }));
	}}
	deletion={{
		options: clientDeletion,
		describe: (row) => row.name,
		code: (row) => row.code,
		toast,
		reload: () => invalidateAll(),
		onDeleted: (id) => forgetOpened('clients', id)
	}}
	onNew={() => void goto(resolve('/clients/new'))}
	onExit={() => void goto(resolve('/'))}
/>
