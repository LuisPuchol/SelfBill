import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { searchFor } from './list';

async function createArticle(request: APIRequestContext, code: number, name: string) {
	const response = await request.post('/api/articles', { data: { code, name } });
	expect(response.ok()).toBe(true);
	return (await response.json()) as { id: number; code: number };
}

/** The key bar lists an action only once its keyboard layer is registered. */
async function ready(page: Page, action: string) {
	await expect(page.locator('footer')).toContainText(action);
}

async function openList(page: Page) {
	await page.goto('/articles');
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	await ready(page, 'Nuevo');
}

test('two new articles in a row', async ({ page }) => {
	await openList(page);
	// Its own rows: other tests leave enough articles to push new ones off the first page.
	await searchFor(page, 'Tomate e2e');
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Artículo nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	const code = Number(await page.getByLabel('Código').inputValue());
	expect(code).toBeGreaterThan(0);

	await page.getByLabel('Nombre').fill('Tomate e2e A');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Artículo ${code} guardado`)).toBeVisible();
	await expect(page.getByLabel('Código')).toHaveValue(String(code + 1));
	await expect(page.getByLabel('Nombre')).toHaveValue('');

	await page.getByLabel('Nombre').fill('Tomate e2e B');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Artículo ${code + 1} guardado`)).toBeVisible();
	await page.keyboard.press('Escape');

	// Back to the list it came from, with the last one saved selected.
	await expect(page).toHaveURL(
		(url) => url.pathname === '/articles' && url.searchParams.get('q') === 'Tomate e2e'
	);
	await ready(page, 'Nuevo');
	await expect(page.getByRole('row', { name: /Tomate e2e B/ })).toHaveAttribute(
		'aria-selected',
		'true'
	);
	await expect(page.getByRole('cell', { name: 'Tomate e2e A' })).toBeVisible();
});

test('search by name and by code', async ({ page, request }) => {
	const { id } = await createArticle(request, 9101, 'Zanahoria búsqueda');
	await createArticle(request, 9106, 'Zanahoria otra');
	await openList(page);
	await searchFor(page, 'zanahoria');
	await expect(page.getByRole('cell', { name: 'Zanahoria otra' })).toBeVisible();
	// Neither accents nor case count.
	await searchFor(page, 'ZANAHORIA busqueda');
	await expect(page.getByRole('cell', { name: 'Zanahoria búsqueda' })).toBeVisible();
	await expect(page.getByRole('cell', { name: 'Zanahoria otra' })).toBeHidden();
	// Digits are a code: Enter opens it, even before the search has come back.
	await page.getByLabel('Buscar (código o nombre)').fill('9101');
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await expect(page.getByRole('heading', { name: 'Artículo 9101' })).toBeVisible();
});

test('an edit returns to the list with the row selected', async ({ page, request }) => {
	await createArticle(request, 9100, 'Berenjena primera');
	const { id } = await createArticle(request, 9102, 'Berenjena editar');
	await openList(page);
	await searchFor(page, 'berenjena');
	await expect(page.getByRole('row', { name: /Berenjena/ })).toHaveCount(2);
	// The second row: going back must not simply land on the first.
	await page.keyboard.press('ArrowDown');
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await ready(page, 'Guardar');
	await page.getByLabel('Nombre').fill('Berenjena editada');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Artículo 9102 guardado')).toBeVisible();
	await expect(page).toHaveURL('/articles?q=berenjena');
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	const row = page.getByRole('row', { name: /Berenjena editada/ });
	await expect(row).toBeVisible();
	await expect(row).toHaveAttribute('aria-selected', 'true');
	await expect(page.getByRole('row', { name: /Berenjena primera/ })).not.toHaveAttribute(
		'aria-selected',
		'true'
	);
});

