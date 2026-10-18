<script lang="ts">
	import SettingsMenu from '#lib/settings/SettingsMenu.svelte';

	let { data } = $props();
</script>

<SettingsMenu status={data.status} />
