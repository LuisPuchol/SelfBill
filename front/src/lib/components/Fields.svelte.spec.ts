import { describe, expect, it } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import FieldsHarness from '#lib/test/FieldsHarness.svelte';
import { formatDate, todayIso } from '#lib/format/date.js';

const lote = () => page.getByLabelText('Lote');
const fecha = () => page.getByLabelText('Fecha');
const formato = () => page.getByRole('radiogroup', { name: 'Formato' });
/** Clicking the group's centre would hit an option: focus it like Enter from the previous field does. */
const focusFormato = () => formato().element().focus();
const fin = () => page.getByLabelText('Fin');
const entero = () => page.getByLabelText('Entero');
const porcentaje = () => page.getByLabelText('Porcentaje');
/** Like the page's "+": the cursor stays where it is. */
const validateAll = () =>
	page
		.getByRole('button', { name: 'Validar todo' })
		.element()
		.dispatchEvent(new MouseEvent('click', { bubbles: true }));

describe('Lot, date and choice fields', () => {
	it('the lot keeps only digits and its leading zero', async () => {
		render(FieldsHarness);
		await lote().click();
		await userEvent.keyboard('01a21-0426');
		await expect.element(lote()).toHaveValue('01210426');
		await expect.element(page.getByTestId('lot')).toHaveTextContent('01210426');
	});

	it('a short lot keeps the cursor', async () => {
		render(FieldsHarness);
		await lote().click();
		await userEvent.keyboard('0121{Enter}');
		await expect.element(page.getByText('Tienen que ser 8 cifras')).toBeVisible();
		await expect.element(lote()).toHaveFocus();
	});

	it('the date starts as today', async () => {
		render(FieldsHarness);
		await expect.element(fecha()).toHaveValue(formatDate(todayIso()));
	});

	it('the date accepts six digits', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('121026{Enter}');
		await expect.element(fecha()).toHaveValue('12/10/2026');
		await expect.element(page.getByTestId('date')).toHaveTextContent('2026-10-12');
		await expect.element(formato()).toHaveFocus();
	});

	it('a read-only date cannot be typed into', async () => {
		render(FieldsHarness, { dateReadOnly: true });
		await fecha().click();
		await userEvent.keyboard('121026');
		await expect.element(fecha()).toHaveValue(formatDate(todayIso()));
		await expect.element(fecha()).toHaveAttribute('readonly');
		await expect.element(page.getByTestId('date')).toHaveTextContent(todayIso());
	});

	it('the date is normalised when leaving with the mouse', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('121026');
		await lote().click();
		await expect.element(fecha()).toHaveValue('12/10/2026');
		await expect.element(page.getByTestId('date')).toHaveTextContent('2026-10-12');
	});

	it('an impossible date keeps the cursor', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('310226{Enter}');
		await expect.element(page.getByText('Fecha no válida')).toBeVisible();
		await expect.element(fecha()).toHaveFocus();
	});

	it('the date value follows the text while typing', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('121026');
		await expect.element(page.getByTestId('date')).toHaveTextContent('2026-10-12');
		await expect.element(fecha()).toHaveValue('121026');
		await expect.element(fecha()).toHaveFocus();
	});

	it('an invalid date shows its error on leaving and empties the value', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('3210');
		await lote().click();
		await expect.element(page.getByText('Fecha no válida')).toBeVisible();
		await expect.element(fecha()).toHaveValue('3210');
		await expect.element(page.getByTestId('date')).toHaveTextContent(/^$/);
	});

	it('a date set from outside replaces invalid text', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.keyboard('3210');
		// The page resets the date with the cursor still in the field, as after saving with "+".
		page
			.getByRole('button', { name: 'Poner 01/01/2026' })
			.element()
			.dispatchEvent(new MouseEvent('click', { bubbles: true }));
		await expect.element(fecha()).toHaveValue('01/01/2026');
		await expect.element(page.getByTestId('date')).toHaveTextContent('2026-01-01');
		await expect.element(fecha()).toHaveFocus();
	});

	it('leaving a field reports what is mistyped', async () => {
		render(FieldsHarness);
		await lote().click();
		await userEvent.keyboard('0121{Tab}');
		await expect.element(page.getByText('Tienen que ser 8 cifras')).toBeVisible();
	});

	it('leaving an empty required date says nothing', async () => {
		render(FieldsHarness);
		await fecha().click();
		await userEvent.clear(fecha());
		await userEvent.keyboard('{Tab}');
		await expect.element(page.getByText('Este dato es obligatorio')).not.toBeInTheDocument();
	});

	it('an integer field ignores the decimal key', async () => {
		render(FieldsHarness);
		await entero().click();
		await userEvent.keyboard('10');
		const event = new KeyboardEvent('keydown', {
			key: '.',
			code: 'NumpadDecimal',
			bubbles: true,
			cancelable: true
		});
		entero().element().dispatchEvent(event);
		expect(event.defaultPrevented).toBe(true);
		await expect.element(entero()).toHaveValue('10');
	});

	it('an integer field refuses decimals', async () => {
		render(FieldsHarness);
		await entero().click();
		await userEvent.keyboard('10,5{Tab}');
		await expect.element(page.getByText('Escribe un número entero')).toBeVisible();
		await expect.element(entero()).toHaveValue('10,5');
	});

	it('NumberField validate shows its message on leaving and in validateAll', async () => {
		render(FieldsHarness);
		const message = () => page.getByText('Tiene que estar entre 0 y 100');
		await porcentaje().click();
		await userEvent.keyboard('150{Tab}');
		await expect.element(message()).toBeVisible();
		await porcentaje().click();
		await userEvent.keyboard('50{Tab}');
		await expect.element(porcentaje()).toHaveValue('50,00');
		await expect.element(message()).not.toBeInTheDocument();

		// The other fields valid, so validateAll leaves the cursor in this one: no leaving.
		await lote().click();
		await userEvent.keyboard('01210426');
		await page.getByRole('radio', { name: /Simple/ }).click();
		await porcentaje().click();
		await userEvent.keyboard('150');
		validateAll();
		await expect.element(message()).toBeVisible();
		await expect.element(porcentaje()).toHaveValue('150,00');
		await expect.element(porcentaje()).toHaveFocus();
		await userEvent.clear(porcentaje());
		await userEvent.keyboard('50');
		validateAll();
		await expect.element(porcentaje()).toHaveValue('50,00');
		await expect.element(message()).not.toBeInTheDocument();
		await expect.element(porcentaje()).toHaveFocus();
	});

	it('a digit picks the option', async () => {
		render(FieldsHarness);
		focusFormato();
		await userEvent.keyboard('2');
		await expect
			.element(page.getByRole('radio', { name: /Valorado/ }))
			.toHaveAttribute('aria-checked', 'true');
		await expect.element(page.getByTestId('format')).toHaveTextContent('VALUED');
		await userEvent.keyboard('{Enter}');
		await expect.element(fin()).toHaveFocus();
	});

	it('clicking an option picks it and the group keeps the cursor', async () => {
		render(FieldsHarness);
		await page.getByRole('radio', { name: /Simple/ }).click();
		await expect.element(page.getByTestId('format')).toHaveTextContent('PLAIN');
		await expect.element(formato()).toHaveFocus();
	});

	it('digits outside the options do nothing', async () => {
		render(FieldsHarness);
		focusFormato();
		await userEvent.keyboard('7');
		await expect.element(page.getByTestId('format')).toHaveTextContent('null');
	});

	it('a required choice keeps the cursor', async () => {
		render(FieldsHarness);
		focusFormato();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Elige una opción')).toBeVisible();
		await expect.element(formato()).toHaveFocus();
	});

	it('a compact field keeps its accessible name and its error', async () => {
		render(FieldsHarness, { compact: true });
		await fin().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(page.getByText('Fin', { exact: true })).toHaveClass('sr-only');
	});
});
