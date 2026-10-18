import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '#lib/api/errors.js';
import { errorState } from '#lib/components/errors.svelte.js';
import { DeleteFlow, type DeleteTarget } from '#lib/records/delete-flow.svelte.js';

const target: DeleteTarget = { id: 5, code: 101, name: 'Tomate' };

function setup(
	overrides: {
		remove?: (id: number) => Promise<void>;
		pendingForDelete?: (id: number) => Promise<string | null>;
	} = {}
) {
	const remove = vi.fn(overrides.remove ?? (async () => {}));
	const pendingForDelete = vi.fn(overrides.pendingForDelete ?? (async () => null));
	const toast = vi.fn();
	const onDeleted = vi.fn();
	const onFailed = vi.fn();
	const flow = new DeleteFlow({
		entity: { one: 'artículo', Label: 'Artículo' },
		remove,
		pendingForDelete,
		toast,
		onDeleted,
		onFailed
	});
	return { flow, remove, pendingForDelete, toast, onDeleted, onFailed };
}

const failure = () => new ApiError(409, 'INVOICE_MIXED_MONTHS', 'Detail EN', {});

beforeEach(() => {
	errorState.view = null;
	errorState.connectionLost = false;
});

describe('DeleteFlow', () => {
	it('asks with the name and the pending text', async () => {
		const { flow } = setup({ pendingForDelete: async () => 'Está en 3…' });
		await flow.ask(target);
		expect(flow.dialog).toEqual({
			title: '¿Borrar el artículo 101?',
			name: 'Tomate',
			body: 'Está en 3…'
		});

		const none = setup();
		await none.flow.ask(target);
		expect(none.flow.dialog?.body).toBeNull();
	});

	it('a failed count shows the error and asks nothing', async () => {
		const { flow } = setup({
			pendingForDelete: async () => {
				throw failure();
			}
		});
		await flow.ask(target);
		expect(flow.dialog).toBeNull();
		expect(errorState.view).not.toBeNull();
	});

	it('confirm removes, toasts and reports', async () => {
		const { flow, remove, toast, onDeleted } = setup();
		await flow.ask(target);
		await flow.confirm();
		expect(remove).toHaveBeenCalledOnce();
		expect(remove).toHaveBeenCalledWith(5);
		expect(toast).toHaveBeenCalledWith('Artículo 101 borrado');
		expect(onDeleted).toHaveBeenCalledWith(target);
		expect(flow.dialog).toBeNull();
	});

	it('a failed remove closes the question, shows the error and reports it', async () => {
		const { flow, onDeleted, onFailed } = setup({
			remove: async () => {
				throw failure();
			}
		});
		await flow.ask(target);
		await flow.confirm();
		expect(flow.dialog).toBeNull();
		expect(errorState.view).not.toBeNull();
		expect(onFailed).toHaveBeenCalledWith(target);
		expect(onDeleted).not.toHaveBeenCalled();
	});

	it('a second confirm while removing does nothing', async () => {
		let finish!: () => void;
		const { flow, remove } = setup({
			remove: () => new Promise<void>((resolve) => (finish = resolve))
		});
		await flow.ask(target);
		const first = flow.confirm();
		const second = flow.confirm();
		finish();
		await Promise.all([first, second]);
		expect(remove).toHaveBeenCalledOnce();
	});

	it('a second ask while counting does nothing', async () => {
		let finish!: (body: string | null) => void;
		const { flow, pendingForDelete } = setup({
			pendingForDelete: () => new Promise((resolve) => (finish = resolve))
		});
		const first = flow.ask(target);
		const second = flow.ask(target);
		finish(null);
		await Promise.all([first, second]);
		expect(pendingForDelete).toHaveBeenCalledOnce();
	});

	it('cancel closes without removing', async () => {
		const { flow, remove } = setup();
		await flow.ask(target);
		flow.cancel();
		expect(flow.dialog).toBeNull();
		expect(remove).not.toHaveBeenCalled();
	});
});
