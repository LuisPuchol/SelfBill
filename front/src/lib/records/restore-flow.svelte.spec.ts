import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '#lib/api/errors.js';
import { connectionRestored, dismissError, errorState } from '#lib/components/errors.svelte.js';
import { RestoreFlow, type RestoreTarget } from '#lib/records/restore-flow.svelte.js';

const target: RestoreTarget = { id: 5, code: 101, name: 'Tomate' };

function setup(restore: (id: number) => Promise<void> = async () => {}) {
	const spy = vi.fn(restore);
	const toast = vi.fn();
	const onRestored = vi.fn();
	const onFailed = vi.fn();
	const flow = new RestoreFlow({
		entity: { one: 'artículo', Label: 'Artículo' },
		restore: spy,
		toast,
		onRestored,
		onFailed
	});
	return { flow, restore: spy, toast, onRestored, onFailed };
}

beforeEach(() => {
	connectionRestored();
	dismissError();
});

describe('RestoreFlow', () => {
	it('asks with the code and the name', () => {
		const { flow, restore } = setup();
		flow.ask(target);
		expect(flow.dialog).toEqual({ title: '¿Restaurar el artículo 101?', name: 'Tomate' });
		expect(restore).not.toHaveBeenCalled();
	});

	it('confirm restores, toasts and reports', async () => {
		const { flow, restore, toast, onRestored, onFailed } = setup();
		flow.ask(target);
		await flow.confirm();
		expect(restore).toHaveBeenCalledExactlyOnceWith(5);
		expect(toast).toHaveBeenCalledExactlyOnceWith('Artículo 101 restaurado');
		expect(onRestored).toHaveBeenCalledWith(target);
		expect(onFailed).not.toHaveBeenCalled();
		expect(flow.dialog).toBeNull();
	});

	it('a code taken by a live one closes the question, says so and reports it', async () => {
		const { flow, toast, onRestored, onFailed } = setup(async () => {
			throw new ApiError(409, 'ARTICLE_DUPLICATE_CODE', 'Duplicate', {});
		});
		flow.ask(target);
		await flow.confirm();
		expect(flow.dialog).toBeNull();
		expect(errorState.view).toMatchObject({ message: 'Ya existe un artículo con ese código.' });
		expect(onFailed).toHaveBeenCalledWith(target);
		expect(onRestored).not.toHaveBeenCalled();
		expect(toast).not.toHaveBeenCalled();
	});

	it('a second confirm while restoring does nothing', async () => {
		let finish!: () => void;
		const { flow, restore } = setup(() => new Promise<void>((resolve) => (finish = resolve)));
		flow.ask(target);
		const first = flow.confirm();
		const second = flow.confirm();
		finish();
		await Promise.all([first, second]);
		expect(restore).toHaveBeenCalledOnce();
	});

	it('a question already open is not replaced', () => {
		const { flow } = setup();
		flow.ask(target);
		flow.ask({ id: 6, code: 102, name: 'Pepino' });
		expect(flow.dialog?.title).toBe('¿Restaurar el artículo 101?');
	});

	it('cancel closes without restoring', async () => {
		const { flow, restore } = setup();
		flow.ask(target);
		flow.cancel();
		expect(flow.dialog).toBeNull();
		await flow.confirm();
		expect(restore).not.toHaveBeenCalled();
	});

	it('a feminine entity takes the feminine article and participle', async () => {
		const toast = vi.fn();
		const flow = new RestoreFlow({
			entity: { one: 'factura', Label: 'Factura', feminine: true },
			restore: async () => {},
			toast,
			onRestored: vi.fn()
		});
		flow.ask({ id: 1, code: 'S-12', name: 'Frutas Paco' });
		expect(flow.dialog?.title).toBe('¿Restaurar la factura S-12?');
		await flow.confirm();
		expect(toast).toHaveBeenCalledWith('Factura S-12 restaurada');
	});
});
