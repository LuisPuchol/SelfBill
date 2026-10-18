<script lang="ts">
	import ArticleRecord from '#lib/catalog/ArticleRecord.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<ArticleRecord initial={data.initial} />
