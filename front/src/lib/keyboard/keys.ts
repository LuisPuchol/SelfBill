export interface KeyAction {
	/** event.code or event.key: 'NumpadAdd', 'Escape', 'Enter', 'F3', '1' */
	key: string;
	shift?: boolean;
	label: string;
	/** Replaces keyLabel(key) in the bar, e.g. '1–6' */
	keyLabel?: string;
	hidden?: boolean;
	/** False: the action does not exist and the key keeps going down */
	when?: () => boolean;
	run: () => void | Promise<void>;
}

export interface Layer {
	id: number;
	actions: () => KeyAction[];
	modal: boolean;
	/** Nearest ancestor layer in the component tree */
	parent?: Layer;
}

export type KeyEventLike = Pick<
	KeyboardEvent,
	| 'key'
	| 'code'
	| 'shiftKey'
	| 'ctrlKey'
	| 'altKey'
	| 'metaKey'
	| 'defaultPrevented'
	| 'repeat'
	| 'target'
	| 'preventDefault'
>;

// Browser shortcuts (help, search, ...) that must never fire in the app.
export const ALWAYS_PREVENTED: readonly string[] = ['F1', 'F2', 'F3', 'F4', 'F7', 'F8', 'F9'];

// Holding these keys keeps moving; any other held key must not fire its action again.
const REPEATABLE: readonly string[] = ['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown'];

const BAR_ORDER = ['Enter', 'NumpadAdd', 'Escape', 'F2', 'F3', 'F4', 'F8', 'F9'];

const NAMES: Record<string, string> = {
	NumpadAdd: '+',
	Escape: 'Esc',
	PageDown: 'AvPág',
	PageUp: 'RePág',
	ArrowUp: '↑',
	ArrowDown: '↓'
};

export function isEditable(target: unknown): boolean {
	if (typeof target !== 'object' || target === null) return false;
	const t = target as { tagName?: string; isContentEditable?: boolean };
	return (
		t.tagName === 'INPUT' ||
		t.tagName === 'TEXTAREA' ||
		t.tagName === 'SELECT' ||
		t.isContentEditable === true
	);
}

function path(layer: Layer): number[] {
	const ids: number[] = [];
	for (let l: Layer | undefined = layer; l; l = l.parent) ids.unshift(l.id);
	return ids;
}

function compare(a: number[], b: number[]): number {
	for (let i = 0; i < Math.min(a.length, b.length); i++) {
		if (a[i] !== b[i]) return a[i] - b[i];
	}
	return a.length - b.length;
}

/**
 * Orders registered layers bottom to top following the component tree, not
 * registration timing: the root group first, then one group per modal layer
 * (a modal and its descendants). Modal groups stack by registration order (the
 * position in `layers`, which is append-on-add), except that a modal nested in
 * another modal's content always sits above the enclosing one. Inside a group
 * a parent precedes its descendants and siblings follow creation id.
 */
export function orderLayers(layers: readonly Layer[]): Layer[] {
	const modalOf = (layer: Layer | undefined): Layer | undefined => {
		for (let l = layer; l; l = l.parent) {
			if (l.modal && layers.includes(l)) return l;
		}
		return undefined;
	};
	const ranks = new Map<Layer, number>();
	const rank = (modal: Layer | undefined): number => {
		if (!modal) return -1;
		let r = ranks.get(modal);
		if (r === undefined) {
			r = Math.max(layers.indexOf(modal), rank(modalOf(modal.parent)) + 0.5);
			ranks.set(modal, r);
		}
		return r;
	};
	const keyed = layers.map((layer) => {
		const modal = modalOf(layer);
		return { layer, group: rank(modal), path: path(layer) };
	});
	keyed.sort((a, b) => a.group - b.group || compare(a.path, b.path));
	return keyed.map((k) => k.layer);
}

/** Layers from top to bottom, stopping after the first modal one. */
function* active(layers: readonly Layer[]): Generator<KeyAction> {
	const ordered = orderLayers(layers);
	for (let i = ordered.length - 1; i >= 0; i--) {
		yield* ordered[i].actions().filter((a) => a.when?.() !== false);
		if (ordered[i].modal) return;
	}
}

function matches(a: KeyAction, event: KeyEventLike): boolean {
	return (a.key === event.code || a.key === event.key) && (a.shift ?? false) === event.shiftKey;
}

export function dispatch(layers: readonly Layer[], event: KeyEventLike): boolean {
	if (event.defaultPrevented) return false;
	// Ctrl, Alt and Win combinations belong to Windows and Edge (Alt+F4 closes, Ctrl with + zooms).
	if (event.ctrlKey || event.altKey || event.metaKey) return false;
	const repeated =
		event.repeat && !REPEATABLE.includes(event.code) && !REPEATABLE.includes(event.key);
	const typing = isEditable(event.target);
	for (const a of active(layers)) {
		if (!matches(a, event)) continue;
		if (typing && a.key.length === 1) continue;
		// A held key keeps its default suppressed (no "+++" typed) but fires the action only once.
		event.preventDefault();
		if (repeated) return false;
		void a.run();
		return true;
	}
	if (ALWAYS_PREVENTED.includes(event.key) || ALWAYS_PREVENTED.includes(event.code)) {
		event.preventDefault();
	}
	return false;
}

export function visibleActions(layers: readonly Layer[]): KeyAction[] {
	const seen = new Set<string>();
	const result: KeyAction[] = [];
	for (const a of active(layers)) {
		const id = `${a.shift ? 'Shift+' : ''}${a.key}`;
		if (seen.has(id)) continue;
		// A hidden action still shadows lower ones: that is what dispatch runs.
		seen.add(id);
		if (!a.hidden) result.push(a);
	}
	const rank = (a: KeyAction) => {
		const i = BAR_ORDER.indexOf(a.key);
		return i === -1 ? BAR_ORDER.length : i;
	};
	// Array.sort is stable, so the rest keeps declaration order.
	return result.sort((a, b) => rank(a) - rank(b));
}

export function keyLabel(action: Pick<KeyAction, 'key' | 'shift' | 'keyLabel'>): string {
	if (action.keyLabel) return action.keyLabel;
	const name = NAMES[action.key] ?? action.key;
	return action.shift ? `Shift+${name}` : name;
}
