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
		rows = all,
		search = '',
		selectId,
		page = { number: 0, totalPages: 1 },
		onSearch,
		onOpen,
		onNew,
		onExit,
		onPage = () => {},
		deletion
	}: {
		rows?: Row[];
		search?: string;
		selectId?: number;
		page?: { number: number; totalPages: number };
		onSearch: (text: string) => void;
		onOpen: (row: Row) => void;
		onNew: () => void;
		onExit: () => void;
		onPage?: (n: number) => void;
		deletion?: ComponentProps<typeof ListScreen<Row>>['deletion'];
	} = $props();
</script>

<KeyboardRoot />
<ListScreen
	title="Clientes"
	entity={{ one: 'cliente', many: 'clientes' }}
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
	{deletion}
/>
<ErrorDialog />
<KeyBar />
