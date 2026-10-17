import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

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

	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	await ready(page, 'Nuevo');
	await page.getByLabel('Buscar (código o nombre)').fill('e2e');
	await expect(page.getByRole('cell', { name: 'Tomate e2e A' })).toBeVisible();
	await expect(page.getByRole('cell', { name: 'Tomate e2e B' })).toBeVisible();
});

test('search by name and by code', async ({ page, request }) => {
	const { id } = await createArticle(request, 9101, 'Zanahoria búsqueda');
	await openList(page);
	const search = page.getByLabel('Buscar (código o nombre)');
	await search.fill('ZANAHORIA busqueda');
	await expect(page.getByRole('cell', { name: 'Zanahoria búsqueda' })).toBeVisible();
	await search.fill('9101');
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await expect(page.getByRole('heading', { name: 'Artículo 9101' })).toBeVisible();
});

test('an edit returns to the list with the row selected', async ({ page, request }) => {
	const { id } = await createArticle(request, 9102, 'Berenjena editar');
	await openList(page);
	await page.getByLabel('Buscar (código o nombre)').fill('9102');
	await page.keyboard.press('Enter');
	await expect(page).toHaveURL(`/articles/${id}`);
	await ready(page, 'Guardar');
	await page.getByLabel('Nombre').fill('Berenjena editada');
	await page.keyboard.press('NumpadAdd');
	await expect(page.getByText('Artículo 9102 guardado')).toBeVisible();
	await expect(page).toHaveURL(/\/articles(\?.*)?$/);
	await expect(page.getByRole('heading', { name: 'Artículos' })).toBeVisible();
	const row = page.getByRole('row', { name: /Berenjena editada/ });
	await expect(row).toBeVisible();
	await expect(row).toHaveAttribute('aria-selected', 'true');
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
	await page.getByLabel('Buscar (código o nombre)').fill('9105');
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
