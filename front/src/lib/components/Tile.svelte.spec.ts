import { describe, expect, it } from 'vitest';
import { page } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import { sections } from '#lib/sections.js';
import Tile from './Tile.svelte';

describe('Tile', () => {
	it('is a link to the section showing its digit and title', async () => {
		render(Tile, { section: sections[0] });
		const link = page.getByRole('link', { name: /Albaranes/ });
		await expect.element(link).toBeVisible();
		await expect.element(link).toHaveAttribute('href', '/delivery-notes');
		await expect.element(page.getByText('1', { exact: true })).toBeVisible();
	});
});

describe('Tile detail', () => {
	it('shows the detail line under the title', async () => {
		render(Tile, { section: sections[5], detail: 'Sin configurar' });
		await expect.element(page.getByText('Sin configurar')).toBeVisible();
	});
});
