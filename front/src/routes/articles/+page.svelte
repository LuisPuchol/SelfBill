<script lang="ts">
	import { afterNavigate, goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { articleDeletion, articleRestoration } from '#lib/catalog/articles.js';
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
		if (!data.deleted) rememberListUrl('articles', page.url.pathname + page.url.search);
	});

	interface Row {
		id: number;
		code: number;
		name: string;
		deletedAt: string;
	}

	const rows = $derived<Row[]>(
		(data.result.content ?? []).map((a) => ({
			id: a.id!,
			code: a.code!,
			name: a.name ?? '',
			deletedAt: a.deletedAt ?? ''
		}))
	);
	const columns: Column<Row>[] = [
		{ label: 'Código', value: (r) => String(r.code), align: 'right' },
		{ label: 'Nombre', value: (r) => r.name }
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

<!-- The live articles and the deleted ones are two lists, not one that changes: ListScreen reads
	what F8 does on entry. -->
{#key data.deleted}
	<ListScreen
		title={data.deleted ? 'Artículos borrados' : 'Artículos'}
		entity={data.deleted
			? { one: 'artículo borrado', many: 'artículos borrados' }
			: { one: 'artículo', many: 'artículos' }}
		columns={data.deleted ? deletedColumns : columns}
		{rows}
		page={{
			number: data.result.page?.number ?? 0,
			totalPages: data.result.page?.totalPages ?? 1
		}}
		search={data.q}
		selectId={recallList('articles')?.openedId}
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
			rememberList('articles', { href: page.url.pathname + page.url.search, openedId: row.id });
			void goto(resolve('/articles/[id]', { id: String(row.id) }));
		}}
		deletion={data.deleted
			? undefined
			: {
					options: articleDeletion,
					describe: (row) => row.name,
					code: (row) => row.code,
					toast,
					reload: () => invalidateAll(),
					onDeleted: (id) => forgetOpened('articles', id)
				}}
		restoration={data.deleted
			? {
					options: articleRestoration,
					describe: (row) => row.name,
					code: (row) => row.code,
					toast,
					reload: () => invalidateAll()
				}
			: undefined}
		onNew={() => void goto(resolve('/articles/new'))}
		onExit={() => void goto(resolve('/'))}
	/>
{/key}
