<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';
	import KeysChild from './KeysChild.svelte';

	let {
		parentActions,
		childActions,
		late = false
	}: { parentActions: KeyAction[]; childActions: KeyAction[]; late?: boolean } = $props();

	// svelte-ignore state_referenced_locally
	let showChild = $state(!late);

	useKeys(() => parentActions);
</script>

<KeyboardRoot />
<button onclick={() => (showChild = true)}>Montar</button>
{#if showChild}
	<KeysChild actions={childActions} />
{/if}
<KeyBar />
