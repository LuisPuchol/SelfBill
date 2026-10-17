<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import Tile from '#lib/components/Tile.svelte';
	import { useKeys } from '#lib/keyboard/keymap.svelte.js';
	import { sections } from '#lib/sections.js';

	useKeys(() =>
		sections.map((s, i) => ({
			key: s.digit,
			label: 'Abrir sección',
			keyLabel: '1–6',
			hidden: i > 0,
			run: () => goto(resolve(s.href))
		}))
	);
</script>

<div class="grid grid-cols-3 gap-6">
	{#each sections as section (section.href)}
		<Tile {section} />
	{/each}
</div>
