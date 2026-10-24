import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

// Codes 9501-9549 are this file's: the other e2e files use their own. Its notes are dated in
// 2026-04, a month no other file uses: the period 01/04/2026-30/04/2026 sees only them.

interface Saved {
	id: number;
	code: number;
}

/** The DB lasts the whole run: a code already there (409) is fetched instead. */
async function ensure(
	request: APIRequestContext,
	path: '/api/clients' | '/api/articles',
	data: { code: number; name: string } & Record<string, string | number>
): Promise<Saved> {
	const created = await request.post(path, { data });
	if (created.status() === 409) {
		const found = await request.get(`${path}/by-code/${data.code}`);
		expect(found.ok()).toBe(true);
		return (await found.json()) as Saved;
	}
	expect(created.ok()).toBe(true);
	return (await created.json()) as Saved;
}

async function ensureClient(request: APIRequestContext, code: number, name: string) {
	// 409: the tax config already exists
	await request.post('/api/tax-config', {
		data: { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }
	});
	return ensure(request, '/api/clients', {
		code,
		name,
		businessName: `${name} SL`,
		vatType: 'WITH_VAT',
		surchargeType: 'WITH_SURCHARGE',
		defaultDeliveryNoteFormat: 'VALUED'
	});
}

async function createNote(
	request: APIRequestContext,
	clientId: number,
	articleId: number,
	lots: { traceabilityCode1: string; traceabilityCode2?: string },
	quantity: number
) {
	const response = await request.post('/api/delivery-notes', {
		data: {
			date: '2026-04-15T09:00:00',
			clientId,
			lines: [{ articleId, quantity, price: 1, ...lots }]
		}
	});
	expect(response.ok()).toBe(true);
}

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

test('the lots of an article, the notes of a lot, and F9 prints them', async ({
	page,
	request
}) => {
	const bar = await ensureClient(request, 9501, 'Bar Uno trazabilidad e2e');
	const shop = await ensureClient(request, 9502, 'Bar Dos trazabilidad e2e');
	const article = await ensure(request, '/api/articles', {
		code: 9503,
		name: 'Lechuga trazabilidad e2e'
	});
	await createNote(request, bar.id, article.id, { traceabilityCode1: '02010426' }, 5);
	// The lot in the second slot: it counts for 02010426 and for 03020426
	await createNote(
		request,
		shop.id,
		article.id,
		{ traceabilityCode1: '03020426', traceabilityCode2: '02010426' },
		3
	);
	// In every frame, the printed document's too: print() counts in the page instead of opening
	// the dialog, and fires afterprint as a closed dialog does.
	await page.addInitScript(() => {
		window.print = () => {
			const top = window.top as Window & { printed?: number };
			top.printed = (top.printed ?? 0) + 1;
			window.dispatchEvent(new Event('afterprint'));
		};
	});
	const printed = () => page.evaluate(() => (window as Window & { printed?: number }).printed ?? 0);

	await page.goto('/');
	await ready(page, '1–6');
	await page.keyboard.press('5');
	await expect(page).toHaveURL('/traceability');
	await expect(page.getByLabel('Desde')).toBeFocused();
	await page.keyboard.type('010426');
	await page.keyboard.press('Enter');
	await page.keyboard.type('300426');
	await page.keyboard.press('Enter');

	const tables = page.getByRole('table');
	await expect(tables.nth(0).getByRole('row')).toHaveCount(2);
	await expect(tables.nth(0).getByRole('row').nth(1)).toContainText('Lechuga trazabilidad e2e');
	const lots = tables.nth(1).getByRole('row');
	await expect(lots).toHaveCount(3);
	await expect(lots.nth(1)).toContainText('02010426');
	await expect(lots.nth(1)).toContainText('8,00');
	await expect(lots.nth(2)).toContainText('03020426');
	await expect(lots.nth(2)).toContainText('3,00');

	await page.keyboard.press('Enter');
	await page.keyboard.press('ArrowDown');
	const notes = tables.nth(2).getByRole('row');
	await expect(notes).toHaveCount(2);
	await expect(notes.nth(1)).toContainText('Bar Dos trazabilidad e2e');
	await expect(notes.nth(1)).toContainText('3,00');
	await expect(page.getByText('1 albarán · 3,00')).toBeVisible();

	await page.keyboard.press('F9');
	await expect.poll(printed).toBe(1);
	await expect(page.getByRole('dialog')).toHaveCount(0);
});
