<script lang="ts" generics="T">
	import { getFormNav } from '../keyboard/form-nav';
	import Field from './Field.svelte';

	let {
		label,
		name,
		options,
		value = $bindable(null),
		required = false,
		error
	}: {
		label: string;
		/** API field path; also the group's id */
		name: string;
		/** Option n is picked with the digit n */
		options: { value: T; label: string }[];
		value?: T | null;
		required?: boolean;
		/** From outside (e.g. the API); wins over the field's own */
		error?: string;
	} = $props();

	const nav = getFormNav();
	let group: HTMLDivElement;
	let ownError = $state<string>();

	function pick(option: { value: T }): void {
		value = option.value;
		ownError = undefined;
	}

	function commit(): boolean {
		ownError = required && value === null ? 'Elige una opción' : undefined;
		return ownError === undefined;
	}

	function onkeydown(event: KeyboardEvent): void {
		if (event.ctrlKey || event.altKey || event.metaKey || !/^[0-9]$/.test(event.key)) return;
		// A digit is ours even when it matches no option: the global keymap must not see it.
		event.preventDefault();
		const option = options[Number(event.key) - 1];
		if (option) pick(option);
	}

	$effect(() => nav?.register({ name, el: group, commit }));
</script>

<Field id={name} {label} error={error || ownError}>
	{#snippet children(describedBy)}
		<div
			bind:this={group}
			id={name}
			role="radiogroup"
			tabindex="0"
			aria-labelledby="{name}-label"
			aria-invalid={describedBy ? true : undefined}
			aria-describedby={describedBy}
			class="flex min-h-12 flex-wrap items-center gap-2 rounded border-2 border-line bg-surface p-1 text-ink focus:border-focus focus:bg-field-active"
			{onkeydown}
		>
			{#each options as option, index (index)}
				<!-- Not focusable on purpose: the group keeps the cursor, so Enter continues -->
				<!-- svelte-ignore a11y_click_events_have_key_events, a11y_interactive_supports_focus -->
				<div
					role="radio"
					aria-checked={value === option.value}
					class="cursor-pointer rounded px-3 py-1 aria-checked:bg-accent aria-checked:text-accent-ink"
					onclick={() => pick(option)}
				>
					<kbd>{index + 1}</kbd>
					{option.label}
				</div>
			{/each}
		</div>
	{/snippet}
</Field>
