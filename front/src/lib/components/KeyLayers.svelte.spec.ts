import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import NestHarness from '#lib/test/NestHarness.svelte';
import ModalHarness from '#lib/test/ModalHarness.svelte';

describe('key layers follow the component tree', () => {
	it('lets the child win over the parent when mounted together', async () => {
		const padre = vi.fn();
		const hijo = vi.fn();
		render(NestHarness, {
			parentActions: [{ key: 'Escape', label: 'Padre', run: padre }],
			childActions: [{ key: 'Escape', label: 'Hijo', run: hijo }]
		});
		await expect.element(page.getByText('Hijo')).toBeVisible();
		await expect.element(page.getByText('Padre')).not.toBeInTheDocument();
		await userEvent.keyboard('{Escape}');
		expect(hijo).toHaveBeenCalledOnce();
		expect(padre).not.toHaveBeenCalled();
	});

	it('lets the child win over the parent when mounted later', async () => {
		const padre = vi.fn();
		const hijo = vi.fn();
		render(NestHarness, {
			late: true,
			parentActions: [{ key: 'Escape', label: 'Padre', run: padre }],
			childActions: [{ key: 'Escape', label: 'Hijo', run: hijo }]
		});
		await expect.element(page.getByText('Padre')).toBeVisible();
		await page.getByRole('button', { name: 'Montar' }).click();
		await expect.element(page.getByText('Hijo')).toBeVisible();
		await expect.element(page.getByText('Padre')).not.toBeInTheDocument();
		await userEvent.keyboard('{Escape}');
		expect(hijo).toHaveBeenCalledOnce();
		expect(padre).not.toHaveBeenCalled();
	});

	it('keeps content inside a modal reachable and blocks the page below', async () => {
		const guardar = vi.fn();
		const seguir = vi.fn();
		render(ModalHarness, {
			pageActions: [{ key: 'NumpadAdd', label: 'Guardar', run: guardar }],
			contentActions: [{ key: 'Enter', label: 'Seguir editando', run: seguir }]
		});
		await expect.element(page.getByText('Seguir editando')).toBeVisible();
		await expect.element(page.getByText('Guardar')).not.toBeInTheDocument();
		await userEvent.keyboard('{NumpadAdd}');
		await userEvent.keyboard('{Enter}');
		expect(seguir).toHaveBeenCalledOnce();
		expect(guardar).not.toHaveBeenCalled();
	});
});
