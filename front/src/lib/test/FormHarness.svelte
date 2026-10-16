<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import NumberField from '../components/NumberField.svelte';

	let { onLast, quantityError }: { onLast: () => void; quantityError?: string } = $props();

	let name = $state('');
	let quantity = $state<number | null>(null);
	let last = $state('');
</script>

<KeyboardRoot />
<Form {onLast}>
	<TextField label="Nombre" name="name" bind:value={name} required />
	<NumberField label="Cantidad" name="quantity" bind:value={quantity} error={quantityError} />
	<TextField label="Último" name="last" bind:value={last} />
</Form>
<output data-testid="quantity">{String(quantity)}</output>
<KeyBar />
