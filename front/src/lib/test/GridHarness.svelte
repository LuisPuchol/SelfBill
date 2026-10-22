<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Form from '../components/Form.svelte';
	import type { FormNav } from '../keyboard/form-nav';
	import { useKeys } from '../keyboard/keymap.svelte';
	import LinesGrid from '../deliverynotes/LinesGrid.svelte';
	import type { LineValues } from '../deliverynotes/form';

	let {
		initial,
		readOnly = false,
		lookupArticle,
		articleName,
		proposePrice,
		errors = {}
	}: {
		initial: LineValues[];
		readOnly?: boolean;
		lookupArticle: (code: number) => Promise<string | null>;
		articleName: (code: number) => string | undefined;
		proposePrice: (code: number) => Promise<number | null>;
		errors?: Record<string, string>;
	} = $props();

	// svelte-ignore state_referenced_locally
	let lines = $state(initial);
	let nav = $state<FormNav>();
	let grid = $state<ReturnType<typeof LinesGrid>>();
	let valid = $state('');

	// What the editor does with "+": validate everything (the saving itself is not the grid's).
	useKeys([
		{
			key: 'NumpadAdd',
			label: 'Guardar',
			run: async () => {
				valid = String(await nav?.validateAll());
			}
		}
	]);
</script>

<KeyboardRoot />
<Form bind:nav onLast={() => grid?.appendLine()}>
	<LinesGrid
		bind:this={grid}
		bind:lines
		{readOnly}
		{articleName}
		{lookupArticle}
		{proposePrice}
		errorFor={(name) => errors[name]}
	/>
</Form>
<!-- What the editor does when another client is found -->
<button type="button" onclick={() => grid?.reproposePrices()}>Otro cliente</button>
<output data-testid="lines">{JSON.stringify(lines)}</output>
<output data-testid="valid">{valid}</output>
<KeyBar />
