<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		id,
		label,
		error,
		children
	}: {
		id: string;
		label: string;
		error?: string;
		/** Receives the id of the error text, for the input's aria-describedby */
		children: Snippet<[describedBy: string | undefined]>;
	} = $props();

	const errorId = $derived(`${id}-error`);
</script>

<div class="flex flex-col gap-1">
	<label id="{id}-label" for={id} class="font-medium">{label}</label>
	{@render children(error ? errorId : undefined)}
	<p id={errorId} class="min-h-[1.5em] text-danger" aria-live="polite">{error ?? ''}</p>
</div>
