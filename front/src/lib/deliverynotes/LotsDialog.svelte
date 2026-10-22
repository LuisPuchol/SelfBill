<script lang="ts">
	import { untrack } from 'svelte';
	import Dialog from '../components/Dialog.svelte';
	import DigitsField from '../components/DigitsField.svelte';
	import { FormNav, setFormNav } from '../keyboard/form-nav';
	import type { KeyAction } from '../keyboard/keys';

	type Lots = [string, string, string];

	let {
		open = $bindable(false),
		lots,
		onAccept
	}: {
		open?: boolean;
		/** The line's lots when it opens */
		lots: Lots;
		onAccept: (lots: Lots) => void;
	} = $props();

	// Its own Enter order: the editor's form must neither walk nor validate these fields.
	const nav = new FormNav({ onLast: () => nav.focusFirstField() });
	setFormNav(nav);

	let values = $state<Lots>(['', '', '']);

	// A fresh copy every time it opens: what was typed and cancelled is gone.
	$effect.pre(() => {
		if (open) values = [...untrack(() => lots)];
	});

	async function accept(): Promise<void> {
		if (!(await nav.validateAll())) return;
		onAccept([...values]);
		open = false;
	}

	const actions: KeyAction[] = [
		{
			key: 'Enter',
			label: 'Siguiente',
			hidden: true,
			when: () => nav.hasFocus(),
			run: () => nav.next()
		},
		{
			key: 'Enter',
			shift: true,
			label: 'Anterior',
			hidden: true,
			when: () => nav.hasFocus(),
			run: () => nav.prev()
		},
		{ key: 'NumpadAdd', label: 'Aceptar', run: accept },
		{
			key: 'Escape',
			label: 'Cancelar',
			run: () => {
				open = false;
			}
		}
	];
</script>

<Dialog bind:open title="Lotes" {actions}>
	<table class="border-collapse">
		<thead>
			<tr class="text-muted">
				<th class="px-1 pb-1 text-left font-medium">Lote 1</th>
				<th class="px-1 pb-1 text-left font-medium">Lote 2</th>
				<th class="px-1 pb-1 text-left font-medium">Lote 3</th>
			</tr>
		</thead>
		<tbody>
			<tr class="align-top">
				<td class="w-40 px-1">
					<DigitsField
						label="Lote 1"
						name="lots[0]"
						length={8}
						required
						compact
						bind:value={values[0]}
					/>
				</td>
				<td class="w-40 px-1">
					<DigitsField label="Lote 2" name="lots[1]" length={8} compact bind:value={values[1]} />
				</td>
				<td class="w-40 px-1">
					<DigitsField label="Lote 3" name="lots[2]" length={8} compact bind:value={values[2]} />
				</td>
			</tr>
		</tbody>
	</table>
</Dialog>
