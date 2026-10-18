<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import DataTable from '../components/DataTable.svelte';
	import type { Column } from '../components/table';
	import { formatMoney } from '../format/money';

	interface Row {
		name: string;
		amount: number;
	}

	const all: Row[] = [
		{ name: 'Frutas Paco', amount: 84 },
		{ name: 'Bar Pepe', amount: 32 },
		{ name: 'Hotel Sol', amount: 210 }
	];

	const columns: Column<Row>[] = [
		{ label: 'Cliente', value: (r) => r.name },
		{ label: 'Importe', value: (r) => formatMoney(r.amount), align: 'right' }
	];

	let {
		rows = all,
		onOpen,
		onPage,
		page
	}: {
		rows?: Row[];
		onOpen: (row: Row) => void;
		onPage?: (n: number) => void;
		page?: { number: number; totalPages: number };
	} = $props();

	// svelte-ignore state_referenced_locally
	let data = $state(rows);
	let selected = $state(0);
</script>

<KeyboardRoot />
<input aria-label="Filtro" />
<button onclick={() => (data = [...data].reverse())}>Cambiar filas</button>
<button>Otro</button>
<p data-testid="selected">{selected}</p>
<DataTable {columns} rows={data} {onOpen} {page} {onPage} bind:selected />
<KeyBar />
