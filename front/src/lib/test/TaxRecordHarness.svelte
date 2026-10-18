<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import TaxRecord from '../settings/TaxRecord.svelte';

	let props: ComponentProps<typeof TaxRecord> = $props();
</script>

<KeyboardRoot />
<TaxRecord {...props} />
<ErrorDialog />
<KeyBar />
