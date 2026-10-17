import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import FormHarness from '#lib/test/FormHarness.svelte';

const nombre = () => page.getByLabelText('Nombre');
const cantidad = () => page.getByLabelText('Cantidad');
const ultimo = () => page.getByLabelText('Último');

/** A real keydown from the numpad's decimal key; Windows may report it as "." or ",". */
function numpadDecimal(key: string): void {
	cantidad()
		.element()
		.dispatchEvent(
			new KeyboardEvent('keydown', { key, code: 'NumpadDecimal', bubbles: true, cancelable: true })
		);
}

describe('Form', () => {
	it('Enter moves to the next field', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('Pepe{Enter}');
		await expect.element(cantidad()).toHaveFocus();
	});

	it('Shift+Enter moves back', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('Pepe{Enter}');
		await expect.element(cantidad()).toHaveFocus();
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
		await expect.element(nombre()).toHaveFocus();
	});

	it('Enter on the last field calls onLast', async () => {
		const onLast = vi.fn();
		render(FormHarness, { onLast });
		await ultimo().click();
		await userEvent.keyboard('{Enter}');
		await vi.waitFor(() => expect(onLast).toHaveBeenCalledOnce());
		await expect.element(ultimo()).toHaveFocus();
	});

	it('a required empty field keeps the cursor', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(nombre()).toHaveAccessibleDescription('Este dato es obligatorio');
		await expect.element(nombre()).toHaveFocus();
	});

	it("typing clears the field's own error", async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await userEvent.keyboard('P');
		await expect.element(page.getByText('Este dato es obligatorio')).not.toBeInTheDocument();
	});

	it('shows an error given from outside under the field', async () => {
		render(FormHarness, { onLast: vi.fn(), quantityError: 'Revisa este dato' });
		await expect.element(page.getByText('Revisa este dato')).toBeVisible();
		await expect.element(cantidad()).toHaveAccessibleDescription('Revisa este dato');
	});

	it('an invalid number keeps the cursor', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		await userEvent.keyboard('abc{Enter}');
		await expect.element(page.getByText('Escribe un número')).toBeVisible();
		await expect.element(cantidad()).toHaveFocus();
	});

	it('numbers are normalised on leaving', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		await userEvent.keyboard('10{Enter}');
		await expect.element(cantidad()).toHaveValue('10,00');
		await expect.element(page.getByTestId('quantity')).toHaveTextContent(/^10$/);
		await expect.element(ultimo()).toHaveFocus();

		// Leaving with the mouse normalises too.
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
		await userEvent.keyboard('7.5');
		await nombre().click();
		await expect.element(cantidad()).toHaveValue('7,50');
		await expect.element(page.getByTestId('quantity')).toHaveTextContent(/^7\.5$/);
	});

	it('the numpad decimal key always writes a comma', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		for (const key of ['.', ',']) {
			await userEvent.clear(cantidad());
			await userEvent.keyboard('3');
			numpadDecimal(key);
			await userEvent.keyboard('5');
			await expect.element(cantidad()).toHaveValue('3,5');
		}
	});

	it('the numpad decimal key writes at the cursor, over any selection', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		await userEvent.keyboard('35{ArrowLeft}');
		numpadDecimal('.');
		await expect.element(cantidad()).toHaveValue('3,5');

		await ultimo().click();
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
		numpadDecimal('.');
		await expect.element(cantidad()).toHaveValue(',');
	});

	it('entering a field selects its content', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('Pepe{Enter}');
		await userEvent.keyboard('10{Enter}');
		await expect.element(ultimo()).toHaveFocus();
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
		await expect.element(cantidad()).toHaveFocus();
		await userEvent.keyboard('4');
		await expect.element(cantidad()).toHaveValue('4');
	});

	it('the page validates every field before saving, through bind:nav', async () => {
		const onSave = vi.fn();
		render(FormHarness, { onLast: vi.fn(), onSave });
		await cantidad().click();
		await userEvent.keyboard('abc');
		await ultimo().click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(page.getByText('Escribe un número')).toBeVisible();
		await expect.element(nombre()).toHaveFocus();
		expect(onSave).not.toHaveBeenCalled();

		await userEvent.keyboard('Pepe');
		await cantidad().click();
		await userEvent.keyboard('5{NumpadAdd}');
		await vi.waitFor(() => expect(onSave).toHaveBeenCalledOnce());
	});

	it('the key bar shows Siguiente only while a field has the cursor', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await expect.element(page.getByText('Siguiente')).toBeVisible();
		(document.activeElement as HTMLElement).blur();
		await expect.element(page.getByText('Siguiente')).not.toBeInTheDocument();
		await cantidad().click();
		await expect.element(page.getByText('Siguiente')).toBeVisible();
	});
});
