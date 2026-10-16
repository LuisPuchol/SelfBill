<script lang="ts">
	import { keymap } from '../keyboard/keymap.svelte';
	import { keyLabel } from '../keyboard/keys';
</script>

<footer
	class="fixed inset-x-0 bottom-0 flex flex-wrap gap-x-6 gap-y-1 border-t border-line bg-canvas px-4 py-2 text-sm text-muted"
>
	{#each keymap.visible as action (`${action.shift ? 'S+' : ''}${action.key}`)}
		<span><kbd class="font-medium text-ink">{keyLabel(action)}</kbd> {action.label}</span>
	{/each}
</footer>
