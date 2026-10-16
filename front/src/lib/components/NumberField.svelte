<script lang="ts">
	import { formatDecimal, parseDecimal } from '../format/number';
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';

	let {
		label,
		name,
		value = $bindable<number | null>(null),
		decimals = 2,
		required = false,
		error
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		/** Null while the field is empty or does not hold a number */
		value?: number | null;
		decimals?: number;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();
	/** What the user wrote; shown as long as it still stands for `value`. */
	let typed = $state<string>();

	const text = $derived(
		typed !== undefined && parseDecimal(typed) === value
			? typed
			: value === null
				? ''
				: formatDecimal(value, decimals)
	);

	function write(raw: string): void {
		typed = raw;
		value = parseDecimal(raw);
		ownError = undefined;
	}

	/** Fixed decimals on leaving (10 → 10,00); invalid text is left as typed. */
	function normalize(): void {
		if (text.trim() === '') {
			typed = '';
			return;
		}
		const number = parseDecimal(text);
		if (number === null) return;
		// The value is what the user sees: rounded to the shown decimals.
		write(formatDecimal(number, decimals));
	}

	function commit(): boolean {
		normalize();
		if (text === '') ownError = required ? 'Este dato es obligatorio' : undefined;
		else ownError = value === null ? 'Escribe un número' : undefined;
		return ownError === undefined;
	}

	function onkeydown(event: KeyboardEvent): void {
		if (event.code !== 'NumpadDecimal') return;
		// Windows writes "." or "," depending on its settings: always a comma here.
		event.preventDefault();
		const end = input.value.length;
		input.setRangeText(',', input.selectionStart ?? end, input.selectionEnd ?? end, 'end');
		write(input.value);
	}

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} error={error ?? ownError}>
	{#snippet children(describedBy)}
		<input
			bind:this={input}
			bind:value={() => text, write}
			id={name}
			{name}
			type="text"
			inputmode="decimal"
			class="{inputClass} tabular text-right"
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			onfocus={() => input.select()}
			onblur={normalize}
			{onkeydown}
		/>
	{/snippet}
</Field>
