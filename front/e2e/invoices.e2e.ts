import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

// Codes 9451–9499 are this file's: the other e2e files use their own. Its notes are dated in
// 2026-02, a month no other file uses: Facturar up to 28/02/2026 sees only them.

interface Saved {
	id: number;
	code: number;
}

interface Note extends Saved {
	invoiceId: number | null;
	invoiceStatus: 'DRAFT' | 'ISSUED' | null;
}

interface Pending {
	clientCode: number;
	total: number;
	deliveryNotes: { id: number }[];
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

async function createNote(
	request: APIRequestContext,
	clientId: number,
	date: string,
	line: { articleId: number; quantity: number; price: number }
): Promise<Saved> {
	const response = await request.post('/api/delivery-notes', {
		data: { date, clientId, lines: [{ traceabilityCode1: '01010226', ...line }] }
	});
	expect(response.ok()).toBe(true);
	return (await response.json()) as Saved;
}

async function getJson<T>(request: APIRequestContext, path: string): Promise<T> {
	const response = await request.get(path);
	expect(response.ok()).toBe(true);
	return (await response.json()) as T;
}

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

test('billing issues the pending notes up to a date', async ({ page, request }) => {
	const first = await invoiceableClient(request, 9451, 'Frutas Facturar e2e');
	const second = await invoiceableClient(request, 9452, 'Bar Facturar e2e');
	const article = await ensure(request, '/api/articles', {
		code: 9453,
		name: 'Melón facturar e2e'
	});
	const notes = [
		await createNote(request, first.id, '2026-02-10T09:00:00', {
			articleId: article.id,
			quantity: 3,
			price: 2.35
		}),
		await createNote(request, first.id, '2026-02-20T09:00:00', {
			articleId: article.id,
			quantity: 1.5,
			price: 4.1
		}),
		await createNote(request, second.id, '2026-02-20T10:00:00', {
			articleId: article.id,
			quantity: 7,
			price: 1.15
		})
	];
	// What each future invoice comes to, read before issuing.
	const pending = await getJson<Pending[]>(request, '/api/invoices/pending?dateTo=2026-02-28');
	const totalOf = new Map(
		pending.flatMap((row) => row.deliveryNotes.map((n) => [n.id, row.total] as const))
	);
	expect(pending.map((row) => row.clientCode)).toEqual([9451, 9452]);

	await page.goto('/');
	await ready(page, '1–6');
	await page.keyboard.press('2');
	await expect(page).toHaveURL('/invoices');
	await ready(page, '1–2');
	await page.keyboard.press('1');
	await expect(page).toHaveURL('/invoices/billing');
	await expect(page.getByRole('heading', { name: 'Facturar' })).toBeVisible();
	await ready(page, 'Volver');
	await expect(page.getByLabel('Facturar hasta')).toBeFocused();
	await page.keyboard.type('280226');
	await page.keyboard.press('Enter');
	const rows = page.getByRole('table').first().getByRole('row');
	await expect(rows).toHaveCount(3);

	// The second client alone, with F3.
	await page.keyboard.press('ArrowDown');
	await expect(rows.nth(2)).toHaveAttribute('aria-selected', 'true');
	await page.keyboard.press('F3');
	await expect(page.getByRole('dialog')).toContainText('¿Emitir la factura de Bar Facturar e2e?');
	await page.keyboard.press('F3');
	await expect(page.getByText(/^Factura \d+ emitida$/)).toBeVisible();
	await expect(rows).toHaveCount(2);

	// The rest, with "+".
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByRole('dialog')).toContainText('¿Emitir 1 factura?');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(/emitida/).last()).toBeVisible();
	await expect(page.getByText('No hay albaranes pendientes hasta el 28/02/2026.')).toBeVisible();

	for (const saved of notes) {
		const note = await getJson<Note>(request, `/api/delivery-notes/${saved.id}`);
		expect(note.invoiceStatus).toBe('ISSUED');
		const invoice = await getJson<{ total: number }>(request, `/api/invoices/${note.invoiceId}`);
		expect(invoice.total).toBe(totalOf.get(saved.id));
	}
});

test('the ledger shows an issued invoice and deleting frees its notes', async ({
	page,
	request
}) => {
	// This test's notes are dated in 2026-03, which no other test uses.
	const client = await invoiceableClient(request, 9470, 'Frutas Libro e2e');
	const article = await ensure(request, '/api/articles', { code: 9471, name: 'Pera libro e2e' });
	const note = await createNote(request, client.id, '2026-03-10T09:00:00', {
		articleId: article.id,
		quantity: 4,
		price: 2.5
	});
	const issued = await request.post('/api/invoices/issue', {
		data: { clientId: client.id, deliveryNoteIds: [note.id] }
	});
	expect(issued.ok()).toBe(true);
	const invoice = (await issued.json()) as { id: number; number: string; total: number };

	await page.goto('/');
	await ready(page, '1–6');
	await page.keyboard.press('2');
	await expect(page).toHaveURL('/invoices');
	await ready(page, '1–2');
	await page.keyboard.press('2');
	await expect(page).toHaveURL('/invoices/ledger');
	await ready(page, 'Borrar');

	// Issued today: the default period shows it; it is the newest, so the last row.
	const rows = page.getByRole('table').first().getByRole('row');
	const mine = rows.filter({ hasText: 'Frutas Libro e2e' });
	await expect(mine).toHaveCount(1);
	await expect(mine).toContainText(invoice.number);
	await expect(mine).toContainText(formatEuros(invoice.total));
	await page.keyboard.press('Enter');
	await page.keyboard.press('Enter');
	await expect(page.getByLabel('Hasta')).not.toBeFocused();
	const count = await rows.count();
	for (let i = 2; i < count; i++) await page.keyboard.press('ArrowDown');
	await expect(mine).toHaveAttribute('aria-selected', 'true');
	await expect(page.getByRole('region')).toContainText(`Factura ${invoice.number}`);

	await page.keyboard.press('F8');
	await expect(page.getByRole('dialog')).toContainText(`¿Borrar la factura ${invoice.number}?`);
	await page.keyboard.press('F8');
	await expect(page.getByText(`Factura ${invoice.number} borrada`)).toBeVisible();
	await expect(mine).toHaveCount(0);

	const pending = await getJson<Pending[]>(request, '/api/invoices/pending?dateTo=2026-03-31');
	const row = pending.find((p) => p.clientCode === 9470);
	expect(row?.deliveryNotes.map((n) => n.id)).toEqual([note.id]);

	// Back to the menu (table → Desde → menu), and Facturar sees the note again.
	await page.keyboard.press('Escape');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/invoices');
	await ready(page, '1–2');
	await page.keyboard.press('1');
	await expect(page.getByLabel('Facturar hasta')).toBeFocused();
	await page.keyboard.type('310326');
	await page.keyboard.press('Enter');
	await expect(
		page.getByRole('table').first().getByRole('row').filter({ hasText: 'Frutas Libro e2e' })
	).toHaveCount(1);
});

/** "1.234,50 €" as the app prints it. */
function formatEuros(amount: number): string {
	return `${amount.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} €`;
}
