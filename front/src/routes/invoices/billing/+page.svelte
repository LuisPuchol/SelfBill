<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '#lib/api/client.js';
	import { toast } from '#lib/components/toast.svelte.js';
	import Billing from '#lib/invoices/Billing.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();

	async function issue(clientId: number, deliveryNoteIds: number[]): Promise<{ number: string }> {
		const invoice = await request(
			api.POST('/api/invoices/issue', { body: { clientId, deliveryNoteIds } })
		);
		// An issued invoice always has its number.
		return { number: invoice.number! };
	}
</script>

<Billing
	today={data.today}
	hasProfile={data.hasProfile}
	loadPending={(dateTo) =>
		request(api.GET('/api/invoices/pending', { params: { query: { dateTo } } }))}
	{issue}
	{toast}
	onExit={() => goto(resolve('/invoices'))}
/>
