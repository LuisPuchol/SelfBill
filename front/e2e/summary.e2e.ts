import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

// Codes 9601-9649 are this file's: the other e2e files use their own. The invoices are issued
// today, so the default period sees them; the other clients' rows are found by code.

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

/** A client whose notes can be invoiced: issuing needs the owner, a VAT period and fiscal data. */
async function invoiceableClient(request: APIRequestContext, code: number, name: string) {
	// 409: another test created them already
	await request.post('/api/user-profile', {
		data: {
			fullName: 'María López',
			fiscalName: 'Frutas López S.L.',
			nif: 'B87654321',
			address: 'Mercavalencia, nave 3',
			email: 'facturas@ejemplo.es'
		}
	});
	await request.post('/api/tax-config', {
		data: { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }
	});
	return ensure(request, '/api/clients', {
		code,
		name,
		businessName: `${name} SL`,
		vatType: 'WITH_VAT',
		surchargeType: 'WITH_SURCHARGE',
		defaultDeliveryNoteFormat: 'VALUED',
		nif: 'B12345674',
		address: 'Calle Mayor 1',
		postalCode: '46001',
		city: 'Valencia'
	});
}

/** Notes of one line each, invoiced together; the invoice is as of today. */
async function issueInvoice(
	request: APIRequestContext,
	clientId: number,
	articleId: number,
	quantity: number,
	notes = 1
): Promise<{ id: number; number: string; total: number }> {
	const noteIds: number[] = [];
	for (let n = 0; n < notes; n++) {
		const note = await request.post('/api/delivery-notes', {
			data: {
				date: '2026-05-10T09:00:00',
				clientId,
				lines: [{ traceabilityCode1: '01010526', articleId, quantity, price: 2.5 }]
			}
		});
		expect(note.ok()).toBe(true);
		noteIds.push(((await note.json()) as { id: number }).id);
	}
	const issued = await request.post('/api/invoices/issue', {
		data: { clientId, deliveryNoteIds: noteIds }
	});
	expect(issued.ok()).toBe(true);
	return (await issued.json()) as { id: number; number: string; total: number };
}

/** "1.234,50 €" as the app prints it. */
function formatEuros(amount: number): string {
	return new Intl.NumberFormat('es-ES', {
		style: 'currency',
		currency: 'EUR',
		useGrouping: 'always'
	}).format(amount);
}

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

