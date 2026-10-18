<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import ActionButton from '../components/ActionButton.svelte';

	let props: ComponentProps<typeof ActionButton> = $props();
</script>

<input aria-label="Campo" />
<ActionButton {...props} />
