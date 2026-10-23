import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { searchFor } from './list';

// Codes 9401–9449 are this file's: the other e2e files use their own.

interface Saved {
	id: number;
	code: number;
}

interface Note extends Saved {
	date: string;
	clientId: number;
	lines: { articleId: number; traceabilityCode1: string; quantity: number; price: number }[];
}

interface InvoicedNote extends Note {
	invoiceId: number;
	vatPercentage: number;
	surchargePercentage: number;
}

interface Amounts {
	subtotal: number;
	vatPercentage: number;
	vatAmount: number;
	surchargePercentage: number;
	surchargeAmount: number;
	total: number;
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

async function ensureClient(
	request: APIRequestContext,
	code: number,
	name: string,
	extra: Record<string, string> = {}
) {
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
		defaultDeliveryNoteFormat: 'VALUED',
		...extra
	});
}

/** A client whose notes can be invoiced: issuing needs the owner and the client's fiscal data. */
async function invoiceableClient(request: APIRequestContext, code: number, name: string) {
	// 409: another test created the owner already
	await request.post('/api/user-profile', {
		data: {
			fullName: 'María López',
			fiscalName: 'Frutas López S.L.',
			nif: 'B87654321',
			address: 'Mercavalencia, nave 3',
			email: 'facturas@ejemplo.es'
		}
	});
	return ensureClient(request, code, name, {
		nif: 'B12345674',
		address: 'Calle Mayor 1',
		postalCode: '46001',
		city: 'Valencia'
	});
}

const ensureArticle = (request: APIRequestContext, code: number, name: string) =>
	ensure(request, '/api/articles', { code, name });

async function createNote(
	request: APIRequestContext,
	clientId: number,
	date: string,
	line: { articleId: number; quantity: number; price: number }
): Promise<Note> {
	const response = await request.post('/api/delivery-notes', {
		data: { date, clientId, lines: [{ traceabilityCode1: '01010126', ...line }] }
	});
	expect(response.ok()).toBe(true);
	return (await response.json()) as Note;
}

async function getNote(request: APIRequestContext, id: number): Promise<Note> {
	const response = await request.get(`/api/delivery-notes/${id}`);
	expect(response.ok()).toBe(true);
	return (await response.json()) as Note;
}

const today = () => new Date().toLocaleDateString('sv');
const shown = (iso: string) => iso.split('-').reverse().join('/');

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

type Column = 'Código' | 'Lote' | 'Cantidad' | 'Precio';
const cell = (page: Page, row: number, column: Column) =>
	page.getByLabel(column, { exact: true }).nth(row);

