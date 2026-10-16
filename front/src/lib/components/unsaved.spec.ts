import { describe, expect, it, vi } from 'vitest';
import { createUnsavedGuard, type PendingNavigation } from './unsaved';

const url = new URL('http://localhost/albaranes');

function setup(
	options: { dirty?: boolean; save?: () => Promise<boolean>; goto?: () => Promise<void> } = {}
) {
	const goto = vi.fn(options.goto ?? (() => Promise.resolve()));
	const onAsk = vi.fn();
	const guard = createUnsavedGuard({
		isDirty: () => options.dirty ?? true,
		save: options.save ?? (() => Promise.resolve(true)),
		goto,
		onAsk
	});
	const nav = (willUnload = false): PendingNavigation => ({
		to: { url },
		willUnload,
		cancel: vi.fn()
	});
	return { guard, goto, onAsk, nav };
}

describe('unsaved changes guard', () => {
	it('lets clean forms leave', () => {
		const { guard, onAsk, nav } = setup({ dirty: false });
		const n = nav();
		guard.check(n);
		expect(n.cancel).not.toHaveBeenCalled();
		expect(onAsk).not.toHaveBeenCalled();
	});

	it('stops a dirty form and asks', () => {
		const { guard, onAsk, nav } = setup();
		const n = nav();
		guard.check(n);
		expect(n.cancel).toHaveBeenCalledOnce();
		expect(onAsk).toHaveBeenCalledWith(true);
	});

	it('closing the window with changes uses the browser warning', () => {
		const { guard, onAsk, nav } = setup();
		const n = nav(true);
		guard.check(n);
		expect(n.cancel).toHaveBeenCalledOnce();
		expect(onAsk).not.toHaveBeenCalled();
	});

	it('keep editing closes the question', () => {
		const { guard, goto, onAsk, nav } = setup();
		guard.check(nav());
		guard.keepEditing();
		expect(onAsk).toHaveBeenLastCalledWith(false);
		expect(goto).not.toHaveBeenCalled();
	});

	it('discard leaves to the requested page', async () => {
		let during: PendingNavigation | undefined;
		const { guard, goto, onAsk, nav } = setup();
		goto.mockImplementation(() => {
			during = nav();
			guard.check(during);
			return Promise.resolve();
		});
		guard.check(nav());
		await guard.discard();
		expect(onAsk).toHaveBeenLastCalledWith(false);
		expect(goto).toHaveBeenCalledWith(url);
		expect(during?.cancel).not.toHaveBeenCalled();
	});

	it('save and leave only leaves when saving works', async () => {
		const save = vi.fn().mockResolvedValueOnce(false).mockResolvedValueOnce(true);
		const { guard, goto, nav } = setup({ save });
		guard.check(nav());
		await guard.saveAndLeave();
		expect(goto).not.toHaveBeenCalled();
		await guard.saveAndLeave();
		expect(goto).toHaveBeenCalledWith(url);
	});

	it('ignores save and leave while a save is in flight', async () => {
		let resolve!: (ok: boolean) => void;
		const save = vi.fn(() => new Promise<boolean>((r) => (resolve = r)));
		const { guard, goto, nav } = setup({ save });
		guard.check(nav());
		const first = guard.saveAndLeave();
		const second = guard.saveAndLeave();
		resolve(true);
		await Promise.all([first, second]);
		expect(save).toHaveBeenCalledOnce();
		expect(goto).toHaveBeenCalledOnce();
	});
});
