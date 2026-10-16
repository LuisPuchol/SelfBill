<script lang="ts">
	import { formatDate, parseDateDigits, todayIso } from '../format/date';
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';

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
	/** What the user wrote, until it is understood as a date. */
	let typed = $state<string>();

	const text = $derived(typed ?? (value === '' ? '' : formatDate(value)));

	function write(raw: string): void {
		typed = raw;
		ownError = undefined;
	}

	/** Reads the typed digits as a date (121026 → 12/10/2026); invalid text is left as typed. */
	function normalize(): void {
		if (typed === undefined) return;
		if (typed.trim() === '') {
			value = '';
			typed = undefined;
			return;
		}
		const iso = parseDateDigits(typed, todayIso());
		if (iso === null) return;
		value = iso;
		typed = undefined;
	}

	function commit(): boolean {
		normalize();
		if (typed !== undefined) ownError = 'Fecha no válida';
		else ownError = required && value === '' ? 'Este dato es obligatorio' : undefined;
		return ownError === undefined;
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
			inputmode="numeric"
			class="{inputClass} tabular"
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			onfocus={() => input.select()}
			onblur={normalize}
		/>
	{/snippet}
</Field>
