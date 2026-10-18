<script lang="ts">
	import type { DeleteFlow } from '../records/delete-flow.svelte';
	import Dialog from './Dialog.svelte';

	let { flow }: { flow: DeleteFlow } = $props();
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
		{ key: 'Enter', label: 'No borrar', run: () => flow.cancel() },
		{ key: 'Escape', label: 'No borrar', hidden: true, run: () => flow.cancel() },
		{ key: 'F8', label: 'Borrar', run: () => flow.confirm() }
	]}
>
	{#if flow.dialog?.name}
		<p class="font-medium">{flow.dialog.name}</p>
	{/if}
	{#if flow.dialog?.body}
		<p class="mt-2">{flow.dialog.body}</p>
	{/if}
</Dialog>
