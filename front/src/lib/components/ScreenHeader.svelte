<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		title,
		middle,
		children
	}: {
		title: string;
		/** Between the title and the actions, on the same line */
		middle?: Snippet;
		children?: Snippet;
	} = $props();
</script>

<div class="mb-4 flex flex-wrap items-center justify-between gap-3">
	<h1 class="text-2xl font-semibold">{title}</h1>
	{@render middle?.()}
	<div class="flex gap-3">{@render children?.()}</div>
</div>
