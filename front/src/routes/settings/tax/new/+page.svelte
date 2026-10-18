<script lang="ts">
	import TaxRecordPage from '#lib/settings/TaxRecordPage.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<TaxRecordPage mode="new" initial={data.initial} />