test('a delivery note by keyboard', async ({ page, request }) => {
	const client = await ensureClient(request, 9401, 'Frutería Teclado e2e');
	const tomate = await ensureArticle(request, 9402, 'Tomate teclado e2e');
	const pepino = await ensureArticle(request, 9403, 'Pepino teclado e2e');
	// The tomato's last price to this client: proposed in the new note.
	await createNote(request, client.id, `${today()}T00:00:01`, {
		articleId: tomate.id,
		quantity: 1,
		price: 1.2
	});

	await page.goto('/delivery-notes');
	await ready(page, 'Nuevo');
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Albarán nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	const cliente = page.getByLabel('Cliente', { exact: true });
	const fecha = page.getByLabel('Fecha', { exact: true });
	await expect(cliente).toBeFocused();
	await page.keyboard.type('9401');
	await page.keyboard.press('Enter');
	await expect(fecha).toBeFocused();
	await expect(page.getByText('Frutería Teclado e2e')).toBeVisible();
	await expect(fecha).toHaveValue(shown(today()));
	await page.keyboard.press('Enter');

	await expect(cell(page, 0, 'Código')).toBeFocused();
	await page.keyboard.type('9402');
	await page.keyboard.press('Enter');
	await expect(cell(page, 0, 'Lote')).toBeFocused();
	await page.keyboard.type('01181026');
	await page.keyboard.press('Enter');
	await page.keyboard.type('25');
	await page.keyboard.press('Enter');
	await expect(cell(page, 0, 'Precio')).toBeFocused();
	await expect(cell(page, 0, 'Precio')).toHaveValue('1,20');
	await page.keyboard.press('Enter');

	await expect(cell(page, 1, 'Código')).toBeFocused();
	await page.keyboard.type('9403');
	await page.keyboard.press('Enter');
	await expect(cell(page, 1, 'Lote')).toBeFocused();
	await page.keyboard.type('02181026');
	await page.keyboard.press('Enter');
	await page.keyboard.type('3');
	await page.keyboard.press('Enter');
	await page.keyboard.type('0,33');
	// Enter on the last price adds a line; "+" right after it saves without that empty line.
	await page.keyboard.press('Enter');
	await page.keyboard.press('NumpadAdd');

	const toast = page.getByText(/^Albarán \d+ guardado$/);
	await expect(toast).toBeVisible();
	await expect(cliente).toBeFocused();
	await expect(cliente).toHaveValue('');
	await expect(fecha).toHaveValue(shown(today()));
	await expect(cell(page, 0, 'Código')).toHaveValue('');
	await expect(cell(page, 1, 'Código')).toHaveCount(0);

	const code = Number((await toast.textContent())?.match(/\d+/)?.[0]);
	// Back to the list: the new note is there, still to be invoiced.
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL(/\/delivery-notes$/);
	await expect(page.getByRole('row', { name: new RegExp(`^${code} `) })).toContainText('Pendiente');
	const list = await request.get(`/api/delivery-notes?codeFrom=${code}&codeTo=${code}`);
	const [note] = ((await list.json()) as { content: Note[] }).content;
	expect(note.clientId).toBe(client.id);
	expect(note.date.slice(0, 10)).toBe(today());
	expect(note.lines).toMatchObject([
		{ articleId: tomate.id, traceabilityCode1: '01181026', quantity: 25, price: 1.2 },
		{ articleId: pepino.id, traceabilityCode1: '02181026', quantity: 3, price: 0.33 }
	]);
});

