<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import DeliveryNoteRecord from '../deliverynotes/DeliveryNoteRecord.svelte';

	let props: Omit<ComponentProps<typeof DeliveryNoteRecord>, 'guard'> = $props();
</script>

<KeyboardRoot />
<DeliveryNoteRecord {...props}>
	{#snippet guard(form)}
		<!-- What the page's UnsavedGuard asks -->
		<output data-testid="dirty">{String(form.isDirty)}</output>
	{/snippet}
</DeliveryNoteRecord>
<ErrorDialog />
<KeyBar />
