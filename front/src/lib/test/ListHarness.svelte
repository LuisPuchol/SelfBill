<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import ListScreen from '../components/ListScreen.svelte';
	import type { Column } from '../components/table';

	interface Row {
		id: number;
		code: number;
		name: string;
	}

	const all: Row[] = [
		{ id: 11, code: 1, name: 'Frutas Paco' },
		{ id: 12, code: 2, name: 'Bar Pepe' },
		{ id: 13, code: 3, name: 'Hotel Sol' }
	];

	const columns: Column<Row>[] = [
		{ label: 'Código', value: (r) => String(r.code), align: 'right' },
		{ label: 'Nombre', value: (r) => r.name }
	];

	let {
		title = 'Clientes',
		entity = { one: 'cliente', many: 'clientes' },
		rows = all,
		search = '',
		selectId,
		page = { number: 0, totalPages: 1 },
		onSearch,
		onOpen,
		onNew,
		onExit,
		onPage = () => {},
		onToggleDeleted,
		deletion,
		restoration,
		extraActions
	}: {
		title?: string;
		entity?: { one: string; many: string };
		rows?: Row[];
		search?: string;
		selectId?: number;
		page?: { number: number; totalPages: number };
		onSearch: (text: string) => void;
		onOpen: (row: Row) => void;
		onNew: () => void;
		onExit: () => void;
		onPage?: (n: number) => void;
		onToggleDeleted?: (search: string) => void;
		deletion?: ComponentProps<typeof ListScreen<Row>>['deletion'];
		restoration?: ComponentProps<typeof ListScreen<Row>>['restoration'];
		extraActions?: ComponentProps<typeof ListScreen<Row>>['extraActions'];
	} = $props();
</script>

<KeyboardRoot />
<ListScreen
	{title}
	{entity}
	{columns}
	{rows}
	{page}
	{search}
	{selectId}
	rowId={(row) => row.id}
	{onSearch}
	{onPage}
	{onOpen}
	{onNew}
	{onExit}
	{onToggleDeleted}
	{deletion}
	{restoration}
	{extraActions}
/>
<ErrorDialog />
<KeyBar />
