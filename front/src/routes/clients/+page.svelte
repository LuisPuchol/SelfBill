<script lang="ts">
	import { afterNavigate, goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { clientDeletion, clientRestoration } from '#lib/catalog/clients.js';
	import ListScreen from '#lib/components/ListScreen.svelte';
	import type { Column } from '#lib/components/table.js';
	import { toast } from '#lib/components/toast.svelte.js';
	import { formatDate } from '#lib/format/date.js';
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
	// opened, comes back to it. Not the deleted ones: no record opens from them.
	afterNavigate(() => {
		if (!data.deleted) rememberListUrl('clients', page.url.pathname + page.url.search);
	});

	interface Row {
		businessName: string;
		city: string;
		nif: string;
		id: number;
		code: number;
		name: string;
		deletedAt: string;
	}

	const rows = $derived<Row[]>(
		(data.result.content ?? []).map((c) => ({
			id: c.id!,
			code: c.code!,
			name: c.name ?? '',
			businessName: c.businessName ?? '',
			city: c.city ?? '',
			nif: c.nif ?? '',
			deletedAt: c.deletedAt ?? ''
		}))
	);
	const columns: Column<Row>[] = [
		{ label: 'Código', value: (r) => String(r.code), align: 'right' },
		{ label: 'Nombre', value: (r) => r.name },
		{ label: 'Razón social', value: (r) => r.businessName },
		{ label: 'Población', value: (r) => r.city },
		{ label: 'NIF', value: (r) => r.nif }
	];
	const deletedColumns: Column<Row>[] = [
		...columns,
		{ label: 'Borrado', value: (r) => formatDate(r.deletedAt) }
	];
	// The deleted ones, or back to the live ones, with what is typed in the search box.
	function toggleDeleted(text: string) {
		const href = withParams(new URL(page.url.href), {
			deleted: data.deleted ? null : 1,
			q: text || null,
			page: null
		});
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- same page, only the query changes
		void goto(href, { replace: true, reset: false });
	}
</script>

<!-- The live clients and the deleted ones are two lists, not one that changes: ListScreen reads
	what F8 does on entry. -->
{#key data.deleted}
	<ListScreen
		title={data.deleted ? 'Clientes borrados' : 'Clientes'}
		entity={data.deleted
			? { one: 'cliente borrado', many: 'clientes borrados' }
			: { one: 'cliente', many: 'clientes' }}
		columns={data.deleted ? deletedColumns : columns}
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
		onToggleDeleted={toggleDeleted}
		onOpen={(row) => {
			rememberList('clients', { href: page.url.pathname + page.url.search, openedId: row.id });
			void goto(resolve('/clients/[id]', { id: String(row.id) }));
		}}
		deletion={data.deleted
			? undefined
			: {
					options: clientDeletion,
					describe: (row) => row.name,
					code: (row) => row.code,
					toast,
					reload: () => invalidateAll(),
					onDeleted: (id) => forgetOpened('clients', id)
				}}
		restoration={data.deleted
			? {
					options: clientRestoration,
					describe: (row) => row.name,
					code: (row) => row.code,
					toast,
					reload: () => invalidateAll()
				}
			: undefined}
		onNew={() => void goto(resolve('/clients/new'))}
		onExit={() => void goto(resolve('/'))}
	/>
{/key}