test('Esc from a new article goes back to the list it came from', async ({ page, request }) => {
	const { id } = await createArticle(request, 9107, 'Alcachofa memoria');
	// An earlier visit to a record leaves the list remembered with its search.
	await openList(page);
	await searchFor(page, 'alcachofa');
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await ready(page, 'Guardar');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/articles?q=alcachofa');
	await ready(page, 'Borrar búsqueda');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/articles');
	await ready(page, 'Salir');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/');

	// The list again, now with no search: F2, save one, Esc.
	await expect(page.locator('footer')).toContainText('1–6');
	await page.keyboard.press('4');
	await expect(page).toHaveURL('/articles');
	await ready(page, 'Nuevo');
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Artículo nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	const code = await page.getByLabel('Código').inputValue();
	await page.getByLabel('Nombre').fill('Alcachofa nueva');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText(`Artículo ${code} guardado`)).toBeVisible();
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/articles');
	await ready(page, 'Nuevo');
	// The new one may be past the first page of the whole list: its search still selects it.
	await searchFor(page, 'alcachofa');
	await expect(page.getByRole('row', { name: /Alcachofa nueva/ })).toHaveAttribute(
		'aria-selected',
		'true'
	);

	// With a search on the list when F2 is pressed, Esc comes back to that search.
	await page.keyboard.press('F2');
	await expect(page.getByRole('heading', { name: 'Artículo nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	await page.keyboard.press('Escape');
	await expect(page).toHaveURL('/articles?q=alcachofa');
});

test('a repeated code is reported under Código', async ({ page, request }) => {
	await createArticle(request, 9103, 'Cebolla repetida');
	await page.goto('/articles/new');
	await ready(page, 'Guardar');
	await page.getByLabel('Código').fill('9103');
	await page.getByLabel('Nombre').fill('Cebolla otra');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Ya existe un artículo con ese código')).toBeVisible();
	await expect(page.getByLabel('Código')).toBeFocused();
});

test('delete asks and removes', async ({ page, request }) => {
	const { id } = await createArticle(request, 9104, 'Puerro borrar');
	await page.goto(`/articles/${id}`);
	await ready(page, 'Borrar');
	await page.keyboard.press('F8');
	await expect(page.getByText('¿Borrar el artículo 9104?')).toBeVisible();
	await expect(page.getByText('Puerro borrar')).toBeVisible();
	await page.keyboard.press('Enter');
	await expect(page.getByText('¿Borrar el artículo 9104?')).toBeHidden();
	await expect(page).toHaveURL(`/articles/${id}`);

	await page.keyboard.press('F8');
	await expect(page.getByText('¿Borrar el artículo 9104?')).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByText('Artículo 9104 borrado')).toBeVisible();
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	await ready(page, 'Nuevo');
	await page.getByLabel('Buscar (código o nombre)').fill('9104');
	await expect(page.getByText('No existe ningún artículo con el código 9104.')).toBeVisible();
});

test('unsaved changes', async ({ page, request }) => {
	const { id } = await createArticle(request, 9105, 'Calabacín cambios');
	await page.goto(`/articles/${id}`);
	await ready(page, 'Guardar');
	await page.getByLabel('Nombre').fill('Calabacín cambiado');
	await page.keyboard.press('Escape');
	await expect(page.getByText('Hay cambios sin guardar')).toBeVisible();

	// Enter: keep editing, with the cursor back in the field
	await page.keyboard.press('Enter');
	await expect(page.getByText('Hay cambios sin guardar')).toBeHidden();
	await expect(page.getByLabel('Nombre')).toBeFocused();
	await expect(page).toHaveURL(`/articles/${id}`);

	// F8: leave without saving
	await page.keyboard.press('Escape');
	await expect(page.getByText('Hay cambios sin guardar')).toBeVisible();
	await page.keyboard.press('F8');
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	await ready(page, 'Nuevo');
	await searchFor(page, '9105');
	await expect(page.getByRole('cell', { name: 'Calabacín cambios' })).toBeVisible();

	// "+": save and leave
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await ready(page, 'Guardar');
	await page.getByLabel('Nombre').fill('Calabacín guardado');
	await page.keyboard.press('Escape');
	await expect(page.getByText('Hay cambios sin guardar')).toBeVisible();
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Artículo 9105 guardado')).toBeVisible();
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	await ready(page, 'Nuevo');
	await expect(page.getByRole('cell', { name: 'Calabacín guardado' })).toBeVisible();
});

test('with the mouse only: new, save, back, delete', async ({ page }) => {
	await openList(page);
	await page.getByRole('button', { name: /Nuevo/ }).click();
	await expect(page.getByRole('heading', { name: 'Artículo nuevo' })).toBeVisible();
	await ready(page, 'Guardar');
	const code = Number(await page.getByLabel('Código').inputValue());
	await page.getByLabel('Nombre').fill('Ratón e2e');
	await page.getByRole('button', { name: /Guardar/ }).click();
	await expect(page.getByText(`Artículo ${code} guardado`)).toBeVisible();
	await page.getByRole('button', { name: /Volver/ }).click();
	await expect(page).toHaveURL('/articles');
	await ready(page, 'Nuevo');

	await searchFor(page, 'Ratón e2e');
	await page.getByRole('row', { name: /Ratón e2e/ }).click();
	await page.getByRole('button', { name: /Borrar/ }).click();
	await expect(page.getByText(`¿Borrar el artículo ${code}?`)).toBeVisible();
	await expect(page.getByRole('dialog').getByText('Ratón e2e')).toBeVisible();
	await page
		.getByRole('dialog')
		.getByRole('button', { name: /Borrar/ })
		.click();
	await expect(page.getByText(`Artículo ${code} borrado`)).toBeVisible();
	await expect(page.getByRole('row', { name: /Ratón e2e/ })).toBeHidden();
	await expect(
		page.getByText('No hay ningún artículo que coincida con «Ratón e2e».')
	).toBeVisible();
});

test('the page buttons turn the page', async ({ page, request }) => {
	for (let i = 1; i <= 21; i++) {
		await createArticle(request, 9299 + i, `Página e2e ${String(i).padStart(2, '0')}`);
	}
	await openList(page);
	await searchFor(page, 'Página e2e');
	await expect(page.getByText('Página 1 de 2')).toBeVisible();
	await page.getByRole('button', { name: /Página siguiente/ }).click();
	await expect(page).toHaveURL(/page=1/);
	await expect(page.getByText('Página 2 de 2')).toBeVisible();
	await expect(page.getByRole('row', { name: /Página e2e/ })).toHaveCount(1);
	await page.getByRole('button', { name: /Página anterior/ }).click();
	await expect(page.getByText('Página 1 de 2')).toBeVisible();
});
