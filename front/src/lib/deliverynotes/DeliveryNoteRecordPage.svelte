<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '../api/client';
	import type { components } from '../api/schema';
	import { toast } from '../components/toast.svelte';
	import UnsavedGuard from '../components/UnsavedGuard.svelte';
	import { recallList, rememberList } from '../records/list-memory';
	import { deliveryNoteDeletion } from './deletion';
	import DeliveryNoteRecord from './DeliveryNoteRecord.svelte';
	import type { NoteValues } from './form';
	import { lastPrice, lastPriceQuery } from './last-price';
	import type { Lookups } from './lookups';

	let {
		id,
		initial,
		saved,
		periods,
		lookups
	}: {
		id: number | null;
		initial: NoteValues;
		saved?: components['schemas']['DeliveryNoteResponse'];
		periods: components['schemas']['TaxConfigResponse'][];
		lookups: Lookups;
	} = $props();

	async function proposePrice(clientId: number, articleId: number): Promise<number | null> {
		const notes = await request(
			api.GET('/api/delivery-notes', { params: { query: lastPriceQuery(clientId, articleId) } })
		);
		return lastPrice(notes.content ?? [], articleId);
	}

	async function goToList(savedId?: number): Promise<void> {
		const memory = recallList('delivery-notes');
		const href = memory?.href ?? resolve('/delivery-notes');
		rememberList('delivery-notes', { href, openedId: savedId ?? memory?.openedId });
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- the remembered URL came from the list page
		await goto(href);
	}
</script>

<!-- What a page does: the note is read once, when the record opens. -->
<DeliveryNoteRecord
	{id}
	{initial}
	{saved}
	{periods}
	{lookups}
	create={(body) => request(api.POST('/api/delivery-notes', { body }))}
	update={(noteId, body) =>
		request(api.PUT('/api/delivery-notes/{id}', { params: { path: { id: noteId } }, body }))}
	remove={deliveryNoteDeletion.remove}
	{proposePrice}
	{goToList}
	{toast}
>
	{#snippet guard(form)}
		<UnsavedGuard
			isDirty={() => form.isDirty}
			save={() => form.save()}
			restoreFocus={() => form.restoreFocus()}
		/>
	{/snippet}
</DeliveryNoteRecord>
