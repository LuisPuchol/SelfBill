<script lang="ts">
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';

	let {
		label,
		name,
		value = $bindable(''),
		required = false,
		error,
		type = 'text'
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		value?: string;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
		type?: 'text' | 'email';
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();

	function commit(): boolean {
		ownError = required && value.trim() === '' ? 'Este dato es obligatorio' : undefined;
		return ownError === undefined;
	}

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} error={error ?? ownError}>
	{#snippet children(describedBy)}
		<input
			bind:this={input}
			bind:value
			id={name}
			{name}
			{type}
			class={inputClass}
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			onfocus={() => input.select()}
			oninput={() => (ownError = undefined)}
		/>
	{/snippet}
</Field>
