<script lang="ts">
	// The real styles: the tables' scroll boxes are measured.
	import '../../routes/layout.css';
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Traceability from '../traceability/Traceability.svelte';

	let props: ComponentProps<typeof Traceability> = $props();
</script>

<KeyboardRoot />
<!-- The app's main: its bottom padding keeps the list clear of the key bar -->
<main class="p-6 pb-16">
	<Traceability {...props} />
</main>
<ErrorDialog />
<KeyBar />
