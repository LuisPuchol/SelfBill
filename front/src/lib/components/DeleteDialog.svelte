<script lang="ts">
	import Dialog from './Dialog.svelte';

	let {
		flow,
		act = 'Borrar',
		keep = 'No borrar'
	}: {
		/** A `DeleteFlow`, or a `RestoreFlow` with its own words */
		flow: {
			readonly dialog: { title: string; name: string; body?: string | null } | null;
			confirm(): Promise<void>;
			cancel(): void;
		};
		/** F8's word: going ahead */
		act?: string;
		/** Enter's word: leaving the record as it is */
		keep?: string;
	} = $props();
</script>

<Dialog
	bind:open={
		() => flow.dialog !== null,
		(open) => {
			if (!open) flow.cancel();
		}
	}
	title={flow.dialog?.title ?? ''}
	actions={[
		{ key: 'Enter', label: keep, run: () => flow.cancel() },
		{ key: 'Escape', label: keep, hidden: true, run: () => flow.cancel() },
		{ key: 'F8', label: act, run: () => flow.confirm() }
	]}
>
	{#if flow.dialog?.name}
		<p class="font-medium">{flow.dialog.name}</p>
	{/if}
	{#if flow.dialog?.body}
		<p class="mt-2">{flow.dialog.body}</p>
	{/if}
</Dialog>
