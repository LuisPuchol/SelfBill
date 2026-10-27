<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '#lib/api/client.js';
	import ClientSummary from '#lib/invoices/ClientSummary.svelte';
	import { LEDGER_LIMIT } from '#lib/invoices/ledger.js';
	import { printDocument } from '#lib/print.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	async function loadInvoices(clientId: number, dateFrom: string, dateTo: string) {
		const result = await request(
			api.GET('/api/invoices', {
				params: {
					query: {
						clientId,
						status: 'ISSUED',
						dateFrom,
						dateTo,
						size: LEDGER_LIMIT,
						sort: ['date,asc', 'id,asc']
					}
				}
			})
		);
		return result.content ?? [];
	}
</script>

<ClientSummary
	today={data.today}
	loadSummary={(dateFrom, dateTo) =>
		request(api.GET('/api/invoices/summary', { params: { query: { dateFrom, dateTo } } }))}
	{loadInvoices}
	loadInvoice={(id) => request(api.GET('/api/invoices/{id}', { params: { path: { id } } }))}
	fetchPdf={(id) =>
		request(api.GET('/api/invoices/{id}/pdf', { params: { path: { id } }, parseAs: 'blob' }))}
	print={printDocument}
	onExit={() => goto(resolve('/invoices'))}
/>
