import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { searchFor } from './list';

async function createClient(
	request: APIRequestContext,
	code: number,
	name: string,
	extra: Record<string, string> = {}
) {
	const response = await request.post('/api/clients', {
		data: {
			code,
			name,
			businessName: `${name} SL`,
			vatType: 'WITH_VAT',
			surchargeType: 'WITHOUT_SURCHARGE',
			defaultDeliveryNoteFormat: 'PLAIN',
			...extra
		}
	});
	expect(response.ok()).toBe(true);
	return (await response.json()) as { id: number; code: number };
}

/** A client with one unbilled delivery note. */
async function clientWithNote(request: APIRequestContext, code: number, name: string) {
	// 409: the tax config already exists
	await request.post('/api/tax-config', {
		data: { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }
	});
	const article = await request.post('/api/articles', { data: { code, name: `Art ${name}` } });
	expect(article.ok()).toBe(true);
	const { id: articleId } = (await article.json()) as { id: number };
	const client = await createClient(request, code, name);
	const today = new Date().toLocaleDateString('sv');
	const note = await request.post('/api/delivery-notes', {
		data: {
			date: `${today}T08:00:00`,
			clientId: client.id,
			lines: [{ articleId, traceabilityCode1: '01210426', quantity: 1, price: 1 }]
		}
	});
	expect(note.ok()).toBe(true);
	return client;
}

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

async function openList(page: Page) {
	await page.goto('/clients');
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
	await ready(page, 'Nuevo');
}

test('an empty catalog says so', async ({ page, request }) => {
	// The temp DB starts empty; skip the check if an earlier test already created clients.
	const list = await request.get('/api/clients?size=1');
	const total = ((await list.json()) as { page: { totalElements: number } }).page.totalElements;
	test.skip(total > 0, 'clients already exist');
	await page.goto('/clients');
	await expect(
		page.getByText('Todavía no hay clientes. Pulsa F2 para dar de alta el primero.')
	).toBeVisible();
});

test('a minimal client saves and says what is missing for invoices', async ({ page }) => {
	await openList(page);
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Cliente nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	const code = Number(await page.getByLabel('Código', { exact: true }).inputValue());
	expect(code).toBeGreaterThan(0);

	await page.getByLabel('Nombre comercial').fill('Bar Minimal e2e');
	await page.getByLabel('Razón social').fill('Bar Minimal SL');
	const notice = 'Para poder facturarle faltan: NIF, dirección, código postal y población.';
	await expect(page.getByText(notice)).toBeVisible();
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Cliente ${code} guardado`)).toBeVisible();

	await page.keyboard.press('Escape');
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
	await ready(page, 'Nuevo');
	await page.getByLabel('Buscar (código o nombre)').fill(String(code));
	await page.keyboard.press('Enter');
	await expect(page.getByRole('heading', { name: `Cliente ${code}` })).toBeVisible();
	await ready(page, 'Guardar');
	await expect(page.getByText(notice)).toBeVisible();
	await page.getByLabel('NIF').fill('B12345678');
	await page.getByLabel('Dirección').fill('Calle Mayor 1');
	await page.getByLabel('Código postal').fill('46001');
	await page.getByLabel('Población').fill('Valencia');
	await expect(page.getByText(/Para poder facturarle faltan/)).toBeHidden();
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Cliente ${code} guardado`)).toBeVisible();
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
});

test('search by business name and by NIF', async ({ page, request }) => {
	// Only the business name carries the term, and with an accent.
	await createClient(request, 9201, 'Bar Pepe', {
		businessName: 'Hostelería García SL',
		nif: 'B98765432'
	});
	await createClient(request, 9204, 'Otro cliente');
	await openList(page);
	// Its own rows: the whole list depends on what other tests created.
	await searchFor(page, 'Otro cliente');
	await expect(page.getByRole('cell', { name: 'Otro cliente', exact: true })).toBeVisible();
	await searchFor(page, 'garcia');
	await expect(page.getByRole('cell', { name: 'Hostelería García SL' })).toBeVisible();
	await expect(page.getByRole('cell', { name: 'Otro cliente', exact: true })).toBeHidden();
	// A NIF fragment with its letter: digits alone would be a code.
	await searchFor(page, 'B98765');
	await expect(page.getByRole('cell', { name: 'B98765432' })).toBeVisible();
	await expect(page.getByRole('cell', { name: 'Otro cliente', exact: true })).toBeHidden();
});

test('a regime change with a pending delivery note asks first', async ({ page, request }) => {
	const { id } = await clientWithNote(request, 9202, 'Regimen e2e');
	await page.goto(`/clients/${id}`);
	await ready(page, 'Guardar');
	await page.getByLabel('Nombre comercial').fill('Regimen e2e');
	await page.locator('#vatType').focus();
	await page.keyboard.press('2');
	await page.keyboard.press('NumpadAdd');
	await expect(
		page.getByText(
			'Cambiar el IVA o el recargo recalculará 1 albarán sin facturar de este cliente.'
		)
	).toBeVisible();

	await page.keyboard.press('Enter');
	await expect(page.getByText(/Cambiar el IVA/)).toBeHidden();
	await expect(page).toHaveURL(`/clients/${id}`);

	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(/Cambiar el IVA/)).toBeVisible();
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Cliente 9202 guardado')).toBeVisible();
	await expect(page.getByRole('heading', { name: 'Clientes' })).toBeVisible();
});

