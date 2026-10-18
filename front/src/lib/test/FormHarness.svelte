<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import NumberField from '../components/NumberField.svelte';
	import type { FormNav } from '../keyboard/form-nav';
	import { useKeys } from '../keyboard/keymap.svelte';

	let {
		onLast,
		quantityError,
		nameError,
		onSave
	}: { onLast: () => void; quantityError?: string; nameError?: string; onSave?: () => void } =
		$props();

	let name = $state('');
	let quantity = $state<number | null>(null);
	let last = $state('');
	let nav = $state<FormNav>();

	// What a page does: validate the whole form, then save.
	useKeys([
		{
			key: 'NumpadAdd',
			label: 'Guardar',
			run: async () => {
				if (await nav?.validateAll()) onSave?.();
			}
		}
	]);
</script>

<KeyboardRoot />
<Form {onLast} bind:nav>
	<TextField label="Nombre" name="name" bind:value={name} error={nameError} required />
	<NumberField label="Cantidad" name="quantity" bind:value={quantity} error={quantityError} />
	<TextField
		label="Último"
		name="last"
		bind:value={last}
		validate={(v) => (v.includes('@') ? undefined : 'Revisa el email')}
	/>
</Form>
<output data-testid="quantity">{String(quantity)}</output>
<KeyBar />
