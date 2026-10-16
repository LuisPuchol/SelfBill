<script lang="ts">
	import { onDestroy } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import { keymap, useKeys } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';

	let { actions, modalActions = [] }: { actions: KeyAction[]; modalActions?: KeyAction[] } =
		$props();

	let removeModal: (() => void) | undefined;
	onDestroy(() => removeModal?.());

	useKeys(() => actions);
</script>

<KeyboardRoot />
<button onclick={() => (removeModal = keymap.push(() => modalActions, true))}>Abrir capa</button>
<KeyBar />