test('deleting a client with a pending delivery note warns with the number', async ({
	page,
	request
}) => {
	const { id } = await clientWithNote(request, 9203, 'Borrar e2e');
	await page.goto(`/clients/${id}`);
	await ready(page, 'Borrar');
	await page.keyboard.press('F8');
	await expect(
		page.getByText('Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.')
	).toBeVisible();
	await page.keyboard.press('Enter');
	await expect(page.getByText(/Tiene 1 albarán/)).toBeHidden();
	await expect(page).toHaveURL(`/clients/${id}`);
});

test('deleting from the list warns with the number', async ({ page, request }) => {
	await clientWithNote(request, 9205, 'Lista borrar e2e');
	await openList(page);
	await searchFor(page, 'Lista borrar e2e');
	await expect(page.getByRole('row', { name: /Lista borrar e2e/ })).toBeVisible();
	await page.keyboard.press('F8');
	await expect(
		page.getByText('Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.')
	).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('Cliente 9205 borrado')).toBeVisible();
	await expect(page.getByRole('row', { name: /Lista borrar e2e/ })).toBeHidden();
});

test('F6 shows the deleted clients, and F8 there restores one', async ({ page, request }) => {
	await createClient(request, 9250, 'Restaurar e2e');
	await openList(page);
	await searchFor(page, 'Restaurar e2e');
	await expect(page.getByRole('row', { name: /Restaurar e2e/ })).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('¿Borrar el cliente 9250?')).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('Cliente 9250 borrado')).toBeVisible();
	await expect(page.getByRole('row', { name: /Restaurar e2e/ })).toBeHidden();

	// The deleted ones, with the same search and the day each one was deleted.
	await page.keyboard.press('F6');
	await expect(page).toHaveURL(
		(url) =>
			url.searchParams.get('deleted') === '1' && url.searchParams.get('q') === 'Restaurar e2e'
	);
	await expect(page.getByRole('heading', { name: 'Clientes borrados' })).toBeVisible();
	await ready(page, 'Restaurar');
	await expect(page.locator('footer')).not.toContainText('Nuevo');
	await expect(page.getByRole('columnheader', { name: 'Borrado' })).toBeVisible();
	const today = new Date().toLocaleDateString('es-ES', {
		day: '2-digit',
		month: '2-digit',
		year: 'numeric'
	});
	await expect(page.getByRole('row', { name: /Restaurar e2e/ })).toContainText(today);

	// Enter opens nothing: the next F8 still asks here.
	await page.keyboard.press('Enter');
	await page.keyboard.press('F8');
	await expect(page.getByText('¿Restaurar el cliente 9250?')).toBeVisible();
	await expect(page.getByRole('dialog').getByText('Restaurar e2e')).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('Cliente 9250 restaurado')).toBeVisible();
	await expect(
		page.getByText('No hay ningún cliente borrado que coincida con «Restaurar e2e».')
	).toBeVisible();

	// Back to the live ones, where it is again.
	await page.keyboard.press('F6');
	await expect(page).toHaveURL(
		(url) => !url.searchParams.has('deleted') && url.searchParams.get('q') === 'Restaurar e2e'
	);
	await expect(page.getByRole('heading', { name: 'Clientes', exact: true })).toBeVisible();
	await ready(page, 'Nuevo');
	await expect(page.getByRole('row', { name: /Restaurar e2e/ })).toBeVisible();
});

test('restoring a client whose code a live one has taken says so', async ({ page, request }) => {
	const { id } = await createClient(request, 9251, 'Ocupado e2e borrado');
	expect((await request.delete(`/api/clients/${id}`)).ok()).toBe(true);
	await createClient(request, 9251, 'Ocupado e2e vivo');
	await page.goto('/clients?deleted=1');
	await expect(page.getByRole('heading', { name: 'Clientes borrados' })).toBeVisible();
	await ready(page, 'Restaurar');
	await searchFor(page, 'Ocupado e2e');
	await expect(page.getByRole('row', { name: /Ocupado e2e borrado/ })).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('¿Restaurar el cliente 9251?')).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('Ya existe un cliente con ese código.')).toBeVisible();
	await page.keyboard.press('Enter');
	await expect(page.getByText('Ya existe un cliente con ese código.')).toBeHidden();
	await expect(page.getByRole('row', { name: /Ocupado e2e borrado/ })).toBeVisible();

	// Esc goes back to the live ones, with the search: only the live one has the code.
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL(
		(url) => !url.searchParams.has('deleted') && url.searchParams.get('q') === 'Ocupado e2e'
	);
	await ready(page, 'Nuevo');
	await expect(page.getByRole('row', { name: /Ocupado e2e vivo/ })).toBeVisible();
	await expect(page.getByRole('row', { name: /Ocupado e2e borrado/ })).toBeHidden();
	const live = await request.get('/api/clients?codeFrom=9251&codeTo=9251');
	expect(((await live.json()) as { page: { totalElements: number } }).page.totalElements).toBe(1);
});

test('a missing client shows the not-found page', async ({ page }) => {
	await page.goto('/clients/9999');
	await expect(page.getByText('No se ha encontrado')).toBeVisible();
	await ready(page, 'Volver');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/');
});
