<script lang="ts">
	import { formatDate, parseDateDigits, todayIso } from '../format/date';
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';

	let {
		label,
		name,
		value = $bindable(todayIso()),
		required = false,
		error
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		/** ISO date (yyyy-mm-dd); empty when cleared */
		value?: string;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();
	/** What the user wrote and the value it gave; shown as long as `value` still is that one. */
	let typed = $state<{ text: string; value: string }>();

	const text = $derived(
		typed !== undefined && typed.value === value
			? typed.text
			: value === ''
				? ''
				: formatDate(value)
	);

	/** The value follows the text at once (121026 → 2026-10-12): a save may come without leaving. */
	function write(raw: string): void {
		const iso = parseDateDigits(raw, todayIso()) ?? '';
		typed = { text: raw, value: iso };
		value = iso;
		ownError = undefined;
	}

	/** Shows a date as dd/mm/yyyy; invalid text is left as typed, with its error. */
	function check(includeRequired: boolean): boolean {
		if (value !== '') {
			typed = undefined;
			ownError = undefined;
		} else if (text.trim() !== '') {
			ownError = 'Fecha no válida';
		} else {
			typed = undefined;
			// Empty is only reported when asked to (Enter), never on leaving.
			if (includeRequired) ownError = required ? 'Este dato es obligatorio' : undefined;
		}
		return ownError === undefined;
	}

	const commit = () => check(true);

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} error={error || ownError}>
	{#snippet children(describedBy)}
		<input
			bind:this={input}
			bind:value={() => text, write}
			id={name}
			{name}
			type="text"
			inputmode="numeric"
			class="{inputClass} tabular"
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			use:selectOnFocus
			autocomplete="off"
			onblur={() => check(false)}
		/>
	{/snippet}
</Field>
