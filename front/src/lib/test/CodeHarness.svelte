<script lang="ts">
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { FormNav } from '../keyboard/form-nav';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import CodeField from '../components/CodeField.svelte';

	let {
		lookup,
		initial = null,
		onSave,
		compact = false,
		onFound
	}: {
		lookup: (code: number) => Promise<string | null>;
		initial?: number | null;
		onSave?: () => void;
		compact?: boolean;
		onFound?: (code: number) => void;
	} = $props();

	let nav = $state<FormNav>();

	// svelte-ignore state_referenced_locally
	let articleId = $state<number | null>(initial);
	let quantity = $state('');

	// What a screen does with "+": validate everything, then save.
	useKeys(() => [
		{
			key: 'NumpadAdd',
			label: 'Guardar',
			when: () => onSave !== undefined,
			run: async () => {
				if (await nav?.validateAll()) onSave?.();
			}
		}
	]);
</script>

<KeyboardRoot />
<Form bind:nav onLast={() => {}}>
	<CodeField
		label="Artículo"
		name="articleId"
		bind:value={articleId}
		{lookup}
		{compact}
		{onFound}
		notFoundMessage="No existe ningún artículo con ese código"
		required
	/>
	<TextField label="Cantidad" name="quantity" bind:value={quantity} />
</Form>
<output data-testid="articleId">{String(articleId)}</output>
