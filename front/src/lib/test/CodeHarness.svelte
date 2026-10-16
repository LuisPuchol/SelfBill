<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import CodeField from '../components/CodeField.svelte';

	let {
		lookup,
		initial = null
	}: { lookup: (code: number) => Promise<string | null>; initial?: number | null } = $props();

	// svelte-ignore state_referenced_locally
	let articleId = $state<number | null>(initial);
	let quantity = $state('');
</script>

<KeyboardRoot />
<Form onLast={() => {}}>
	<CodeField
		label="Artículo"
		name="articleId"
		bind:value={articleId}
		{lookup}
		notFoundMessage="No existe ningún artículo con ese código"
		required
	/>
	<TextField label="Cantidad" name="quantity" bind:value={quantity} />
</Form>
<output data-testid="articleId">{String(articleId)}</output>
