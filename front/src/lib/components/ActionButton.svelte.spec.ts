import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ButtonHarness from '#lib/test/ButtonHarness.svelte';
import ButtonSwapHarness from '#lib/test/ButtonSwapHarness.svelte';

const guardar = (run = vi.fn()) => ({ key: 'NumpadAdd', label: 'Guardar', run });

describe('ActionButton', () => {
	it('shows the label and the key', async () => {
		await render(ButtonHarness, { action: guardar() });
		const button = page.getByRole('button', { name: /Guardar/ });
		await expect.element(button).toBeVisible();
		await expect.element(button).toHaveTextContent('+');
	});

	it('a click runs the action', async () => {
		const run = vi.fn();
		await render(ButtonHarness, { action: guardar(run) });
		await page.getByRole('button', { name: /Guardar/ }).click();
		expect(run).toHaveBeenCalledOnce();
	});

	it('a double click runs the action once', async () => {
		const run = vi.fn();
		await render(ButtonHarness, { action: guardar(run) });
		await page.getByRole('button', { name: /Guardar/ }).dblClick();
		expect(run).toHaveBeenCalledOnce();
	});

	it('the second press of a double click does not click what the first one brought', async () => {
		// "Nuevo" opens the record, whose "Guardar" is now under the pointer.
		const nuevo = vi.fn();
		const save = vi.fn();
		await render(ButtonSwapHarness, {
			first: { key: 'F2', label: 'Nuevo', run: nuevo },
			second: guardar(save)
		});
		await page.getByRole('button', { name: /Nuevo/ }).dblClick();
		await expect.element(page.getByRole('button', { name: /Guardar/ })).toBeVisible();
		expect(nuevo).toHaveBeenCalledOnce();
		expect(save).not.toHaveBeenCalled();
	});

	it('a click from the keyboard runs the action', async () => {
		// Enter on a focused button clicks it with no pointer: no click count.
		const run = vi.fn();
		await render(ButtonHarness, { action: guardar(run) });
		(page.getByRole('button', { name: /Guardar/ }).element() as HTMLElement).focus();
		await userEvent.keyboard('{Enter}');
		expect(run).toHaveBeenCalledOnce();
	});

	it('disabled when the action is not available, and a click does nothing', async () => {
		const run = vi.fn();
		const { unmount } = await render(ButtonHarness, {
			action: guardar(run),
			enabled: () => false
		});
		const button = page.getByRole('button', { name: /Guardar/ });
		await expect.element(button).toBeDisabled();
		await button.click({ force: true });
		unmount();

		await render(ButtonHarness, { action: { ...guardar(run), when: () => false } });
		const other = page.getByRole('button', { name: /Guardar/ });
		await expect.element(other).toBeDisabled();
		await other.click({ force: true });
		expect(run).not.toHaveBeenCalled();
	});

	it('the focus stays in the field', async () => {
		const run = vi.fn();
		await render(ButtonHarness, { action: guardar(run) });
		const field = page.getByLabelText('Campo');
		await field.click();
		await expect.element(field).toHaveFocus();
		await page.getByRole('button', { name: /Guardar/ }).click();
		await expect.element(field).toHaveFocus();
		expect(run).toHaveBeenCalledOnce();
	});

	it('a click on a greyed-out button leaves the cursor in the field', async () => {
		const run = vi.fn();
		await render(ButtonHarness, { action: guardar(run), enabled: () => false });
		const field = page.getByLabelText('Campo');
		await field.click();
		await expect.element(field).toHaveFocus();
		await page.getByRole('button', { name: /Guardar/ }).click({ force: true });
		await expect.element(field).toHaveFocus();
		expect(run).not.toHaveBeenCalled();
	});

	it('Tab skips it', async () => {
		await render(ButtonHarness, { action: guardar() });
		await expect
			.element(page.getByRole('button', { name: /Guardar/ }))
			.toHaveAttribute('tabindex', '-1');
	});

	it('variants', async () => {
		const { unmount } = await render(ButtonHarness, { action: guardar(), variant: 'primary' });
		await expect.element(page.getByRole('button', { name: /Guardar/ })).toHaveClass('bg-accent');
		unmount();
		await render(ButtonHarness, { action: guardar(), variant: 'danger' });
		await expect.element(page.getByRole('button', { name: /Guardar/ })).toHaveClass('text-danger');
	});
});
