import { expect, test } from '@playwright/test';

test('Ajustes opens with 6 and says what is configured', async ({ page, request }) => {
	// The database is shared by all e2e tests: read what is there instead of assuming it is empty.
	const taxResponse = await request.get('/api/tax-config');
	const profileResponse = await request.get('/api/user-profile');
	const tax = taxResponse.ok()
		? ((await taxResponse.json()) as { vatPercentage: number; surchargePercentage: number })
		: null;
	const profile = profileResponse.ok()
		? ((await profileResponse.json()) as { fiscalName: string })
		: null;

	await page.goto('/');
	await expect(page.locator('footer')).toContainText('1–6');
	await page.keyboard.press('6');
	await expect(page).toHaveURL('/settings');
	const taxTile = page.getByRole('link', { name: /IVA y recargo/ });
	await expect(taxTile).toBeVisible();
	await expect(page.getByRole('link', { name: /Datos del titular/ })).toBeVisible();
	const percent = (n: number) => String(n).replace('.', ',') + ' %';
	if (tax) {
		await expect(taxTile).toContainText(
			`IVA ${percent(tax.vatPercentage)} · Recargo ${percent(tax.surchargePercentage)}`
		);
	} else {
		await expect(taxTile).toContainText(/Sin configurar|No hay IVA vigente hoy/);
	}
	await expect(page.getByRole('link', { name: /Datos del titular/ })).toContainText(
		profile ? profile.fiscalName : 'Sin configurar'
	);
	await expect(page.locator('footer')).toContainText('1–2');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/');
});
