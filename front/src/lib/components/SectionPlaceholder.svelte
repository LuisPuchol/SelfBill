<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { useKeys } from '../keyboard/keymap.svelte.js';
	import { sections, type SectionRoute } from '../sections.js';

	let { href }: { href: SectionRoute } = $props();
	const section = $derived(sections.find((s) => s.href === href));

	useKeys([{ key: 'Escape', label: 'Salir', run: () => goto(resolve('/')) }]);
</script>

<h1 class="text-3xl font-semibold">{section?.title}</h1>
<p class="mt-4 text-muted">Esta sección todavía no está hecha.</p>
