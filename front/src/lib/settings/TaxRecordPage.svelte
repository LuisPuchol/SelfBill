<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '../api/client';
	import { toast } from '../components/toast.svelte';
	import UnsavedGuard from '../components/UnsavedGuard.svelte';
	import { rememberList } from '../records/list-memory';
	import TaxRecord from './TaxRecord.svelte';

	let {
		mode,
		initial
	}: {
		mode: 'new' | 'edit';
		initial: {
			validFrom: string;
			vatPercentage: number | null;
			surchargePercentage: number | null;
		};
	} = $props();

	async function goToList(savedId?: number): Promise<void> {
		if (savedId !== undefined)
			rememberList('settings-tax', { href: '/settings/tax', openedId: savedId });
		await goto(resolve('/settings/tax'));
	}
</script>

<!-- What a page does: the mode and the values are read once, when the record opens. -->
<TaxRecord
	{mode}
	{initial}
	create={(body) => request(api.POST('/api/tax-config', { body }))}
	update={(body) => request(api.PUT('/api/tax-config', { body }))}
	{goToList}
	{toast}
>
	{#snippet guard(form, save)}
		<UnsavedGuard isDirty={() => form.isDirty} {save} restoreFocus={() => form.restoreFocus()} />
	{/snippet}
</TaxRecord>
