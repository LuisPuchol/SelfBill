<script lang="ts">
	import ActionButton from '../components/ActionButton.svelte';
	import type { KeyAction } from '../keyboard/keys';

	type Action = Pick<KeyAction, 'key' | 'label' | 'run'>;

	// What a screen change does: the first button's click puts another button in its place.
	let { first, second }: { first: Action; second: Action } = $props();

	let swapped = $state(false);
</script>

{#if swapped}
	<ActionButton action={second} />
{:else}
	<ActionButton
		action={{
			...first,
			run: () => {
				swapped = true;
				return first.run();
			}
		}}
	/>
{/if}
