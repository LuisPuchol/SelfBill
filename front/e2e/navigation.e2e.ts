import { expect, test } from '@playwright/test';

test('opens a section with its number and comes back with Esc', async ({ page }) => {
	await page.goto('/');
	await expect(page.getByRole('link', { name: /^[1-6]/ })).toHaveCount(6);
	// The key bar lists an action only once its keyboard layer is registered.
	await expect(page.locator('footer')).toContainText('1–6');
	await page.keyboard.press('1');
	await expect(page).toHaveURL('/delivery-notes');
	await expect(page.getByRole('heading', { name: 'Albaranes' })).toBeVisible();
	await expect(page.locator('footer')).toContainText('Salir');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/');
});

test('numpad digits work too', async ({ page }) => {
	await page.goto('/');
	await expect(page.locator('footer')).toContainText('1–6');
	// Playwright's Numpad4 yields ArrowLeft (NumLock off); a real numpad with NumLock on sends key "4".
	await page.evaluate(() =>
		window.dispatchEvent(new KeyboardEvent('keydown', { key: '4', code: 'Numpad4', bubbles: true }))
	);
	await expect(page).toHaveURL('/articles');
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
});

test('the top bar opens any section', async ({ page }) => {
	await page.goto('/');
	await page.locator('nav').getByRole('link', { name: 'Facturas' }).click();
	await expect(page).toHaveURL('/invoices');
});

test('a section survives a reload', async ({ page }) => {
	await page.goto('/clients');
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
	await page.reload();
	await expect(page).toHaveURL('/clients');
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
});

test('the key bar shows the keys of the screen', async ({ page }) => {
	await page.goto('/clients');
	const footer = page.locator('footer');
	await expect(footer).toContainText('Esc');
	await expect(footer).toContainText('Salir');
});

test('the api still answers next to the front', async ({ request }) => {
	const response = await request.get('/api/articles/next-code');
	expect(response.ok()).toBe(true);
	expect(await response.json()).toHaveProperty('code');
});
