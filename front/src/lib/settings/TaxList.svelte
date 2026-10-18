<script lang="ts">
	import { untrack } from 'svelte';
	import ActionButton from '../components/ActionButton.svelte';
	import DataTable from '../components/DataTable.svelte';
	import ScreenHeader from '../components/ScreenHeader.svelte';
	import type { Column } from '../components/table';
	import type { components } from '../api/schema';
	import { formatDate } from '../format/date';
	import { formatPercent } from '../format/number';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';

	type TaxConfigResponse = components['schemas']['TaxConfigResponse'];

	let {
		periods,
		today,
		selectId,
		onNew,
		onOpen,
		onExit
	}: {
		/** Newest first, as the API gives them */
		periods: TaxConfigResponse[];
		/** ISO date: which period is in force */
		today: string;
		/** Row to mark on entry: the last one opened; without it, the one in force */
		selectId?: number;
		onNew: () => void;
		onOpen: (period: TaxConfigResponse) => void;
		onExit: () => void;
	} = $props();

	let selected = $state(0);

	/** The newest period that has started. */
	const inForce = $derived(periods.find((p) => (p.validFrom ?? '') <= today));

	const columns: Column<TaxConfigResponse>[] = [
		{ label: 'Desde', value: (p) => formatDate(p.validFrom ?? '') },
		{ label: 'IVA', value: (p) => formatPercent(p.vatPercentage ?? 0), align: 'right' },
		{ label: 'Recargo', value: (p) => formatPercent(p.surchargePercentage ?? 0), align: 'right' },
		{
			label: 'Estado',
			value: (p) =>
				p === inForce
					? 'Vigente'
					: (p.validFrom ?? '') > today
						? `A partir del ${formatDate(p.validFrom ?? '')}`
						: ''
		}
	];

	// DataTable goes back to the first row when the rows change (its effect runs first).
	$effect(() => {
		void periods;
		untrack(() => {
			const wanted = periods.findIndex((p) => p.id === selectId);
			const current = periods.findIndex((p) => p === inForce);
			selected = wanted >= 0 ? wanted : Math.max(0, current);
		});
	});

	const hasRows = () => periods.length > 0;
	function openSelected(): void {
		const period = periods[selected];
		if (period !== undefined) onOpen(period);
	}

	const create: KeyAction = { key: 'F2', label: 'Nuevo', run: () => onNew() };
	const back: KeyAction = { key: 'Escape', label: 'Volver', run: () => onExit() };
	// Enter itself belongs to DataTable: this one is only the button's twin.
	const open: KeyAction = { key: 'Enter', label: 'Corregir', run: openSelected };

	useKeys(() => [create, back]);
</script>

<ScreenHeader title="IVA y recargo">
	<ActionButton action={create} />
	<ActionButton action={open} enabled={hasRows} />
	<ActionButton action={back} />
</ScreenHeader>
<DataTable
	{columns}
	rows={periods}
	{onOpen}
	openLabel="Corregir"
	emptyText="Todavía no hay ningún tipo. Pulsa F2 para añadir el primero."
	bind:selected
/>
