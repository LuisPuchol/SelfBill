import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import RecordHarness from '#lib/test/RecordHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import type { Saved } from '#lib/records/record-form.svelte.js';

interface ArticleValues {
	code: number | null;
	name: string;
}

function options() {
	return {
		create: vi.fn<(payload: unknown) => Promise<Saved>>().mockResolvedValue({ id: 5, code: 101 }),
		update: vi
			.fn<(id: number, payload: unknown) => Promise<Saved>>()
			.mockResolvedValue({ id: 5, code: 101 }),
		remove: vi.fn<(id: number) => Promise<void>>().mockResolvedValue(undefined),
		blank: vi.fn<() => Promise<ArticleValues>>().mockResolvedValue({ code: 102, name: '' }),
		pendingForDelete: vi.fn<(id: number) => Promise<string | null>>().mockResolvedValue(null),
		goToList: vi.fn<(savedId?: number) => void>(),
		toast: vi.fn<(message: string) => void>()
	};
}

/** An edit of article 101, "Tomate". */
const editing = { id: 5, initial: { code: 101, name: 'Tomate' } };

function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

const code = () => page.getByLabelText('Código');
const name = () => page.getByLabelText('Nombre');
const dialog = () => page.getByRole('dialog');
const bar = () => page.getByRole('contentinfo');

