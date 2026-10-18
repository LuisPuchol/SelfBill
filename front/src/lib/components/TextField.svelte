<script lang="ts">
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';

	let {
		label,
		name,
		value = $bindable(''),
		required = false,
		error,
		hint,
		type = 'text',
		validate,
		maxlength
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		value?: string;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
		/** A note under the input */
		hint?: string;
		type?: 'text' | 'email' | 'password';
		/** Own check for a non-empty value: the message, or nothing when it is fine */
		validate?: (value: string) => string | undefined;
		maxlength?: number;
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();

	/** Empty is only reported when asked to (Enter), never on leaving. */
	function check(includeRequired: boolean): boolean {
		if (value.trim() === '') {
			if (includeRequired) ownError = required ? 'Este dato es obligatorio' : undefined;
		} else {
			ownError = validate?.(value);
		}
		return ownError === undefined;
	}

	const commit = () => check(true);

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} {hint} error={error || ownError}>
	{#snippet children(describedBy)}
		<input
			bind:this={input}
			bind:value
			id={name}
			{name}
			{type}
			{maxlength}
			class={inputClass}
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			use:selectOnFocus
			autocomplete="off"
			onblur={() => check(false)}
			oninput={() => (ownError = undefined)}
		/>
	{/snippet}
</Field>
