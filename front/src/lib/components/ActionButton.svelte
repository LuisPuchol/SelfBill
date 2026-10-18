<script lang="ts">
	import { keyLabel, type KeyAction } from '../keyboard/keys';

	type Variant = 'normal' | 'primary' | 'danger';

	let {
		action,
		enabled = () => action.when?.() !== false,
		variant = 'normal'
	}: {
		action: Pick<KeyAction, 'key' | 'shift' | 'keyLabel' | 'label' | 'run' | 'when'>;
		enabled?: () => boolean;
		variant?: Variant;
	} = $props();

	const looks: Record<Variant, string> = {
		normal: 'border-line bg-surface text-ink not-aria-disabled:hover:bg-canvas',
		primary: 'border-accent bg-accent text-accent-ink',
		danger: 'border-line bg-surface text-danger not-aria-disabled:hover:bg-canvas'
	};
</script>

<!-- Out of the Tab order and mousedown keeps the focus where it was: the button is the mouse twin of the key.
	Not the `disabled` attribute: a disabled button swallows the mouse events, so Chromium would take the focus away. -->
<button
	type="button"
	tabindex="-1"
	aria-disabled={!enabled()}
	onmousedown={(e) => e.preventDefault()}
	onclick={() => enabled() && void action.run()}
	class="h-12 rounded border-2 px-4 text-lg aria-disabled:cursor-default aria-disabled:opacity-50 {looks[
		variant
	]}"
>
	{action.label}
	<kbd
		class="rounded px-1.5 text-sm {variant === 'primary'
			? 'bg-accent-ink/20 text-accent-ink'
			: 'bg-canvas text-muted'}">{keyLabel(action)}</kbd
	>
</button>
