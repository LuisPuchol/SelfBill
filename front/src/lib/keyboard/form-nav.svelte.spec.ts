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

	it('focusFirstField focuses the first in page order', () => {
		const [a, b, c] = inputs('a', 'b', 'c');
		const nav = new FormNav();
		for (const el of [c, b, a]) nav.register({ name: el.name, el });

		nav.focusFirstField();
		expect(document.activeElement).toBe(a);
	});

	it('restoreFocus goes back to the last focused field', () => {
		const [a, b, outside] = inputs('a', 'b', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a });
		nav.register({ name: 'b', el: b });

		b.focus();
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(b);
	});

	it('restoreFocus falls back to the first field, also after the last one unregistered', () => {
		const [a, b, outside] = inputs('a', 'b', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a });
		const unregister = nav.register({ name: 'b', el: b });

		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(a);

		b.focus();
		unregister();
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(a);
	});

	it('restoreFocus goes to the field that failed validateAll behind a dialog, once', async () => {
		const [a, b, c, outside] = inputs('a', 'b', 'c', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit: () => true });
		nav.register({ name: 'b', el: b, commit: () => false });
		nav.register({ name: 'c', el: c, commit: () => true });

		c.focus();
		// A modal dialog takes the cursor and makes the form inert: validateAll cannot reach b.
		outside.focus();
		for (const el of [a, b, c]) el.inert = true;
		expect(await nav.validateAll()).toBe(false);
		expect(document.activeElement).toBe(outside);
		for (const el of [a, b, c]) el.inert = false;
		// The dialog closes and gives the focus back to where it was.
		c.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(b);

		// Used up: next time, the last field with the cursor.
		c.focus();
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(c);
	});

	it('a failure the cursor could reach is not remembered: moving on wins', async () => {
		const [a, b, c, outside] = inputs('a', 'b', 'c', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit: () => true });
		nav.register({ name: 'b', el: b, commit: () => false });
		nav.register({ name: 'c', el: c, commit: () => true });

		a.focus();
		expect(await nav.validateAll()).toBe(false);
		expect(document.activeElement).toBe(b);
		c.focus();
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(c);
	});

	it('typing forgets a failure that was waiting for restoreFocus', async () => {
		const [a, b, outside] = inputs('a', 'b', 'outside');
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit: () => false });
		nav.register({ name: 'b', el: b, commit: () => true });

		b.focus();
		outside.focus();
		for (const el of [a, b]) el.inert = true;
		expect(await nav.validateAll()).toBe(false);
		for (const el of [a, b]) el.inert = false;
		b.focus();
		b.dispatchEvent(new Event('input', { bubbles: true }));
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(b);
	});

	it('a passing validateAll forgets the earlier failure', async () => {
		const [a, b, outside] = inputs('a', 'b', 'outside');
		let ok = false;
		const nav = new FormNav();
		nav.register({ name: 'a', el: a, commit: () => true });
		nav.register({ name: 'b', el: b, commit: () => ok });

		a.focus();
		// Behind a dialog: the failure waits for restoreFocus.
		b.inert = true;
		expect(await nav.validateAll()).toBe(false);
		b.inert = false;
		ok = true;
		a.focus();
		expect(await nav.validateAll()).toBe(true);
		outside.focus();
		nav.restoreFocus();
		expect(document.activeElement).toBe(a);
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

	describe('validateAll', () => {
		it('runs every commit so every error shows, and puts the cursor on the first that fails', async () => {
			const [a, b, c] = inputs('a', 'b', 'c');
			const commits = [vi.fn(() => true), vi.fn(() => false), vi.fn(() => false)];
			const nav = new FormNav();
			// Registered out of page order: the cursor still goes to the first failing in the page.
			nav.register({ name: 'c', el: c, commit: commits[2] });
			nav.register({ name: 'a', el: a, commit: commits[0] });
			nav.register({ name: 'b', el: b, commit: commits[1] });
			a.focus();

			expect(await nav.validateAll()).toBe(false);
			for (const commit of commits) expect(commit).toHaveBeenCalledOnce();
			expect(document.activeElement).toBe(b);
		});

		it('is true when every field passes, and leaves the cursor be', async () => {
			const [a, b, c] = inputs('a', 'b', 'c');
			const nav = new FormNav();
			nav.register({ name: 'a', el: a, commit: () => true });
			nav.register({ name: 'b', el: b });
			nav.register({ name: 'c', el: c, commit: () => true });
			c.focus();

			expect(await nav.validateAll()).toBe(true);
			expect(document.activeElement).toBe(c);
		});

		it('waits for an async commit', async () => {
			const [a, b] = inputs('a', 'b');
			let resolve!: (ok: boolean) => void;
			const nav = new FormNav();
			nav.register({ name: 'a', el: a, commit: () => new Promise((r) => (resolve = r)) });
			nav.register({ name: 'b', el: b, commit: () => true });
			b.focus();

			const result = nav.validateAll();
			resolve(false);
			expect(await result).toBe(false);
			expect(document.activeElement).toBe(a);
		});

		it('is busy while it runs: next() does nothing and another validateAll waits its turn', async () => {
			const [a, b] = inputs('a', 'b');
			let resolve!: (ok: boolean) => void;
			const slow = vi.fn(() => new Promise<boolean>((r) => (resolve = r)));
			const other = vi.fn(() => true);
			const nav = new FormNav();
			nav.register({ name: 'a', el: a, commit: slow });
			nav.register({ name: 'b', el: b, commit: other });
			a.focus();

			const first = nav.validateAll();
			expect(nav.busy).toBe(true);
			const second = nav.validateAll();
			await nav.next();
			expect(slow).toHaveBeenCalledOnce();
			expect(document.activeElement).toBe(a);

			resolve(true);
			expect(await first).toBe(true);
			// The second one runs the commits only after the first finished.
			expect(slow).toHaveBeenCalledTimes(2);
			resolve(true);
			expect(await second).toBe(true);
			expect(other).toHaveBeenCalledTimes(2);
			expect(nav.busy).toBe(false);
		});

		it('waits for a commit that is still running', async () => {
			const [a, b] = inputs('a', 'b');
			let resolve!: (ok: boolean) => void;
			const slow = vi.fn(() => new Promise<boolean>((r) => (resolve = r)));
			const nav = new FormNav();
			nav.register({ name: 'a', el: a, commit: slow });
			nav.register({ name: 'b', el: b });
			a.focus();

			void nav.next();
			let settled: boolean | undefined;
			const result = nav.validateAll().then((ok) => (settled = ok));
			await Promise.resolve();
			expect(settled).toBeUndefined();
			expect(slow).toHaveBeenCalledOnce();

			resolve(true);
			await vi.waitFor(() => expect(slow).toHaveBeenCalledTimes(2));
			resolve(true);
			await result;
			expect(settled).toBe(true);
		});
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
