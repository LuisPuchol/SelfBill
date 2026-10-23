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
