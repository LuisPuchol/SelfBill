<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import CheckAction from '../components/CheckAction.svelte';

	let props: ComponentProps<typeof CheckAction> = $props();
</script>

<input aria-label="Campo" />
<CheckAction {...props} />
