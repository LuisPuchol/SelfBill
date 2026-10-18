<script lang="ts">
	import TaxRecordPage from '#lib/settings/TaxRecordPage.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

{#key data.initial.validFrom}
	<TaxRecordPage mode="edit" initial={data.initial} />
{/key}
