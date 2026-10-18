<script lang="ts">
	import { setupNotice, type SetupStatus } from '../settings/status';

	let { status }: { status: SetupStatus } = $props();
	const notice = $derived(setupNotice(status));
</script>

{#if notice}
	<p class="mb-6 rounded border-2 border-draft bg-surface p-3 text-draft">{notice}</p>
{/if}
