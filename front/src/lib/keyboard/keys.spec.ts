import { describe, expect, it, vi } from 'vitest';
import {
	dispatch,
	keyLabel,
	orderLayers,
	visibleActions,
	type KeyAction,
	type KeyEventLike,
	type Layer
} from './keys';

function ev(over: Partial<KeyEventLike> = {}): KeyEventLike {
	return {
		key: '',
		code: '',
		shiftKey: false,
		ctrlKey: false,
		altKey: false,
		metaKey: false,
		defaultPrevented: false,
		target: null,
		preventDefault: vi.fn(),
		...over
	};
}

let nextId = 0;
function layer(actions: KeyAction[], modal = false): Layer {
	return { id: nextId++, actions: () => actions, modal };
}

function act(key: string, label = key, extra: Partial<KeyAction> = {}): KeyAction {
	return { key, label, run: vi.fn(), ...extra };
}

describe('dispatch', () => {
	it('runs the action of the top layer and prevents default', () => {
		const page = act('Escape');
		const top = act('Escape');
		const e = ev({ key: 'Escape', code: 'Escape' });
		expect(dispatch([layer([page]), layer([top])], e)).toBe(true);
		expect(top.run).toHaveBeenCalledOnce();
		expect(page.run).not.toHaveBeenCalled();
		expect(e.preventDefault).toHaveBeenCalled();
	});

	it('falls through non-modal layers', () => {
		const page = act('Escape');
		const top = act('F3');
		expect(dispatch([layer([page]), layer([top])], ev({ key: 'Escape', code: 'Escape' }))).toBe(
			true
		);
		expect(page.run).toHaveBeenCalledOnce();
		expect(top.run).not.toHaveBeenCalled();
	});

	it('stops at a modal layer', () => {
		const page = act('NumpadAdd');
		const modal = act('Enter');
		const e = ev({ key: '+', code: 'NumpadAdd' });
		expect(dispatch([layer([page]), layer([modal], true)], e)).toBe(false);
		expect(page.run).not.toHaveBeenCalled();
		expect(modal.run).not.toHaveBeenCalled();
	});

	it('matches NumpadAdd by code only', () => {
		const a = act('NumpadAdd');
		expect(dispatch([layer([a])], ev({ key: '+', code: 'NumpadAdd' }))).toBe(true);
		expect(a.run).toHaveBeenCalledOnce();

		const b = act('NumpadAdd');
		const e = ev({ key: '+', code: 'BracketRight' });
		expect(dispatch([layer([b])], e)).toBe(false);
		expect(b.run).not.toHaveBeenCalled();
		expect(e.preventDefault).not.toHaveBeenCalled();
	});

	it('treats both Enter keys alike', () => {
		const a = act('Enter');
		expect(dispatch([layer([a])], ev({ key: 'Enter', code: 'NumpadEnter' }))).toBe(true);
		expect(a.run).toHaveBeenCalledOnce();
	});

	it('requires the declared shift state', () => {
		const plain = act('Enter');
		expect(dispatch([layer([plain])], ev({ key: 'Enter', code: 'Enter', shiftKey: true }))).toBe(
			false
		);
		expect(plain.run).not.toHaveBeenCalled();

		const shifted = act('Enter', 'Enter', { shift: true });
		expect(dispatch([layer([shifted])], ev({ key: 'Enter', code: 'Enter', shiftKey: true }))).toBe(
			true
		);
		expect(shifted.run).toHaveBeenCalledOnce();
	});

	it('ignores single-character keys while typing', () => {
		const a = act('1');
		const typing = ev({
			key: '1',
			code: 'Digit1',
			target: { tagName: 'INPUT' } as unknown as EventTarget
		});
		expect(dispatch([layer([a])], typing)).toBe(false);
		expect(a.run).not.toHaveBeenCalled();
		expect(dispatch([layer([a])], ev({ key: '1', code: 'Numpad1', target: null }))).toBe(true);
		expect(a.run).toHaveBeenCalledOnce();
	});

	it('skips actions whose when() is false', () => {
		const hidden = act('Escape', 'Escape', { when: () => false });
		const below = act('Escape');
		expect(dispatch([layer([below]), layer([hidden])], ev({ key: 'Escape', code: 'Escape' }))).toBe(
			true
		);
		expect(hidden.run).not.toHaveBeenCalled();
		expect(below.run).toHaveBeenCalledOnce();
	});

	it('ignores events a component already handled', () => {
		const a = act('Escape');
		const e = ev({ key: 'Escape', code: 'Escape', defaultPrevented: true });
		expect(dispatch([layer([a])], e)).toBe(false);
		expect(a.run).not.toHaveBeenCalled();
	});

	it('blocks browser F keys even without an action', () => {
		for (const k of ['F1', 'F3', 'F7']) {
			const e = ev({ key: k, code: k });
			expect(dispatch([], e)).toBe(false);
			expect(e.preventDefault).toHaveBeenCalled();
		}
		const f5 = ev({ key: 'F5', code: 'F5' });
		expect(dispatch([], f5)).toBe(false);
		expect(f5.preventDefault).not.toHaveBeenCalled();
	});

	it('lets Alt+F4 and Ctrl+F4 through, with or without an F4 action', () => {
		for (const layers of [[], [layer([act('F4')])]]) {
			for (const held of [{ altKey: true }, { ctrlKey: true }]) {
				const e = ev({ key: 'F4', code: 'F4', ...held });
				expect(dispatch(layers, e)).toBe(false);
				expect(e.preventDefault).not.toHaveBeenCalled();
			}
		}
		const alone = ev({ key: 'F4', code: 'F4' });
		expect(dispatch([], alone)).toBe(false);
		expect(alone.preventDefault).toHaveBeenCalled();
	});

	it('runs nothing while Ctrl, Alt or Win is held', () => {
		const save = act('NumpadAdd', 'Guardar');
		const zoom = ev({ key: '+', code: 'NumpadAdd', ctrlKey: true });
		expect(dispatch([layer([save])], zoom)).toBe(false);
		expect(save.run).not.toHaveBeenCalled();
		expect(zoom.preventDefault).not.toHaveBeenCalled();

		const exit = act('Escape');
		const winEscape = ev({ key: 'Escape', code: 'Escape', metaKey: true });
		expect(dispatch([layer([exit])], winEscape)).toBe(false);
		expect(exit.run).not.toHaveBeenCalled();
		expect(winEscape.preventDefault).not.toHaveBeenCalled();
	});
});

