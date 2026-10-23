<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { BookOpenText, ReceiptText } from '@lucide/svelte';
	import Tile from '../components/Tile.svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { Section } from '../sections';

	const entries: Section[] = [
		{ digit: '1', title: 'Facturar', href: '/invoices/billing', icon: ReceiptText },
		{ digit: '2', title: 'Libro de facturas', href: '/invoices/ledger', icon: BookOpenText }
	];

	useKeys([
		...entries.map((section, i) => ({
			key: section.digit,
			label: 'Abrir',
			keyLabel: '1–2',
			hidden: i > 0,
			run: () => goto(resolve(section.href))
		})),
		{ key: 'Escape', label: 'Volver', run: () => goto(resolve('/')) }
	]);
</script>

<div class="grid grid-cols-2 gap-6">
	{#each entries as section (section.href)}
		<Tile {section} />
	{/each}
</div>
