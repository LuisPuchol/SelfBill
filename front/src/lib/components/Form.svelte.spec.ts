import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import FormHarness from '#lib/test/FormHarness.svelte';

const nombre = () => page.getByLabelText('Nombre');
const cantidad = () => page.getByLabelText('Cantidad');
const ultimo = () => page.getByLabelText('Último');

/** A keydown with the given key and code; returns whether the field took it over. */
function keydown(key: string, code: string): boolean {
	const event = new KeyboardEvent('keydown', { key, code, bubbles: true, cancelable: true });
	cantidad().element().dispatchEvent(event);
	return event.defaultPrevented;
}

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

	it('clicking into a field selects its content', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		await userEvent.keyboard('10{Enter}');
		await expect.element(cantidad()).toHaveValue('10,00');
		await userEvent.click(cantidad());
		await userEvent.keyboard('4');
		await expect.element(cantidad()).toHaveValue('4');
	});

	it('the numpad decimal key with NumLock off is left alone', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await cantidad().click();
		await userEvent.keyboard('35');
		expect(keydown('Delete', 'NumpadDecimal')).toBe(false);
		await expect.element(cantidad()).toHaveValue('35');
		expect(keydown('.', 'NumpadDecimal')).toBe(true);
	});

	it('an empty error prop does not hide the own error', async () => {
		render(FormHarness, { onLast: vi.fn(), nameError: '' });
		await nombre().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
	});

	it('leaving a field does not report what is missing', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await nombre().click();
		await userEvent.keyboard('{Tab}');
		await expect.element(page.getByText('Este dato es obligatorio')).not.toBeInTheDocument();
		await nombre().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
	});

	it('a text field runs its own check on leaving', async () => {
		render(FormHarness, { onLast: vi.fn() });
		await ultimo().click();
		await userEvent.keyboard('pepe{Tab}');
		await expect.element(page.getByText('Revisa el email')).toBeVisible();
	});

	it('the error line keeps its place', async () => {
		const { container } = render(FormHarness, { onLast: vi.fn() });
		const line = container.querySelector('#name-error');
		expect(line).not.toBeNull();
		expect(line?.textContent).toBe('');
	});

	it('fields turn autocomplete off', async () => {
		const { container } = render(FormHarness, { onLast: vi.fn() });
		const inputs = [...container.querySelectorAll('input')];
		expect(inputs.length).toBeGreaterThan(0);
		for (const input of inputs) expect(input.getAttribute('autocomplete')).toBe('off');
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
