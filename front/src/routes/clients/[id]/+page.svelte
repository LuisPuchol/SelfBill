<script lang="ts">
	import ClientRecord from '#lib/catalog/ClientRecord.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<!-- One form per client: it is built once, when the record opens -->
{#key data.id}
	<ClientRecord id={data.id} initial={data.initial} />
{/key}
