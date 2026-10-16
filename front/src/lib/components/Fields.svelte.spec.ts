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
});