describe('visibleActions', () => {
	it('lists visible actions in fixed order, upper layers winning', () => {
		const page = layer([act('Escape', 'Salir'), act('NumpadAdd', 'Guardar')]);
		const form = layer([
			act('Enter', 'Siguiente'),
			act('Enter', 'Anterior', { shift: true, hidden: true })
		]);
		expect(visibleActions([page, form]).map((a) => a.label)).toEqual([
			'Siguiente',
			'Guardar',
			'Salir'
		]);
	});

	it('stops visible actions at the modal layer', () => {
		const page = layer([act('NumpadAdd', 'Guardar')]);
		const modal = layer([act('Enter', 'Seguir editando'), act('F8', 'Salir sin guardar')], true);
		expect(visibleActions([page, modal]).map((a) => a.label)).toEqual([
			'Seguir editando',
			'Salir sin guardar'
		]);
	});
	it('shows and runs the actions of a modal created first but registered last', () => {
		const page = layer([act('NumpadAdd', 'Emitir')]);
		const confirm = layer([act('Enter', 'Confirmar')], true);
		const errorAction = act('Enter', 'Cerrar error');
		const error = layer([errorAction], true);
		const layers = [page, confirm, error];
		error.id = 2;
		confirm.id = 11;
		expect(visibleActions(layers).map((a) => a.label)).toEqual(['Cerrar error']);
		expect(dispatch(layers, ev({ key: 'Enter', code: 'Enter' }))).toBe(true);
		expect(errorAction.run).toHaveBeenCalledOnce();
	});

	it('does not let a hidden upper action leave a lower one in the bar', () => {
		const lower = layer([act('Escape', 'Salir')]);
		const upper = layer([act('Escape', 'x', { hidden: true })]);
		expect(visibleActions([lower, upper])).toEqual([]);
	});
});

describe('keyLabel', () => {
	it('names keys in spanish', () => {
		const l = (key: string, extra: Partial<KeyAction> = {}) => keyLabel({ key, ...extra });
		expect(l('NumpadAdd')).toBe('+');
		expect(l('Escape')).toBe('Esc');
		expect(l('Enter')).toBe('Enter');
		expect(l('Enter', { shift: true })).toBe('Shift+Enter');
		expect(l('PageDown')).toBe('AvPág');
		expect(l('PageUp')).toBe('RePág');
		expect(l('ArrowUp')).toBe('↑');
		expect(l('ArrowDown')).toBe('↓');
		expect(l('F3')).toBe('F3');
		expect(l('1', { keyLabel: '1–6' })).toBe('1–6');
	});
});

describe('orderLayers', () => {
	function node(id: number, over: Partial<Layer> = {}): Layer {
		return { id, actions: () => [], modal: false, ...over };
	}
	const ids = (ls: Layer[]) => orderLayers(ls).map((l) => l.id);

	it('puts a child above its parent regardless of id order', () => {
		const parent = node(5);
		const child = node(2, { parent });
		expect(ids([child, parent])).toEqual([5, 2]);
	});

	it('keeps modal descendants above the modal and later outsiders below it', () => {
		const modal = node(3, { modal: true });
		const content = node(4, { parent: modal });
		const outsider = node(9);
		expect(ids([outsider, content, modal])).toEqual([9, 3, 4]);
	});

	it('puts the modal registered last on top, whatever its id', () => {
		const early = node(2, { modal: true });
		const late = node(11, { modal: true });
		expect(ids([late, early])).toEqual([11, 2]);
		expect(ids([early, late])).toEqual([2, 11]);
	});

	it('puts a re-added modal back on top', () => {
		const a = node(1, { modal: true });
		const b = node(2, { modal: true });
		expect(ids([a, b])).toEqual([1, 2]);
		expect(ids([b, a])).toEqual([2, 1]);
	});

	it('keeps a nested modal above its enclosing modal even when it registered first', () => {
		const outer = node(3, { modal: true });
		const inner = node(4, { modal: true, parent: outer });
		expect(ids([inner, outer])).toEqual([3, 4]);
	});

	it('groups a non-modal layer with its registered modal ancestor', () => {
		const modal = node(3, { modal: true });
		const mid = node(4, { parent: modal });
		const leaf = node(5, { parent: mid });
		const later = node(6);
		expect(ids([leaf, later, modal])).toEqual([6, 3, 5]);
	});
});
