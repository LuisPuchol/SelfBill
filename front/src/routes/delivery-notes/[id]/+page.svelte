<script lang="ts">
	import DeliveryNoteRecordPage from '#lib/deliverynotes/DeliveryNoteRecordPage.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<!-- One form per note: it is built once, when the record opens -->
{#key data.id}
	<DeliveryNoteRecordPage
		id={data.id}
		initial={data.initial}
		saved={data.saved}
		periods={data.periods}
		lookups={data.lookups}
		hasProfile={data.hasProfile}
	/>
{/key}
