<script lang="ts">
	import { keymap } from './keymap.svelte';
</script>

<svelte:window onkeydown={keymap.handle} />
