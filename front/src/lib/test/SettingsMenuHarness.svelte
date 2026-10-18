<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import SettingsMenu from '../settings/SettingsMenu.svelte';
	import type { SetupStatus } from '../settings/status';

	let { status }: { status: SetupStatus } = $props();
</script>

<KeyboardRoot />
<SettingsMenu {status} />
<KeyBar />
