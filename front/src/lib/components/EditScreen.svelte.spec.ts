import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import EditHarness from '#lib/test/EditHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import type { FormState } from '#lib/records/form-state.svelte.js';

type Form = FormState<{ name: string; amount: number | null }>;

function spies() {
	return {
		persist: vi.fn<(form: Form) => Promise<boolean>>().mockResolvedValue(true),
		exit: vi.fn<() => void>()
	};
}

function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

const name = () => page.getByLabelText('Nombre');
const amount = () => page.getByLabelText('Importe');
const dirty = () => page.getByTestId('dirty');
const bar = () => page.getByRole('contentinfo');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

describe('EditScreen', () => {
	it('the cursor starts in the first field', async () => {
		await render(EditHarness, spies());
		await expect
			.element(page.getByRole('heading', { level: 1, name: 'Ficha de prueba' }))
			.toBeVisible();
		await expect.element(name()).toHaveFocus();
		await expect.element(bar()).toHaveTextContent('+ Guardar');
		await expect.element(bar()).toHaveTextContent('Esc Volver');
	});

	it('"+" checks every field first', async () => {
		const s = spies();
		await render(EditHarness, s);
		await amount().click();
		await userEvent.keyboard('150{NumpadAdd}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(page.getByText('Tiene que estar entre 0 y 100')).toBeVisible();
		await expect.element(name()).toHaveFocus();
		expect(s.persist).not.toHaveBeenCalled();
	});

	it('Guardar and Volver do what their keys do', async () => {
		const s = spies();
		await render(EditHarness, s);
		await userEvent.keyboard('Tomate');
		await button('Guardar').click();
		await vi.waitFor(() => expect(s.persist).toHaveBeenCalledOnce());
		await button('Volver').click();
		expect(s.exit).toHaveBeenCalledOnce();
		// The buttons are the mouse twin of the keys: the cursor stays in the field.
		await expect.element(name()).toHaveFocus();
	});

	it('a second "+" while saving does nothing', async () => {
		const s = spies();
		const saving = deferred<boolean>();
		s.persist.mockReturnValueOnce(saving.promise);
		await render(EditHarness, s);
		await userEvent.keyboard('Tomate{NumpadAdd}');
		await vi.waitFor(() => expect(s.persist).toHaveBeenCalledOnce());
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');
		expect(s.persist).toHaveBeenCalledOnce();
		saving.resolve(true);
		// Once saved, "+" saves again.
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(s.persist).toHaveBeenCalledTimes(2));
	});

	it('API field errors show under the field and clear when it changes', async () => {
		const s = spies();
		const shown: boolean[] = [];
		s.persist.mockImplementation(async (form) => {
			// Anything but a VALIDATION_ERROR with field errors is left to the page.
			shown.push(form.showFieldErrors(new ApiError(0, 'NETWORK', '', {})));
			shown.push(
				form.showFieldErrors(
					new ApiError(400, 'VALIDATION_ERROR', '', { name: 'Name cannot be blank' }),
					{ name: 'Nombre no válido' }
				)
			);
			return false;
		});
		await render(EditHarness, s);
		await userEvent.keyboard('Tomate');
		await amount().click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(name()).toHaveAccessibleDescription('Nombre no válido');
		await expect.element(name()).toHaveFocus();
		expect(shown).toEqual([false, true]);
		expect(page.getByText('Name cannot be blank').query()).toBeNull();
		await userEvent.keyboard('s');
		await expect.element(page.getByText('Nombre no válido')).not.toBeInTheDocument();
	});

	it('isDirty follows the values and markSaved clears it', async () => {
		const s = spies();
		s.persist.mockImplementation(async (form) => {
			form.markSaved();
			return true;
		});
		await render(EditHarness, s);
		await expect.element(dirty()).toHaveTextContent('false');
		await userEvent.keyboard('T');
		await expect.element(dirty()).toHaveTextContent('true');
		// Back to what was saved: nothing to save.
		await userEvent.keyboard('{Backspace}');
		await expect.element(dirty()).toHaveTextContent('false');
		await userEvent.keyboard('Tomate');
		await expect.element(dirty()).toHaveTextContent('true');
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dirty()).toHaveTextContent('false');
		expect(s.persist).toHaveBeenCalledOnce();
		await userEvent.keyboard('s');
		await expect.element(dirty()).toHaveTextContent('true');
	});
});
