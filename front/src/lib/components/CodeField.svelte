<script lang="ts">
	import { untrack } from 'svelte';
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';
	import { inputClass } from './field-style';
	import { selectOnFocus } from './select-on-focus';

	let {
		label,
		name,
		value = $bindable(null),
		lookup,
		notFoundMessage,
		required = false,
		error,
		compact = false,
		readOnly = false,
		onFound
	}: {
		label: string;
		/** API field path; also the input's id */
		name: string;
		value?: number | null;
		/** Resolves to the name for the code, or null when it does not exist */
		lookup: (code: number) => Promise<string | null>;
		notFoundMessage: string;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
		/** For a table cell: label for screen readers only, smaller error line, and the found name is not shown (the grid has its own column) */
		compact?: boolean;
		/** Shown but not editable, e.g. a document that can no longer change */
		readOnly?: boolean;
		/** After a successful lookup; also after the first lookup of a code passed in */
		onFound?: (code: number) => void;
	} = $props();

	const nav = getFormNav();
	let input: HTMLInputElement;
	let ownError = $state<string>();
	let foundName = $state<string>();
	/** What is typed; the bound value only ever holds a found code (or the one passed in). */
	let text = $state(value === null ? '' : String(value));
	/** Bumped on every edit and every lookup, so an older answer can be told apart. */
	let generation = 0;

	/** Looks the code up; false when it does not exist or the text changed meanwhile. */
	async function find(code: number): Promise<boolean> {
		const mine = ++generation;
		const found = await lookup(code);
		if (mine !== generation) return false;
		if (found === null) {
			foundName = undefined;
			ownError = notFoundMessage;
			return false;
		}
		foundName = found;
		value = code;
		onFound?.(code);
		return true;
	}

	/** Keeps only digits, whatever was typed or pasted. */
	function oninput(): void {
		const digits = input.value.replace(/\D/g, '');
		input.value = digits;
		text = digits;
		value = null;
		generation++;
		foundName = undefined;
		ownError = undefined;
	}

	function commit(): boolean | Promise<boolean> {
		if (text === '') {
			ownError = required ? 'Este dato es obligatorio' : undefined;
			return ownError === undefined;
		}
		const code = Number(text);
		if (value === code) return true;
		return find(code);
	}

	// Starting with a code: look it up once to show its name.
	$effect(() => {
		const initial = untrack(() => value);
		if (initial === null) return;
		untrack(() => void find(initial));
	});

	$effect(() => nav?.register({ name, el: input, commit }));
</script>

<Field id={name} {label} {compact} error={error || ownError}>
	{#snippet children(describedBy)}
		<div class="flex items-center gap-3">
			<input
				bind:this={input}
				value={text}
				id={name}
				{name}
				type="text"
				inputmode="numeric"
				class="{inputClass} tabular"
				aria-invalid={describedBy ? true : undefined}
				aria-describedby={describedBy}
				use:selectOnFocus
				autocomplete="off"
				readonly={readOnly}
				{oninput}
			/>
			{#if foundName && !compact}
				<span>{foundName}</span>
			{/if}
		</div>
	{/snippet}
</Field>
