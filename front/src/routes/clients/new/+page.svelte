<script lang="ts">
	import ClientRecord from '#lib/catalog/ClientRecord.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<ClientRecord initial={data.initial} />
