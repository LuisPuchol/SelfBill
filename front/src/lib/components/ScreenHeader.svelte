<script lang="ts">
	import type { Snippet } from 'svelte';

	let { title, children }: { title: string; children?: Snippet } = $props();
</script>

<div class="mb-4 flex flex-wrap items-center justify-between gap-3">
	<h1 class="text-2xl font-semibold">{title}</h1>
	<div class="flex gap-3">{@render children?.()}</div>
</div>
