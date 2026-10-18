<script lang="ts">
	import ArticleRecord from '#lib/catalog/ArticleRecord.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<!-- One form per article: it is built once, when the record opens -->
{#key data.id}
	<ArticleRecord id={data.id} initial={data.initial} />
{/key}
