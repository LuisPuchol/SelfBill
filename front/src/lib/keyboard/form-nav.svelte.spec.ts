import { afterEach, describe, expect, it, vi } from 'vitest';
import { FormNav } from './form-nav';

const created: HTMLElement[] = [];

/** Inputs appended to the page in call order. */
function inputs(...names: string[]): HTMLInputElement[] {
	return names.map((name) => {
		const el = document.createElement('input');
		el.name = name;
		document.body.append(el);
		created.push(el);
		return el;
	});
}

afterEach(() => {
	for (const el of created) el.remove();
	created.length = 0;
});

describe('FormNav', () => {
	it('waits for a running commit before accepting another next()', async () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		let resolve!: (ok: boolean) => void;
		const commit = vi.fn(() => new Promise<boolean>((r) => (resolve = r)));
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit });
		nav.register({ name: 'b', el: b });
		nav.register({ name: 'c', el: c });
		a.focus();

		const first = nav.next();
		expect(nav.busy).toBe(true);
		const second = nav.next();
		expect(commit).toHaveBeenCalledOnce();

		resolve(true);
		await Promise.all([first, second]);
		expect(document.activeElement).toBe(b);
		expect(nav.busy).toBe(false);
	});

	it('orders fields by their place in the page, not by registration', async () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		const nav = new FormNav();
		for (const el of [c, a, b]) nav.register({ name: el.name, el });
		a.focus();

		await nav.next();
		expect(document.activeElement).toBe(b);
		await nav.next();
		expect(document.activeElement).toBe(c);
		nav.prev();
		expect(document.activeElement).toBe(b);
	});

	it('focusFirst picks the first in page order', () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		const nav = new FormNav();
		for (const el of [c, b, a]) nav.register({ name: el.name, el });

		nav.focusFirst(['c', 'b']);
		expect(document.activeElement).toBe(b);
	});

	it('calls onLast on the last field and skips fields that unregistered', async () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		const onLast = vi.fn();
		const nav = new FormNav({ onLast });
		nav.register({ name: 'a', el: a });
		const unregister = nav.register({ name: 'b', el: b });
		nav.register({ name: 'c', el: c });
		unregister();
		a.focus();

		await nav.next();
		expect(document.activeElement).toBe(c);
		expect(onLast).not.toHaveBeenCalled();
		await nav.next();
		expect(onLast).toHaveBeenCalledOnce();
		expect(document.activeElement).toBe(c);
	});

	it('does not move the cursor if it left the field during the commit', async () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		let resolve!: (ok: boolean) => void;
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit: () => new Promise((r) => (resolve = r)) });
		nav.register({ name: 'b', el: b });
		nav.register({ name: 'c', el: c });
		a.focus();

		const pending = nav.next();
		c.focus();
		resolve(true);
		await pending;
		expect(document.activeElement).toBe(c);
	});

	it('knows whether the cursor is in one of its fields', () => {
		const [a, outside] = inputs('a', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a });

		outside.focus();
		expect(nav.hasFocus()).toBe(false);
		nav.focus('a');
		expect(document.activeElement).toBe(a);
		expect(nav.hasFocus()).toBe(true);
	});
});
