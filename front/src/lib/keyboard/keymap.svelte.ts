import { getContext, setContext, untrack } from 'svelte';
import { dispatch, visibleActions, type KeyAction, type Layer } from './keys';

let nextId = 0;
const LAYER_CONTEXT = Symbol('key-layer');

class Keymap {
	layers = $state.raw<Layer[]>([]);
	visible = $derived(visibleActions(this.layers));

	add = (layer: Layer): (() => void) => {
		untrack(() => (this.layers = [...this.layers, layer]));
		return () => {
			untrack(() => (this.layers = this.layers.filter((l) => l !== layer)));
		};
	};

	/** For root-level layers created outside components. */
	push = (actions: () => KeyAction[], modal = false): (() => void) =>
		this.add({ id: nextId++, actions, modal });

	handle = (event: KeyboardEvent): void => {
		dispatch(this.layers, event);
	};
}

export const keymap = new Keymap();

/**
 * Creates (without registering) a layer parented to the nearest layer of the
 * component tree, so order follows nesting and not effect timing. Call it
 * during component init; register it with keymap.add in an $effect.
 */
export function keyLayer(
	actions: KeyAction[] | (() => KeyAction[]),
	options: { modal?: boolean } = {}
): Layer {
	const layer: Layer = {
		id: nextId++,
		actions: typeof actions === 'function' ? actions : () => actions,
		modal: options.modal ?? false,
		parent: getContext<Layer | undefined>(LAYER_CONTEXT)
	};
	setContext(LAYER_CONTEXT, layer);
	return layer;
}

export function useKeys(
	actions: KeyAction[] | (() => KeyAction[]),
	options: { modal?: boolean } = {}
): void {
	const layer = keyLayer(actions, options);
	$effect(() => keymap.add(layer));
}