test('the summary lists each client with its totals, invoices and notes, and F9 prints', async ({
	page,
	request
}) => {
	const first = await invoiceableClient(request, 9601, 'Frutas Resumen Uno e2e');
	const second = await invoiceableClient(request, 9602, 'Bar Resumen Dos e2e');
	const article = await ensure(request, '/api/articles', { code: 9603, name: 'Kiwi resumen e2e' });
	const a1 = await issueInvoice(request, first.id, article.id, 4);
	const a2 = await issueInvoice(request, first.id, article.id, 6);
	const b1 = await issueInvoice(request, second.id, article.id, 2);
	// print() counts in the page instead of opening the dialog, and fires afterprint as a closed
	// dialog does; in every frame, the printed document's too.
	await page.addInitScript(() => {
		window.print = () => {
			const top = window.top as Window & { printed?: number };
			top.printed = (top.printed ?? 0) + 1;
			window.dispatchEvent(new Event('afterprint'));
		};
	});
	const printed = () => page.evaluate(() => (window as Window & { printed?: number }).printed ?? 0);

	await page.goto('/invoices');
	await ready(page, '1–3');
	await page.keyboard.press('3');
	await expect(page).toHaveURL('/invoices/summary');
	await ready(page, 'Imprimir factura');

	const tables = page.getByRole('table');
	const clients = tables.nth(0).getByRole('row');
	const one = clients.filter({ hasText: 'Frutas Resumen Uno e2e' });
	const two = clients.filter({ hasText: 'Bar Resumen Dos e2e' });
	await expect(one).toHaveCount(1);
	await expect(one.getByRole('cell').nth(0)).toHaveText('9601');
	await expect(one.getByRole('cell').nth(2)).toHaveText('2');
	await expect(one.getByRole('cell').nth(6)).toHaveText(formatEuros(a1.total + a2.total));
	await expect(two).toHaveCount(1);
	await expect(two.getByRole('cell').nth(2)).toHaveText('1');
	await expect(two.getByRole('cell').nth(6)).toHaveText(formatEuros(b1.total));

	// Enter, Enter: the cursor leaves the dates for the clients
	await page.keyboard.press('Enter');
	await page.keyboard.press('Enter');
	await expect(page.getByLabel('Hasta')).not.toBeFocused();
	await two.click();
	await expect(two).toHaveAttribute('aria-selected', 'true');
	const invoices = tables.nth(1).getByRole('row');
	await expect(invoices).toHaveCount(2);
	await expect(invoices.nth(1)).toContainText(b1.number);

	await one.click();
	await expect(one).toHaveAttribute('aria-selected', 'true');
	await expect(invoices).toHaveCount(3);
	await expect(invoices.nth(1)).toContainText(a1.number);
	await expect(invoices.nth(2)).toContainText(a2.number);

	await page.keyboard.press('Enter');
	await page.keyboard.press('ArrowDown');
	await expect(invoices.nth(2)).toHaveAttribute('aria-selected', 'true');
	// 6 units at 2,50: one note
	const notes = tables.nth(2).getByRole('row');
	await expect(notes).toHaveCount(2);
	await expect(notes.nth(1)).toContainText('15,00');
	await expect(page.getByText('1 albarán ·')).toBeVisible();

	await page.keyboard.press('F9');
	await expect.poll(printed).toBe(1);
	await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('the summary fits a 1280x800 window: four rows per panel, no scrolling sideways or of the page', async ({
	page,
	request
}) => {
	const client = await invoiceableClient(request, 9611, 'Mercado Resumen Grande e2e');
	const article = await ensure(request, '/api/articles', { code: 9612, name: 'Caqui resumen e2e' });
	// Five invoices of four-digit amounts: more than the four rows that fit
	// (the first with two notes, to move the note mark)
	for (let i = 0; i < 5; i++) {
		await issueInvoice(request, client.id, article.id, 2000 + i, i === 0 ? 2 : 1);
	}
	await page.setViewportSize({ width: 1280, height: 800 });
	await page.goto('/invoices/summary');
	await ready(page, 'Imprimir factura');

	const tables = page.getByRole('table');
	const row = tables.nth(0).getByRole('row').filter({ hasText: 'Mercado Resumen Grande e2e' });
	await expect(row).toHaveCount(1);
	await row.click();
	const invoices = tables.nth(1).getByRole('row');
	await expect(invoices).toHaveCount(6);

	// Each table's scroll box shows all its columns, and the page does not scroll
	for (const i of [0, 1, 2]) {
		const box = tables.nth(i).locator('xpath=..');
		const fit = await box.evaluate((el) => el.scrollWidth <= el.clientWidth);
		expect(fit, `table ${i} scrolls sideways`).toBe(true);
	}
	const fits = await page.evaluate(
		() => document.documentElement.scrollHeight <= window.innerHeight
	);
	expect(fits).toBe(true);

	// The right-hand panels (a table's box and its footer; the notes' box only shrinks to its
	// content) have room for their header and four rows
	for (const i of [1, 2]) {
		const table = tables.nth(i);
		const panel = table.locator('xpath=../..');
		const header = (await table.locator('thead tr').boundingBox())!.height;
		const first = (await table.locator('tbody tr').first().boundingBox())!.height;
		const height = (await panel.boundingBox())!.height;
		expect(height, `table ${i}`).toBeGreaterThanOrEqual(header + 4 * first);
	}

	// Enter, Enter: the notes; the arrows move their mark, Esc goes back to the invoices, whose
	// next invoice brings the mark back to its first note
	const notes = tables.nth(2).getByRole('row');
	await page.keyboard.press('Enter');
	await page.keyboard.press('Enter');
	await expect(notes).toHaveCount(3);
	await page.keyboard.press('ArrowDown');
	await expect(notes.nth(2)).toHaveAttribute('aria-selected', 'true');
	await expect(invoices.nth(1)).toHaveAttribute('aria-selected', 'true');
	await page.keyboard.press('Escape');
	await page.keyboard.press('ArrowDown');
	await expect(invoices.nth(2)).toHaveAttribute('aria-selected', 'true');
	await expect(notes).toHaveCount(2);
	await expect(notes.nth(1)).toHaveAttribute('aria-selected', 'true');
});
