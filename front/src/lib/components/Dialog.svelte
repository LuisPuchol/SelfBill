<script lang="ts">
	import type { Snippet } from 'svelte';
	import { keyLayer, keymap } from '../keyboard/keymap.svelte';
	import { keyLabel, type KeyAction } from '../keyboard/keys';

	let {
		open = $bindable(false),
		title,
		actions,
		children
	}: { open?: boolean; title: string; actions: KeyAction[]; children?: Snippet } = $props();

	let element: HTMLDialogElement | undefined = $state();

	const layer = keyLayer(
		// Fallback after the consumer's actions: every Esc is handled, so the browser never starts a native close request.
		() => [...actions, { key: 'Escape', label: '', hidden: true, run: () => {} }],
		{ modal: true }
	);

	$effect(() => {
		if (open) return keymap.add(layer);
	});

	$effect(() => {
		const el = element;
		if (!el || !open) return;
		const previous = document.activeElement;
		el.showModal();
		return () => {
			el.close();
			if (previous instanceof HTMLElement) previous.focus();
		};
	});
</script>

<dialog
	bind:this={element}
	oncancel={(e) => e.preventDefault()}
	onclose={() => (open = false)}
	class="m-auto min-w-96 rounded border border-line bg-surface p-6 text-ink backdrop:bg-black/40"
>
	{#if open}
		<h2 class="mb-4 text-xl font-semibold">{title}</h2>
		{@render children?.()}
		<div class="mt-6 flex flex-wrap gap-3">
			{#each actions.filter((a) => a.when?.() !== false && !a.hidden) as action (action.key + (action.shift ? '+S' : ''))}
				<button
					type="button"
					class="rounded border border-line bg-surface px-4 py-3 text-lg hover:bg-canvas focus:border-focus"
					onclick={() => void action.run()}
				>
					<kbd class="font-medium">{keyLabel(action)}</kbd>
					{action.label}
				</button>
			{/each}
		</div>
	{/if}
</dialog>
