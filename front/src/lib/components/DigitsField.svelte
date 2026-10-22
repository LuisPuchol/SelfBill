<script lang="ts">
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';

	let {
		label,
		name,
		value = $bindable(''),
		length,
		required = false,
		error,
		compact = false,
		readOnly = false
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		/** Digits only, as text so a leading zero survives */
		value?: string;
		/** Exact number of digits */
		length: number;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
		/** For a table cell: label for screen readers only, smaller error line */
		compact?: boolean;
		/** Shown but not editable, e.g. a document that can no longer change */
		readOnly?: boolean;
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();

	/** Keeps only digits, whatever was typed or pasted. */
	function oninput(): void {
		value = input.value.replace(/\D/g, '').slice(0, length);
		input.value = value;
		ownError = undefined;
	}

	/** Empty is only reported when asked to (Enter), never on leaving. */
	function check(includeRequired: boolean): boolean {
		if (value === '') {
			if (includeRequired) ownError = required ? 'Este dato es obligatorio' : undefined;
		} else {
			ownError = value.length === length ? undefined : `Tienen que ser ${length} cifras`;
		}
		return ownError === undefined;
	}

	const commit = () => check(true);

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} {compact} error={error || ownError}>
	{#snippet children(describedBy)}
		<input
			bind:this={input}
			{value}
			id={name}
			{name}
			type="text"
			inputmode="numeric"
			maxlength={length}
			class="{inputClass} tabular"
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			use:selectOnFocus
			autocomplete="off"
			readonly={readOnly}
			onblur={() => check(false)}
			{oninput}
		/>
	{/snippet}
</Field>
