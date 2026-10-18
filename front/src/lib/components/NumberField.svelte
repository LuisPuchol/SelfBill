<script lang="ts">
	import { formatDecimal, parseDecimal } from '../format/number';
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';

	let {
		label,
		name,
		value = $bindable<number | null>(null),
		decimals = 2,
		required = false,
		error,
		validate,
		compact = false
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
		/** Own check for a number, as rounded: the message, or nothing when it is fine */
		validate?: (value: number) => string | undefined;
		/** For a table cell: label for screen readers only, smaller error line */
		compact?: boolean;
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

	/**
	 * Fixed decimals (10 → 10,00); invalid text is left as typed, with its error.
	 * Empty is only reported when asked to (Enter), never on leaving.
	 */
	function check(includeRequired: boolean): boolean {
		if (text.trim() === '') {
			typed = '';
			if (includeRequired) ownError = required ? 'Este dato es obligatorio' : undefined;
			return ownError === undefined;
		}
		const number = parseDecimal(text);
		if (number === null) ownError = 'Escribe un número';
		else if (decimals === 0 && !Number.isInteger(number)) ownError = 'Escribe un número entero';
		else {
			// The value is what the user sees: rounded to the shown decimals, and checked so.
			const shown = formatDecimal(number, decimals);
			write(shown);
			ownError = validate?.(parseDecimal(shown)!);
		}
		return ownError === undefined;
	}

	const commit = () => check(true);

	function onkeydown(event: KeyboardEvent): void {
		// With NumLock off the key is a Delete: only a real "." or "," is ours.
		if (event.code !== 'NumpadDecimal' || (event.key !== '.' && event.key !== ',')) return;
		if (event.ctrlKey || event.altKey || event.metaKey) return;
		event.preventDefault();
		if (decimals === 0) return;
		// Windows writes "." or "," depending on its settings: always a comma here.
		const end = input.value.length;
		input.setRangeText(',', input.selectionStart ?? end, input.selectionEnd ?? end, 'end');
		write(input.value);
	}

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} {compact} error={error || ownError}>
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
			use:selectOnFocus
			autocomplete="off"
			onblur={() => check(false)}
			{onkeydown}
		/>
	{/snippet}
</Field>
