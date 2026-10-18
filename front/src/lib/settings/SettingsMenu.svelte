<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { Percent, UserRound } from '@lucide/svelte';
	import Tile from '../components/Tile.svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { Section } from '../sections';
	import { taxSummary, type SetupStatus } from './status';

	let { status }: { status: SetupStatus } = $props();

	const entries = $derived<{ section: Section; detail: string }[]>([
		{
			section: { digit: '1', title: 'IVA y recargo', href: '/settings/tax', icon: Percent },
			detail: taxSummary(status)
		},
		{
			section: {
				digit: '2',
				title: 'Datos del titular',
				href: '/settings/profile',
				icon: UserRound
			},
			detail: status.profile?.fiscalName ?? 'Sin configurar'
		}
	]);

	useKeys(() => [
		...entries.map((e, i) => ({
			key: e.section.digit,
			label: 'Abrir',
			keyLabel: '1–2',
			hidden: i > 0,
			run: () => goto(resolve(e.section.href))
		})),
		{ key: 'Escape', label: 'Volver', run: () => goto(resolve('/')) }
	]);
</script>

<div class="grid grid-cols-2 gap-6">
	{#each entries as entry (entry.section.href)}
		<Tile section={entry.section} detail={entry.detail} />
	{/each}
</div>
