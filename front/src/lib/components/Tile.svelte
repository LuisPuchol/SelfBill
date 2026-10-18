<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Section } from '../sections.js';

	let { section, detail }: { section: Section; detail?: string } = $props();
	const Icon = $derived(section.icon);
</script>

<a
	href={resolve(section.href)}
	class="flex flex-col items-center justify-center gap-4 rounded-lg border border-line bg-surface p-8 hover:border-focus"
>
	<span class="tabular text-6xl font-semibold">{section.digit}</span>
	<Icon size={48} aria-hidden="true" />
	<span class="text-2xl">{section.title}</span>
	{#if detail}<span class="text-base text-muted">{detail}</span>{/if}
</a>
