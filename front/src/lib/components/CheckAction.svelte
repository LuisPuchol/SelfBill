<script lang="ts">
	import { keyLabel, type KeyAction } from '../keyboard/keys';

	let {
		action,
		checked,
		enabled = () => action.when?.() !== false,
		reason = null
	}: {
		action: Pick<KeyAction, 'key' | 'label' | 'run' | 'when'>;
		checked: boolean;
		enabled?: () => boolean;
		/** Why it is greyed out, shown under it */
		reason?: string | null;
	} = $props();
</script>

<!-- Like ActionButton: the mouse twin of the key, out of the Tab order, never taking the focus.
	A greyed one is `aria-disabled`, not `disabled`, so a click does not move the focus either. -->
<div class="flex flex-col gap-1">
	<button
		type="button"
		role="checkbox"
		aria-checked={checked}
		tabindex="-1"
		aria-disabled={!enabled()}
		onmousedown={(e) => e.preventDefault()}
		onclick={(e) => e.detail < 2 && enabled() && void action.run()}
		class="flex h-12 items-center gap-3 rounded border-2 border-line bg-surface px-4 text-lg text-ink not-aria-disabled:hover:bg-canvas aria-disabled:cursor-default aria-disabled:opacity-50"
	>
		<span
			aria-hidden="true"
			class="flex size-6 items-center justify-center rounded border-2 border-line {checked
				? 'border-accent bg-accent text-accent-ink'
				: ''}">{checked ? '✓' : ''}</span
		>
		{action.label}
		<kbd class="rounded bg-canvas px-1.5 text-sm text-muted">{keyLabel(action)}</kbd>
	</button>
	{#if reason}
		<p class="text-sm text-muted">{reason}</p>
	{/if}
</div>
