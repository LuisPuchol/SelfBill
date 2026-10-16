<script lang="ts">
	import { toasts } from './toast.svelte';
</script>

<div
	role="status"
	class="pointer-events-none fixed inset-x-0 bottom-14 flex flex-col items-center gap-2"
>
	{#each toasts.list as t (t.id)}
		<p class="rounded border border-line bg-ink px-4 py-2 text-canvas shadow">{t.message}</p>
	{/each}
</div>
