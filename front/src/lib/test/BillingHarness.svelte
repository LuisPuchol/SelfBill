<script lang="ts">
	// The real styles: the list's scroll box is measured.
	import '../../routes/layout.css';
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Billing from '../invoices/Billing.svelte';

	let props: ComponentProps<typeof Billing> = $props();
</script>

<KeyboardRoot />
<!-- The app's main: its bottom padding keeps the list clear of the key bar -->
<main class="p-6 pb-16">
	<Billing {...props} />
</main>
<ErrorDialog />
<KeyBar />
