<script lang="ts">
	import { keyLayer, keymap } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';
	import KeysChild from './KeysChild.svelte';

	let { contentActions }: { contentActions: KeyAction[] } = $props();

	const layer = keyLayer(() => [{ key: 'F8', label: 'Cerrar', run: () => {} }], { modal: true });
	$effect(() => keymap.add(layer));
</script>

<KeysChild actions={contentActions} />