test('editing a note keeps it and returns to the list', async ({ page, request }) => {
	const client = await ensureClient(request, 9404, 'Bar Edición e2e');
	const article = await ensureArticle(request, 9405, 'Lechuga edición e2e');
	const other = await ensureArticle(request, 9409, 'Escarola edición e2e');
	const note = await createNote(request, client.id, `${today()}T07:15:30`, {
		articleId: article.id,
		quantity: 2,
		price: 1.5
	});

	await page.goto(`/delivery-notes/${note.id}`);
	await expect(page.getByRole('heading', { name: `Albarán ${note.code}` })).toBeVisible();
	await ready(page, 'Guardar');
	// The note has ids: the page shows its codes and names.
	await expect(page.getByLabel('Cliente', { exact: true })).toHaveValue('9404');
	await expect(page.getByText('Bar Edición e2e')).toBeVisible();
	await expect(cell(page, 0, 'Código')).toHaveValue('9405');
	await expect(page.getByRole('cell', { name: 'Lechuga edición e2e' })).toBeVisible();
	await expect(cell(page, 0, 'Cantidad')).toHaveValue('2,00');

	await cell(page, 0, 'Cantidad').click();
	await page.keyboard.type('5');
	// Another article, and "+" right after its Enter, without waiting for its name: it saves once
	// the article is found, and the saved price stays.
	await cell(page, 0, 'Código').click();
	await page.keyboard.type('9409');
	await page.keyboard.press('Enter');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Albarán ${note.code} guardado`)).toBeVisible();
	await expect(page).toHaveURL(/\/delivery-notes$/);
	// The list marks the note just edited.
	await expect(page.getByRole('row', { name: new RegExp(`^${note.code} `) })).toHaveAttribute(
		'aria-selected',
		'true'
	);

	const edited = await getNote(request, note.id);
	expect(edited.code).toBe(note.code);
	expect(edited.date).toBe(`${today()}T07:15:30`);
	expect(edited.lines).toMatchObject([{ articleId: other.id, quantity: 5, price: 1.5 }]);
});

test('a missing note shows the not-found page', async ({ page }) => {
	await page.goto('/delivery-notes/999999');
	await expect(page.getByText('No se ha encontrado')).toBeVisible();
});

/** An issued invoice with one note of a fresh client. */
async function issuedNote(request: APIRequestContext, clientCode: number, name: string) {
	const client = await invoiceableClient(request, clientCode, name);
	const article = await ensureArticle(request, clientCode + 20, `${name} artículo`);
	const note = await createNote(request, client.id, `${today()}T09:00:00`, {
		articleId: article.id,
		quantity: 2,
		price: 3
	});
	const invoice = await request.post('/api/invoices', {
		data: { clientId: client.id, deliveryNoteIds: [note.id] }
	});
	expect(invoice.ok()).toBe(true);
	const { id } = (await invoice.json()) as { id: number };
	const issued = await request.post(`/api/invoices/${id}/issue`);
	expect(issued.ok()).toBe(true);
	const { number } = (await issued.json()) as { number: string };
	return { note, number };
}

test('the list finds by client without accents and filters pending', async ({ page, request }) => {
	const garcia = await ensureClient(request, 9406, 'Frutería García e2e');
	const article = await ensureArticle(request, 9407, 'Naranja lista e2e');
	const pending = await createNote(request, garcia.id, `${today()}T08:00:00`, {
		articleId: article.id,
		quantity: 1,
		price: 2
	});
	const { note: issued, number } = await issuedNote(request, 9408, 'Verdulería Facturada e2e');

	await page.goto('/delivery-notes');
	await ready(page, 'Solo pendientes');
	const pendingRow = page.getByRole('row', { name: new RegExp(`^${pending.code} `) });
	const issuedRow = page.getByRole('row', { name: new RegExp(`^${issued.code} `) });
	await expect(pendingRow).toBeVisible();
	await expect(issuedRow).toContainText(`Facturado nº ${number}`);

	await searchFor(page, 'garcia e2e');
	await expect(pendingRow).toContainText('Frutería García e2e');
	await expect(pendingRow).toContainText('Pendiente');
	await expect(issuedRow).toHaveCount(0);
	await page.getByLabel('Buscar (código o nombre)').fill('');
	await expect(page).not.toHaveURL(/q=/);
	await expect(issuedRow).toBeVisible();

	await page.keyboard.press('F3');
	await expect(page).toHaveURL(/pending=1/);
	await expect(page.getByRole('button', { name: /Ver todos/ })).toContainText('F3');
	await expect(issuedRow).toHaveCount(0);
	await expect(pendingRow).toBeVisible();
	await page.keyboard.press('F3');
	await expect(page.getByRole('button', { name: /Solo pendientes/ })).toBeVisible();
	await expect(issuedRow).toBeVisible();
});

test('an issued note opens read-only', async ({ page, request }) => {
	const { note, number } = await issuedNote(request, 9410, 'Frutas Emitidas e2e');

	await page.goto(`/delivery-notes/${note.id}`);
	await expect(page.getByText(`Está en la factura nº ${number}, emitida`)).toBeVisible();
	await ready(page, 'Volver');
	await expect(page.getByRole('button', { name: /Guardar/ })).toHaveCount(0);
	const writes: string[] = [];
	page.on('request', (r) => {
		if (r.method() !== 'GET' && r.url().includes('/api/')) writes.push(`${r.method()} ${r.url()}`);
	});
	const before = await getNote(request, note.id);
	await page.keyboard.press('NumpadAdd');
	// Esc and the list rendering are the anchor: a save started by "+" would have gone out by then.
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL(/\/delivery-notes$/);
	await expect(page.getByRole('row', { name: new RegExp(`^${note.code} `) })).toBeVisible();
	expect(writes).toEqual([]);
	expect(await getNote(request, note.id)).toEqual(before);
});

/** In a new note, by keyboard: the client, today, and one line of the article, 2 × 3,00. */
async function typeNote(page: Page, clientCode: number, articleCode: number) {
	await expect(page.getByLabel('Cliente', { exact: true })).toBeFocused();
	await page.keyboard.type(String(clientCode));
	await page.keyboard.press('Enter');
	await expect(page.getByLabel('Fecha', { exact: true })).toBeFocused();
	await page.keyboard.press('Enter');
	await expect(cell(page, 0, 'Código')).toBeFocused();
	await page.keyboard.type(String(articleCode));
	await page.keyboard.press('Enter');
	await expect(cell(page, 0, 'Lote')).toBeFocused();
	await page.keyboard.type('01221026');
	await page.keyboard.press('Enter');
	await page.keyboard.type('2');
	await page.keyboard.press('Enter');
	await expect(cell(page, 0, 'Precio')).toBeFocused();
	await page.keyboard.type('3');
}

const checkbox = (page: Page, name: RegExp) => page.getByRole('checkbox', { name });

test('issuing on save leaves the note invoiced', async ({ page, request }) => {
	await invoiceableClient(request, 9411, 'Frutas Factura e2e');
	await ensureArticle(request, 9412, 'Melón factura e2e');

	await page.goto('/delivery-notes');
	await ready(page, 'Nuevo');
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Albarán nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	await typeNote(page, 9411, 9412);
	const issue = checkbox(page, /Emitir factura/);
	await expect(issue).toHaveAttribute('aria-disabled', 'false');
	await page.keyboard.press('F3');
	await expect(issue).toHaveAttribute('aria-checked', 'true');
	await page.keyboard.press('NumpadAdd');

	const toast = page.getByText(/^Albarán \d+ guardado · Factura \S+ emitida$/);
	await expect(toast).toBeVisible();
	const [, code, number] = /^Albarán (\d+) guardado · Factura (\S+) emitida$/.exec(
		(await toast.textContent()) ?? ''
	)!;
	// The next note of the chain.
	await expect(page.getByLabel('Cliente', { exact: true })).toBeFocused();
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL(/\/delivery-notes$/);
	await expect(page.getByRole('row', { name: new RegExp(`^${code} `) })).toContainText(
		`Facturado nº ${number}`
	);

	// The invoice carries the note's amounts and rates, not zeros.
	const list = await request.get(`/api/delivery-notes?codeFrom=${code}&codeTo=${code}`);
	const [note] = ((await list.json()) as { content: InvoicedNote[] }).content;
	const response = await request.get(`/api/invoices/${note.invoiceId}`);
	expect(response.ok()).toBe(true);
	const invoice = (await response.json()) as Amounts & { number: string };
	expect(invoice.number).toBe(number);
	const subtotal = note.lines.reduce((sum, line) => sum + line.quantity * line.price, 0);
	expect(subtotal).toBe(6); // 2 × 3, as typed
	expect(invoice.subtotal).toBe(subtotal);
	expect(invoice.vatPercentage).toBe(note.vatPercentage);
	expect(invoice.surchargePercentage).toBe(note.surchargePercentage);
	const share = (percentage: number) => Math.round(subtotal * percentage) / 100;
	expect(invoice.vatAmount).toBeGreaterThan(0);
	expect(invoice.vatAmount).toBeCloseTo(share(note.vatPercentage), 2);
	expect(invoice.surchargeAmount).toBeCloseTo(share(note.surchargePercentage), 2);
	expect(invoice.total).toBeCloseTo(subtotal + invoice.vatAmount + invoice.surchargeAmount, 2);
});

// Needs the PDF viewer of the full Chromium (playwright.config.ts): without one, the PDF never loads.
test('printing on save calls print', async ({ page, request }) => {
	await ensureClient(request, 9413, 'Bar Impresión e2e');
	await ensureArticle(request, 9414, 'Pera impresión e2e');
	// In every frame, the PDF's too: print() counts in the page instead of opening the dialog, and
	// fires afterprint as a closed dialog does.
	await page.addInitScript(() => {
		window.print = () => {
			const top = window.top as Window & { printed?: number };
			top.printed = (top.printed ?? 0) + 1;
			window.dispatchEvent(new Event('afterprint'));
		};
	});
	const printed = () => page.evaluate(() => (window as Window & { printed?: number }).printed ?? 0);

	await page.goto('/delivery-notes/new');
	await ready(page, 'Guardar');
	await typeNote(page, 9413, 9414);
	const print = checkbox(page, /Imprimir albarán/);
	await page.keyboard.press('F9');
	await expect(print).toHaveAttribute('aria-checked', 'true');
	const pdf = page.waitForResponse((r) => /\/api\/delivery-notes\/\d+\/pdf$/.test(r.url()));
	await page.keyboard.press('NumpadAdd');

	expect((await pdf).ok()).toBe(true);
	await expect.poll(printed).toBe(1);
	await expect(page.getByText(/^Albarán \d+ guardado$/)).toBeVisible();
	await expect(page.getByRole('dialog')).toHaveCount(0);
	// The next note of the chain, with the cursor and unticked.
	await expect(page.getByLabel('Cliente', { exact: true })).toBeFocused();
	await expect(print).toHaveAttribute('aria-checked', 'false');
	// Printing ended: the frame it printed from is gone.
	await expect(page.locator('iframe')).toHaveCount(0);
});

test('the reason under "Emitir factura" has its line kept: the lines do not move', async ({
	page,
	request
}) => {
	await invoiceableClient(request, 9417, 'Frutas Con Datos e2e');
	// The same regime and format, so only the reason changes.
	await ensureClient(request, 9418, 'Bar Sin Datos e2e');

	await page.goto('/delivery-notes/new');
	await ready(page, 'Guardar');
	const cliente = page.getByLabel('Cliente', { exact: true });
	const issue = checkbox(page, /Emitir factura/);
	const lineTop = async () => (await cell(page, 0, 'Código').boundingBox())!.y;
	await page.keyboard.type('9417');
	await page.keyboard.press('Enter');
	await expect(issue).toHaveAttribute('aria-disabled', 'false');
	const top = await lineTop();

	await cliente.click();
	await page.keyboard.type('9418');
	await page.keyboard.press('Enter');
	await expect(page.getByText(/^Faltan datos del cliente para facturar/)).toBeVisible();
	await expect(issue).toHaveAttribute('aria-disabled', 'true');
	expect(await lineTop()).toBe(top);
});

test('a long note scrolls its lines, never the page: the totals stay in sight', async ({
	page,
	request
}) => {
	const client = await ensureClient(request, 9415, 'Frutería Larga e2e');
	const article = await ensureArticle(request, 9416, 'Patata larga e2e');
	const lines = Array.from({ length: 20 }, (_, i) => ({
		articleId: article.id,
		traceabilityCode1: '01221026',
		quantity: i + 1,
		price: 1
	}));
	const created = await request.post('/api/delivery-notes', {
		data: { date: `${today()}T10:00:00`, clientId: client.id, lines }
	});
	expect(created.ok()).toBe(true);
	const note = (await created.json()) as Note;

	await page.setViewportSize({ width: 1920, height: 1080 });
	await page.goto(`/delivery-notes/${note.id}`);
	await ready(page, 'Guardar');
	const totals = page.getByRole('region', { name: 'Totales' });
	const pageScrolls = () =>
		page.evaluate(() => document.documentElement.scrollHeight > window.innerHeight);
	/** Inside the window, below the column titles and above the key bar. */
	async function inSight(field: ReturnType<typeof cell>) {
		await expect(field).toBeInViewport({ ratio: 1 });
		const box = (await field.boundingBox())!;
		const titles = (await page.getByRole('columnheader', { name: 'Código' }).boundingBox())!;
		const bar = (await page.locator('footer').boundingBox())!;
		expect(box.y).toBeGreaterThanOrEqual(titles.y + titles.height - 1);
		expect(box.y + box.height).toBeLessThanOrEqual(bar.y);
	}

	// Enter past the last price: a new line, in sight, and the header where it was.
	await cell(page, 19, 'Precio').click();
	await page.keyboard.press('Enter');
	await expect(cell(page, 20, 'Código')).toBeFocused();
	await inSight(cell(page, 20, 'Código'));
	await expect(totals).toBeInViewport({ ratio: 1 });
	expect(await pageScrolls()).toBe(false);

	// ↑ all the way up: the first line comes out below the column titles.
	for (let i = 0; i < 20; i++) await page.keyboard.press('ArrowUp');
	await expect(cell(page, 0, 'Código')).toBeFocused();
	await inSight(cell(page, 0, 'Código'));
	// ↓ back down to the last one.
	for (let i = 0; i < 20; i++) await page.keyboard.press('ArrowDown');
	await expect(cell(page, 20, 'Código')).toBeFocused();
	await inSight(cell(page, 20, 'Código'));
	await expect(totals).toBeInViewport({ ratio: 1 });
	expect(await pageScrolls()).toBe(false);
});
