import { describe, expect, it, vi } from 'vitest';
import { page } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import CheckHarness from '#lib/test/CheckHarness.svelte';

const action = (run = vi.fn()) => ({ key: 'F9', label: 'Imprimir albarán', run });
const box = () => page.getByRole('checkbox', { name: /Imprimir albarán/ });

describe('CheckAction', () => {
	it('shows label, key and state', async () => {
		const { unmount } = await render(CheckHarness, { action: action(), checked: false });
		await expect.element(box()).toHaveAttribute('aria-checked', 'false');
		await expect.element(box()).toHaveTextContent('F9');
		unmount();
		await render(CheckHarness, { action: action(), checked: true });
		await expect.element(box()).toHaveAttribute('aria-checked', 'true');
	});

	it('a click runs the action, once for a double click', async () => {
		const run = vi.fn();
		await render(CheckHarness, { action: action(run), checked: false });
		await box().click();
		expect(run).toHaveBeenCalledOnce();
		await box().dblClick();
		expect(run).toHaveBeenCalledTimes(2);
	});

	it('greyed: click ignored, reason shown', async () => {
		const run = vi.fn();
		await render(CheckHarness, {
			action: action(run),
			checked: false,
			enabled: () => false,
			reason: 'Ya está en una factura'
		});
		await expect.element(box()).toHaveAttribute('aria-disabled', 'true');
		await box().click({ force: true });
		expect(run).not.toHaveBeenCalled();
		await expect.element(page.getByText('Ya está en una factura')).toBeVisible();
	});

	it('keeps the focus in the field and is out of the Tab order', async () => {
		const run = vi.fn();
		await render(CheckHarness, { action: action(run), checked: false });
		const field = page.getByLabelText('Campo');
		await field.click();
		await box().click();
		await expect.element(field).toHaveFocus();
		await expect.element(box()).toHaveAttribute('tabindex', '-1');
		expect(run).toHaveBeenCalledOnce();
	});
});