describe('RecordScreen', () => {
	it('the cursor starts in the first field', async () => {
		await render(RecordHarness, options());
		await expect
			.element(page.getByRole('heading', { level: 1, name: 'Artículo nuevo' }))
			.toBeVisible();
		await expect.element(code()).toHaveFocus();
		// A new record cannot be deleted yet.
		await expect.element(bar()).toHaveTextContent('+ Guardar');
		await expect.element(bar()).not.toHaveTextContent('Borrar');
	});

	it('saving checks every field first', async () => {
		const o = options();
		await render(RecordHarness, o);
		await expect.element(code()).toHaveFocus();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(name()).toHaveFocus();
		expect(o.create).not.toHaveBeenCalled();
	});

	it('a repeated code shows under the code field', async () => {
		const o = options();
		o.create.mockRejectedValue(new ApiError(409, 'ARTICLE_DUPLICATE_CODE', '', {}));
		await render(RecordHarness, o);
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await expect.element(page.getByText('Ya existe un artículo con ese código')).toBeVisible();
		await expect
			.element(code())
			.toHaveAccessibleDescription('Ya existe un artículo con ese código');
		await expect.element(code()).toHaveFocus();
		expect(o.toast).not.toHaveBeenCalled();
	});

	it('a new record chains into another new one', async () => {
		const o = options();
		await render(RecordHarness, o);
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await vi.waitFor(() =>
			expect(o.toast).toHaveBeenCalledExactlyOnceWith('Artículo 101 guardado')
		);
		expect(o.create).toHaveBeenCalledExactlyOnceWith({ code: 101, name: 'Tomate' });
		await expect.element(code()).toHaveValue('102');
		await expect.element(name()).toHaveValue('');
		await expect.element(code()).toHaveFocus();
		expect(o.goToList).not.toHaveBeenCalled();
	});

	it('the chained blank form is clean', async () => {
		const o = options();
		await render(RecordHarness, o);
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
		await name().click();
		await userEvent.keyboard('Tomate');
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('true');
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(code()).toHaveValue('102');
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
		// Walking through the blank form does not change it either.
		await userEvent.keyboard('{Enter}');
		await expect.element(name()).toHaveFocus();
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
	});

	it('a saved new record is clean while the next one is on its way', async () => {
		const o = options();
		const next = deferred<ArticleValues>();
		o.blank.mockReturnValue(next.promise);
		await render(RecordHarness, o);
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await vi.waitFor(() =>
			expect(o.toast).toHaveBeenCalledExactlyOnceWith('Artículo 101 guardado')
		);
		await vi.waitFor(() => expect(o.blank).toHaveBeenCalledOnce());
		// Saved, so leaving now must not ask about unsaved changes.
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
		await expect.element(name()).toHaveValue('Tomate');
		// Still saving: "+" does nothing until the next record arrives.
		await userEvent.keyboard('{NumpadAdd}');
		expect(o.create).toHaveBeenCalledOnce();
		next.resolve({ code: 102, name: '' });
		await expect.element(code()).toHaveValue('102');
		await expect.element(name()).toHaveValue('');
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
		expect(o.create).toHaveBeenCalledOnce();
		expect(o.toast).toHaveBeenCalledOnce();
	});

	it('an edit goes back to the list', async () => {
		const o = options();
		await render(RecordHarness, { ...o, ...editing });
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(o.goToList).toHaveBeenCalledExactlyOnceWith(5));
		expect(o.update).toHaveBeenCalledExactlyOnceWith(5, { code: 101, name: 'Tomate' });
		expect(o.toast).toHaveBeenCalledExactlyOnceWith('Artículo 101 guardado');
		expect(o.create).not.toHaveBeenCalled();
		await expect.element(page.getByTestId('dirty')).toHaveTextContent('false');
	});

	it('a second + while saving does nothing', async () => {
		const o = options();
		const saving = deferred<Saved>();
		o.create.mockReturnValue(saving.promise);
		await render(RecordHarness, o);
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await vi.waitFor(() => expect(o.create).toHaveBeenCalledOnce());
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');
		saving.resolve({ id: 5, code: 101 });
		await expect.element(code()).toHaveValue('102');
		expect(o.create).toHaveBeenCalledOnce();
		expect(o.blank).toHaveBeenCalledOnce();
		expect(o.toast).toHaveBeenCalledOnce();
	});

	it('API field errors show and clear when the field changes', async () => {
		const o = options();
		o.create.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', '', { name: 'x' }));
		await render(RecordHarness, { ...o, fieldMessages: { name: 'Pon un nombre' } });
		await name().click();
		await userEvent.keyboard('Tomate');
		await code().click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Pon un nombre')).toBeVisible();
		await expect.element(name()).toHaveFocus();
		await userEvent.keyboard('s');
		await expect.element(page.getByText('Pon un nombre')).not.toBeInTheDocument();
	});

	it('a code that is not positive is explained under the code field', async () => {
		const o = options();
		o.create.mockRejectedValue(
			new ApiError(400, 'VALIDATION_ERROR', '', { code: 'Code must be positive' })
		);
		await render(RecordHarness, { ...o, initial: { code: 0, name: '' } });
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await expect
			.element(code())
			.toHaveAccessibleDescription('El código tiene que ser mayor que cero');
		await expect.element(code()).toHaveFocus();
	});

	it('restoring the focus prefers a field the API refused', async () => {
		const o = options();
		o.create.mockRejectedValue(new ApiError(400, 'VALIDATION_ERROR', '', { name: 'x' }));
		await render(RecordHarness, o);
		const restore = page.getByRole('button', { name: 'Restaurar el foco' });
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await expect.element(page.getByText('Revisa este dato')).toBeVisible();
		await code().click();
		await restore.click();
		await expect.element(name()).toHaveFocus();
		// Once the field changes, back to the last field with the cursor.
		await userEvent.keyboard('s');
		await code().click();
		await restore.click();
		await expect.element(code()).toHaveFocus();
	});

	it('delete asks with the pending numbers', async () => {
		const o = options();
		const body = 'Está en 3 albaranes sin facturar. Si lo borras no se podrán volver a guardar.';
		o.pendingForDelete.mockResolvedValue(body);
		await render(RecordHarness, { ...o, ...editing });
		await expect.element(bar()).toHaveTextContent('F8 Borrar');
		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toBeVisible();
		await expect.element(dialog()).toHaveTextContent('¿Borrar el artículo 101?');
		await expect.element(dialog()).toHaveTextContent(body);
		// The record's name first, then what depends on it.
		const lines = [...dialog().element().querySelectorAll('p')].map((p) => p.textContent?.trim());
		expect(lines).toEqual(['Tomate', body]);
		expect(o.pendingForDelete).toHaveBeenCalledExactlyOnceWith(5);
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(o.remove).not.toHaveBeenCalled();

		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{F8}');
		await vi.waitFor(() => expect(o.goToList).toHaveBeenCalledExactlyOnceWith());
		expect(o.remove).toHaveBeenCalledExactlyOnceWith(5);
		expect(o.toast).toHaveBeenCalledExactlyOnceWith('Artículo 101 borrado');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('confirmation before saving only when asked', async () => {
		const o = options();
		const message =
			'Cambiar el IVA o el recargo recalculará 5 albaranes sin facturar y 1 factura en borrador de este cliente.';
		const confirmBeforeSave = vi
			.fn<(values: ArticleValues, original: ArticleValues) => Promise<string | null>>()
			.mockResolvedValueOnce(null)
			.mockResolvedValue(message);
		await render(RecordHarness, { ...o, ...editing, confirmBeforeSave });

		// Nothing to confirm: straight to the API.
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(o.update).toHaveBeenCalledOnce());
		expect(confirmBeforeSave).toHaveBeenLastCalledWith(editing.initial, editing.initial);

		// Something to confirm: the question first, and back to the form leaves it unsaved.
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toBeVisible();
		await expect.element(dialog()).toHaveTextContent(message);
		expect(o.update).toHaveBeenCalledOnce();
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(o.update).toHaveBeenCalledOnce();

		// Saving anyway saves once, even with "+" pressed again while it saves.
		const saving = deferred<Saved>();
		o.update.mockReturnValue(saving.promise);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');
		saving.resolve({ id: 5, code: 101 });
		await vi.waitFor(() => expect(o.goToList).toHaveBeenCalledTimes(2));
		expect(o.update).toHaveBeenCalledTimes(2);
		expect(confirmBeforeSave).toHaveBeenCalledTimes(3);
	});

	it('Esc in the delete question does not delete', async () => {
		const o = options();
		await render(RecordHarness, { ...o, ...editing });
		await expect.element(code()).toHaveFocus();
		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{Escape}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(code()).toHaveFocus();
		expect(o.remove).not.toHaveBeenCalled();
		expect(o.goToList).not.toHaveBeenCalled();
	});

	it('Esc in the confirmation goes back to the form without saving', async () => {
		const o = options();
		const confirmBeforeSave = vi.fn().mockResolvedValue('¿Seguro?');
		await render(RecordHarness, { ...o, ...editing, confirmBeforeSave });
		await name().click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('¿Seguro?');
		await userEvent.keyboard('{Escape}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(name()).toHaveFocus();
		expect(o.update).not.toHaveBeenCalled();
		expect(o.goToList).not.toHaveBeenCalled();
	});

	it('back from the confirmation, the cursor is in the form even if it was elsewhere', async () => {
		const o = options();
		const confirmBeforeSave = vi.fn().mockResolvedValue('¿Seguro?');
		await render(RecordHarness, { ...o, ...editing, confirmBeforeSave });
		await name().click();
		// Like "+" in the unsaved-changes question: the focus was on something that goes away.
		(document.activeElement as HTMLElement).blur();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('¿Seguro?');
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(name()).toHaveFocus();
		expect(o.update).not.toHaveBeenCalled();
	});

	it('leaving a chain of new records selects the last one saved', async () => {
		const o = options();
		o.create
			.mockResolvedValueOnce({ id: 5, code: 101 })
			.mockResolvedValueOnce({ id: 6, code: 102 });
		o.blank
			.mockResolvedValueOnce({ code: 102, name: '' })
			.mockResolvedValueOnce({ code: 103, name: '' });
		await render(RecordHarness, o);
		await name().click();
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await expect.element(code()).toHaveValue('102');
		await name().click();
		await userEvent.keyboard('Pimiento{NumpadAdd}');
		await expect.element(code()).toHaveValue('103');
		await userEvent.keyboard('{Escape}');
		expect(o.goToList).toHaveBeenCalledExactlyOnceWith(6);
	});

	it('leaving a new record with nothing saved selects nothing new', async () => {
		const o = options();
		await render(RecordHarness, o);
		await expect.element(code()).toHaveFocus();
		await userEvent.keyboard('{Escape}');
		expect(o.goToList).toHaveBeenCalledOnce();
		expect(o.goToList.mock.calls[0][0]).toBeUndefined();
	});

	it('Enter on the last field goes back to the first', async () => {
		await render(RecordHarness, { ...options(), ...editing });
		await name().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(code()).toHaveFocus();
	});

	describe('buttons', () => {
		const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

		it('the buttons do what their keys do', async () => {
			const o = options();
			const first = await render(RecordHarness, { ...o, ...editing });
			await button('Guardar').click();
			await vi.waitFor(() => expect(o.goToList).toHaveBeenCalledExactlyOnceWith(5));
			expect(o.update).toHaveBeenCalledOnce();
			await first.unmount();

			const second = await render(RecordHarness, { ...o, ...editing });
			await button('Borrar').click();
			await expect.element(dialog()).toHaveTextContent('¿Borrar el artículo 101?');
			await second.unmount();

			o.goToList.mockClear();
			await render(RecordHarness, { ...o, ...editing });
			await button('Volver').click();
			expect(o.goToList).toHaveBeenCalledOnce();
		});

		it('Borrar is greyed out in a new record', async () => {
			await render(RecordHarness, options());
			await expect.element(button('Borrar')).toBeDisabled();
			await expect.element(button('Guardar')).toBeEnabled();
			await expect.element(button('Volver')).toBeEnabled();
		});

		it('the buttons sit on the title line', async () => {
			await render(RecordHarness, options());
			const line = page.getByRole('heading', { level: 1 }).element().parentElement!;
			for (const label of ['Guardar', 'Borrar', 'Volver']) {
				expect(line).toContainElement(button(label).element() as HTMLElement);
			}
		});

		it('a button keeps the cursor in the field', async () => {
			await render(RecordHarness, { ...options(), ...editing });
			await name().click();
			await button('Borrar').click();
			await expect.element(dialog()).toBeVisible();
			await userEvent.keyboard('{Enter}');
			await expect.element(dialog()).not.toBeInTheDocument();
			await expect.element(name()).toHaveFocus();
			await userEvent.keyboard('{Enter}');
			await expect.element(code()).toHaveFocus();
		});

		it('a double click on Guardar saves once', async () => {
			// The save is done before the second press: it must not save again.
			const o = options();
			const edit = await render(RecordHarness, { ...o, ...editing });
			await button('Guardar').dblClick();
			await vi.waitFor(() => expect(o.goToList).toHaveBeenCalled());
			expect(o.update).toHaveBeenCalledOnce();
			expect(o.goToList).toHaveBeenCalledOnce();
			await edit.unmount();

			// A new record: nor try to save the blank one chained after it.
			await render(RecordHarness, o);
			await name().click();
			await userEvent.keyboard('Tomate');
			await button('Guardar').dblClick();
			await expect.element(code()).toHaveValue('102');
			expect(page.getByText('Este dato es obligatorio').query()).toBeNull();
			expect(o.create).toHaveBeenCalledOnce();
		});

		it('Esc is called Volver', async () => {
			await render(RecordHarness, options());
			await expect.element(bar()).toHaveTextContent('Esc Volver');
		});
	});
});
