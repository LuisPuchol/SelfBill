<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		id,
		label,
		error,
		hint,
		compact = false,
		children
	}: {
		id: string;
		label: string;
		error?: string;
		/** A note under the input, always shown */
		hint?: string;
		/** For a table cell: the label is for screen readers only and the error line is smaller */
		compact?: boolean;
		/** Receives the id of the error text, for the input's aria-describedby */
		children: Snippet<[describedBy: string | undefined]>;
	} = $props();

	const errorId = $derived(`${id}-error`);
</script>

<div class="flex flex-col {compact ? 'gap-0' : 'gap-1'}">
	<label id="{id}-label" for={id} class={compact ? 'sr-only' : 'font-medium'}>{label}</label>
	{@render children(error ? errorId : undefined)}
	{#if hint}<p class="text-muted">{hint}</p>{/if}
	<p
		id={errorId}
		class="text-danger {compact ? 'min-h-[1.25em] text-sm' : 'min-h-[1.5em]'}"
		aria-live="polite"
	>
		{error ?? ''}
	</p>
</div>
