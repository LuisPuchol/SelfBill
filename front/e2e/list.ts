import { expect, type Page } from '@playwright/test';

/**
 * Types in a list's search box and waits for the search to reach the URL: it goes out after a
 * pause, and until then the rows on screen are the old ones.
 */
export async function searchFor(page: Page, text: string) {
	await page.getByLabel('Buscar (código o nombre)').fill(text);
	await expect(page).toHaveURL((url) => url.searchParams.get('q') === text);
}
