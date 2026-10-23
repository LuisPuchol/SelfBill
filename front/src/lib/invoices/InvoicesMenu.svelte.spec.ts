import { beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import InvoicesMenuHarness from '#lib/test/InvoicesMenuHarness.svelte';

const { goto } = vi.hoisted(() => ({ goto: vi.fn() }));
vi.mock('$app/navigation', () => ({ goto }));

const bar = () => page.getByRole('contentinfo');

describe('InvoicesMenu', () => {
	beforeEach(() => goto.mockClear());

	it('shows both tiles and the keys', async () => {
		await render(InvoicesMenuHarness);
		await expect
			.element(page.getByRole('link', { name: /Facturar/ }))
			.toHaveAttribute('href', '/invoices/billing');
		await expect
			.element(page.getByRole('link', { name: /Libro de facturas/ }))
			.toHaveAttribute('href', '/invoices/ledger');
		await expect.element(bar()).toHaveTextContent('1–2');
		await expect.element(bar()).toHaveTextContent('Abrir');
		await expect.element(bar()).toHaveTextContent('Volver');
	});

	it('opens Facturar with 1 and the ledger with 2', async () => {
		await render(InvoicesMenuHarness);
		await expect.element(bar()).toHaveTextContent('1–2');
		await userEvent.keyboard('1');
		expect(goto).toHaveBeenLastCalledWith('/invoices/billing');
		await userEvent.keyboard('2');
		expect(goto).toHaveBeenLastCalledWith('/invoices/ledger');
	});

	it('goes back home with Esc', async () => {
		await render(InvoicesMenuHarness);
		await expect.element(bar()).toHaveTextContent('Volver');
		await userEvent.keyboard('{Escape}');
		expect(goto).toHaveBeenCalledExactlyOnceWith('/');
	});
});
