import { describe, expect, it, vi } from 'vitest';
import { DetailLoader } from '#lib/components/detail-loader.svelte.js';

/** A load whose answers the test settles by hand, key by key. */
function setup() {
	const pending = new Map<number, { resolve: (v: string) => void; reject: (e: unknown) => void }>();
	const load = vi.fn(
		(key: number) =>
			new Promise<string>((resolve, reject) => {
				pending.set(key, { resolve, reject });
			})
	);
	const onError = vi.fn();
	const loader = new DetailLoader<number, string>(load, onError);
	const settle = async (key: number, value: string) => {
		pending.get(key)!.resolve(value);
		await new Promise((r) => setTimeout(r, 0));
	};
	const fail = async (key: number, err: unknown) => {
		pending.get(key)!.reject(err);
		await new Promise((r) => setTimeout(r, 0));
	};
	return { loader, load, onError, settle, fail };
}

describe('DetailLoader', () => {
	it('shows the answer of the selected key', async () => {
		const { loader, settle } = setup();
		loader.select(1);
		expect(loader.get(1)).toBeUndefined();
		await settle(1, 'one');
		expect(loader.get(1)).toBe('one');
		expect(loader.get(2)).toBeUndefined();
	});

	it('serves a cached key without asking again', async () => {
		const { loader, load, settle } = setup();
		loader.select(1);
		await settle(1, 'one');
		loader.select(2);
		loader.select(1);
		expect(loader.get(1)).toBe('one');
		expect(load).toHaveBeenCalledTimes(2);
		expect(load).toHaveBeenCalledWith(2);
		expect(load.mock.calls.filter(([k]) => k === 1)).toHaveLength(1);
	});

	it('drops the answer of a key already left', async () => {
		const { loader, load, settle } = setup();
		loader.select(1);
		loader.select(2);
		await settle(1, 'one');
		expect(loader.get(1)).toBeUndefined();
		expect(loader.get(2)).toBeUndefined();
		await settle(2, 'two');
		expect(loader.get(2)).toBe('two');
		loader.select(1);
		expect(loader.get(1)).toBe('one');
		expect(load.mock.calls.filter(([k]) => k === 1)).toHaveLength(1);
	});

	it('drops an answer that started before clear', async () => {
		const { loader, load, settle } = setup();
		loader.select(1);
		loader.clear();
		await settle(1, 'one');
		expect(loader.get(1)).toBeUndefined();
		loader.select(1);
		expect(load.mock.calls.filter(([k]) => k === 1)).toHaveLength(2);
	});

	it('forgets what it showed on clear', async () => {
		const { loader, settle } = setup();
		loader.select(1);
		await settle(1, 'one');
		loader.clear();
		expect(loader.get(1)).toBeUndefined();
	});

	it('reports only the latest error', async () => {
		const { loader, onError, fail } = setup();
		loader.select(1);
		loader.select(2);
		await fail(1, new Error('one'));
		expect(onError).not.toHaveBeenCalled();
		await fail(2, new Error('two'));
		expect(onError).toHaveBeenCalledTimes(1);
	});

	it('no key, no detail', () => {
		const { loader, load } = setup();
		loader.select(undefined);
		expect(loader.get(undefined)).toBeUndefined();
		expect(load).not.toHaveBeenCalled();
	});
});
