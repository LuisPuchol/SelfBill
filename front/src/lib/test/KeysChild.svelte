<script lang="ts">
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { KeyAction } from '../keyboard/keys';

	let { actions }: { actions: KeyAction[] } = $props();

	useKeys(() => actions);
</script>
