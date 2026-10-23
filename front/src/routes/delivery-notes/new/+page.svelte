<script lang="ts">
	import DeliveryNoteRecordPage from '#lib/deliverynotes/DeliveryNoteRecordPage.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<DeliveryNoteRecordPage
	id={null}
	initial={data.initial}
	periods={data.periods}
	lookups={data.lookups}
	hasProfile={data.hasProfile}
/>
