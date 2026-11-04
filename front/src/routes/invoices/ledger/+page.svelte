<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '#lib/api/client.js';
	import { toast } from '#lib/components/toast.svelte.js';
	import { invoiceDeletion } from '#lib/invoices/deletion.js';
	import Ledger from '#lib/invoices/Ledger.svelte';
	import { LEDGER_LIMIT } from '#lib/invoices/ledger.js';
	import { printDocument } from '#lib/print.js';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	async function loadInvoices(dateFrom: string, dateTo: string) {
		const result = await request(
			api.GET('/api/invoices', {
				params: {
					query: {
						status: 'ISSUED',
						dateFrom,
						dateTo,
						size: LEDGER_LIMIT,
						sort: ['date,asc', 'id,asc']
					}
				}
			})
		);
		const invoices = result.content ?? [];
		return { invoices, truncated: (result.page?.totalElements ?? 0) > invoices.length };
	}

	function loadTotals(dateFrom: string, dateTo: string) {
		return request(
			api.GET('/api/invoices/totals', { params: { query: { status: 'ISSUED', dateFrom, dateTo } } })
		);
	}
</script>

<Ledger
	today={data.today}
	{loadInvoices}
	{loadTotals}
	loadInvoice={(id) => request(api.GET('/api/invoices/{id}', { params: { path: { id } } }))}
	fetchPdf={(id) =>
		request(api.GET('/api/invoices/{id}/pdf', { params: { path: { id } }, parseAs: 'blob' }))}
	print={printDocument}
	sendEmail={async (id) => {
		await request(api.POST('/api/invoices/{id}/email', { params: { path: { id } } }));
	}}
	deletion={invoiceDeletion}
	{toast}
	onExit={() => goto(resolve('/invoices'))}
/>
