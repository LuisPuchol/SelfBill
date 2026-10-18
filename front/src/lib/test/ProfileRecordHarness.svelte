<script lang="ts">
	import type { ComponentProps } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import ErrorDialog from '../components/ErrorDialog.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import ProfileRecord from '../settings/ProfileRecord.svelte';

	let props: ComponentProps<typeof ProfileRecord> = $props();
</script>

<KeyboardRoot />
<ProfileRecord {...props} />
<ErrorDialog />
<KeyBar />
