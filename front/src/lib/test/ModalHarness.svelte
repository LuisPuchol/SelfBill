<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';
	import ModalHolder from './ModalHolder.svelte';

	let { pageActions, contentActions }: { pageActions: KeyAction[]; contentActions: KeyAction[] } =
		$props();

	useKeys(() => pageActions);
</script>

<KeyboardRoot />
<ModalHolder {contentActions} />
<KeyBar />
