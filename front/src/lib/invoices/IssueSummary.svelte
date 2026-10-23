<script lang="ts" module>
	/** What a batch did, when some row was left without its invoice. */
	export interface IssueReport {
		/** `issuedSummary` of the numbers issued */
		issued: string;
		notIssued: { name: string; month: string; reason: string }[];
	}
</script>

<script lang="ts">
	import Dialog from '../components/Dialog.svelte';

	let { report, onClose }: { report: IssueReport | null; onClose: () => void } = $props();
</script>

<Dialog
	bind:open={
		() => report !== null,
		(open) => {
			if (!open) onClose();
		}
	}
	title="Facturas emitidas"
	actions={[
		{ key: 'Enter', label: 'Entendido', run: onClose },
		{ key: 'Escape', label: 'Entendido', hidden: true, run: onClose }
	]}
>
	{#if report}
		<p>{report.issued}</p>
		<p class="mt-4 font-medium">No se han facturado:</p>
		<ul class="mt-2 flex max-h-[50vh] max-w-2xl flex-col gap-2 overflow-y-auto">
			{#each report.notIssued as row, i (i)}
				<li>
					<span class="font-medium">{row.name}</span> · <span class="tabular">{row.month}</span>:
					{row.reason}
				</li>
			{/each}
		</ul>
	{/if}
</Dialog>
