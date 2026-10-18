<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import TaxList from '../settings/TaxList.svelte';

	let props: ComponentProps<typeof TaxList> = $props();
</script>

<KeyboardRoot />
<TaxList {...props} />
<KeyBar />
