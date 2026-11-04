<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import ConnectionScreen from '../components/ConnectionScreen.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';

	let {
		ping,
		onRestored,
		intervalMs,
		onBack
	}: {
		ping: () => Promise<boolean>;
		onRestored: () => void;
		intervalMs: number;
		onBack?: () => void;
	} = $props();
</script>

<KeyboardRoot />
<ConnectionScreen {ping} {onRestored} {intervalMs} {onBack} />
<ErrorDialog />
<KeyBar />
