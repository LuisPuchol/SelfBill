import { describe, expect, it } from 'vitest';
import { page } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import SettingsMenuHarness from '#lib/test/SettingsMenuHarness.svelte';

describe('SettingsMenu', () => {
	it('shows each tile with its state and the keys', async () => {
		await render(SettingsMenuHarness, {
			status: {
				tax: { id: 1, vatPercentage: 4, surchargePercentage: 0.5 },
				anyTax: true,
				profile: { id: 1, fiscalName: 'Frutas Luis SL' }
			}
		});
		await expect.element(page.getByRole('link', { name: /IVA y recargo/ })).toBeVisible();
		await expect.element(page.getByText('IVA 4 % · Recargo 0,5 %')).toBeVisible();
		await expect.element(page.getByRole('link', { name: /Datos del titular/ })).toBeVisible();
		await expect.element(page.getByText('Frutas Luis SL')).toBeVisible();
		const bar = page.getByRole('contentinfo');
		await expect.element(bar).toHaveTextContent('1–2');
		await expect.element(bar).toHaveTextContent('Abrir');
		await expect.element(bar).toHaveTextContent('Esc');
		await expect.element(bar).toHaveTextContent('Volver');
	});

	it('says "Sin configurar" when nothing is set', async () => {
		await render(SettingsMenuHarness, { status: { tax: null, anyTax: false, profile: null } });
		expect(page.getByText('Sin configurar').elements()).toHaveLength(2);
	});
});
