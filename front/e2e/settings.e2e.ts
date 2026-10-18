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

test("saves the owner's data and the home notice goes away", async ({ page, request }) => {
	// A period in force, so only the profile is missing; another test may have created it.
	const tax = await request.post('/api/tax-config', {
		data: { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }
	});
	expect([201, 409]).toContain(tax.status());
	const hadProfile = (await request.get('/api/user-profile')).ok();
	const notice = page.getByText('Pulsa 6 para ir a Ajustes.');

	// The home is client-rendered: wait for its keys before judging the notice.
	await page.goto('/');
	await expect(page.locator('footer')).toContainText('1–6');
	if (!hadProfile) await expect(notice).toContainText('datos del titular');

	await page.goto('/settings');
	// The keys are live once the footer shows them.
	await expect(page.locator('footer')).toContainText('1–2');
	await page.keyboard.press('2');
	await expect(page).toHaveURL('/settings/profile');
	await expect(page.getByRole('heading', { name: 'Datos del titular' })).toBeVisible();
	await page.getByLabel('Nombre completo').fill('María López');
	await page.getByLabel('Nombre fiscal').fill('Frutas López S.L.');
	await page.getByLabel('NIF').fill('B87654321');
	await page.getByLabel('Dirección').fill('Mercavalencia, nave 3');
	await page.getByLabel('Email').fill('facturas@ejemplo.es');
	await page.keyboard.press('NumpadAdd');

	await expect(page).toHaveURL('/settings');
	await expect(page.getByText('Datos del titular guardados')).toBeVisible();
	await expect(page.getByRole('link', { name: /Datos del titular/ })).toContainText(
		'Frutas López S.L.'
	);
	await page.goto('/');
	await expect(page.locator('footer')).toContainText('1–6');
	await expect(notice).toHaveCount(0);
});
