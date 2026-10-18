import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import GuardHarness from '#lib/test/GuardHarness.svelte';

const first = () => page.getByLabelText('Primero');
const second = () => page.getByLabelText('Segundo');
const dialog = () => page.getByRole('dialog');

describe('unsaved changes question over a form', () => {
	it('"Guardar y salir" that fails puts the cursor on the first error', async () => {
		const onLeave = vi.fn();
		render(GuardHarness, { onLeave });
		await second().click();
		await userEvent.keyboard('algo{Escape}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(page.getByText('Este dato es obligatorio')).toBeVisible();
		await expect.element(first()).toHaveFocus();
		expect(onLeave).not.toHaveBeenCalled();
	});

	it('"Seguir editando" puts the cursor back where it was', async () => {
		render(GuardHarness, { onLeave: vi.fn() });
		await second().click();
		await userEvent.keyboard('algo{Escape}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(second()).toHaveFocus();
	});

	it('after a failed "Guardar y salir", "Seguir editando" goes to the last field with the cursor', async () => {
		render(GuardHarness, { onLeave: vi.fn() });
		await second().click();
		await userEvent.keyboard('{Escape}{NumpadAdd}');
		await expect.element(first()).toHaveFocus();
		await second().click();
		await userEvent.keyboard('{Escape}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(second()).toHaveFocus();
	});
});
