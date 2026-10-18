import { afterEach, describe, expect, it } from 'vitest';
import { selectOnFocus } from './select-on-focus';

/** Fires a cancelable mouse event on the node and says whether the browser's default was stopped. */
function fire(node: HTMLElement, type: 'mousedown' | 'mouseup'): boolean {
	const event = new MouseEvent(type, { bubbles: true, cancelable: true });
	node.dispatchEvent(event);
	return event.defaultPrevented;
}

describe('selectOnFocus', () => {
	let input: HTMLInputElement;
	let action: { destroy(): void };

	function setup(): void {
		input = document.createElement('input');
		input.value = 'texto';
		document.body.append(input);
		action = selectOnFocus(input);
	}

	afterEach(() => {
		action.destroy();
		input.remove();
	});

	it('selects everything on focus and keeps the selection through the entering click', () => {
		setup();
		fire(input, 'mousedown');
		input.focus();
		expect(input.selectionStart).toBe(0);
		expect(input.selectionEnd).toBe(input.value.length);
		// The browser would undo the selection on this mouseup.
		expect(fire(input, 'mouseup')).toBe(true);
	});

	it('a click on the field that already has the cursor is left alone', () => {
		setup();
		fire(input, 'mousedown');
		input.focus();
		fire(input, 'mouseup');
		expect(fire(input, 'mousedown')).toBe(false);
		expect(fire(input, 'mouseup')).toBe(false);
	});
});
