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

test('adds a period and corrects it', async ({ page }) => {
	await page.goto('/settings/tax');
	await expect(page.getByRole('heading', { name: 'IVA y recargo' })).toBeVisible();
	await expect(page.locator('footer')).toContainText('Nuevo');
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Tipo nuevo' })).toBeVisible();
	// A date far away: the database is shared with the other tests.
	await page.getByLabel('Desde').fill('01012099');
	await page.getByLabel('IVA %').fill('21');
	await page.getByLabel('Recargo %').fill('5,2');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Tipo desde el 01/01/2099 guardado')).toBeVisible();

	const row = page.getByRole('row', { name: /01\/01\/2099/ });
	await expect(row).toHaveText(/01\/01\/2099\s*21\s%\s*5,2\s%\s*A partir del 01\/01\/2099/);
	await expect(row).toHaveAttribute('aria-selected', 'true');

	await page.keyboard.press('Enter');
	await expect(page.getByRole('heading', { name: 'Tipo desde el 01/01/2099' })).toBeVisible();
	await page.getByLabel('IVA %').fill('22');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByRole('row', { name: /01\/01\/2099/ })).toHaveText(/22\s%/);
});
