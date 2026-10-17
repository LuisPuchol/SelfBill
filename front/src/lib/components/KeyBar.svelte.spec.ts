import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import KeysHarness from '#lib/test/KeysHarness.svelte';

describe('KeyBar', () => {
	it('shows the keys of the current screen', async () => {
		render(KeysHarness, {
			actions: [
				{ key: 'Escape', label: 'Salir', run: vi.fn() },
				{ key: 'NumpadAdd', label: 'Guardar', run: vi.fn() }
			]
		});
		await expect.element(page.getByText('Esc', { exact: true })).toBeVisible();
		await expect.element(page.getByText('Salir')).toBeVisible();
		await expect.element(page.getByText('+', { exact: true })).toBeVisible();
		await expect.element(page.getByText('Guardar')).toBeVisible();
	});

	it('runs the action when its key is pressed', async () => {
		const salir = vi.fn();
		render(KeysHarness, { actions: [{ key: 'Escape', label: 'Salir', run: salir }] });
		await userEvent.keyboard('{Escape}');
		expect(salir).toHaveBeenCalledOnce();
	});

	it('replaces the bar while a modal layer is open', async () => {
		render(KeysHarness, {
			actions: [{ key: 'NumpadAdd', label: 'Guardar', run: vi.fn() }],
			modalActions: [{ key: 'Enter', label: 'Seguir editando', run: vi.fn() }]
		});
		await page.getByRole('button', { name: 'Abrir capa' }).click();
		await expect.element(page.getByText('Seguir editando')).toBeVisible();
		await expect.element(page.getByText('Guardar')).not.toBeInTheDocument();
	});
});
