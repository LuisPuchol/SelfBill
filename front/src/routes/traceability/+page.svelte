<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '#lib/api/client.js';
	import { printDocument } from '#lib/print.js';
	import Traceability from '#lib/traceability/Traceability.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<Traceability
	today={data.today}
	loadArticles={(dateFrom, dateTo) =>
		request(api.GET('/api/traceability/articles', { params: { query: { dateFrom, dateTo } } }))}
	loadLots={(articleId, dateFrom, dateTo) =>
		request(
			api.GET('/api/traceability/lots', { params: { query: { articleId, dateFrom, dateTo } } })
		)}
	loadDeliveries={(articleId, code, dateFrom, dateTo) =>
		request(
			api.GET('/api/traceability/deliveries', {
				params: { query: { articleId, code, dateFrom, dateTo } }
			})
		)}
	print={printDocument}
	onExit={() => goto(resolve('/'))}
/>
