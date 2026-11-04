import { beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ConnectionHarness from '#lib/test/ConnectionHarness.svelte';
import { ApiError, networkError } from '#lib/api/errors.js';
import { errorState, showError, whenNoError } from '#lib/components/errors.svelte.js';

beforeEach(() => {
	errorState.view = null;
	errorState.connectionLost = true;
});

describe('ConnectionScreen', () => {
	it('retries on its own and closes when the api answers', async () => {
		const ping = vi.fn().mockResolvedValueOnce(false).mockResolvedValue(true);
		const onRestored = vi.fn();
		render(ConnectionHarness, { ping, onRestored, intervalMs: 50 });
		await expect.element(page.getByText('No se puede conectar con SelfBill')).toBeVisible();
		await vi.waitFor(() => expect(onRestored).toHaveBeenCalledOnce());
		expect(errorState.connectionLost).toBe(false);
	});

	it('Enter retries at once', async () => {
		const ping = vi.fn().mockResolvedValue(false);
		render(ConnectionHarness, { ping, onRestored: vi.fn(), intervalMs: 60_000 });
		await expect.element(page.getByText(/Reintentando/)).toBeVisible();
		expect(ping).not.toHaveBeenCalled();
		await userEvent.keyboard('{Enter}');
		await vi.waitFor(() => expect(ping).toHaveBeenCalledOnce());
	});

	it('Esc goes back when onBack is given', async () => {
		const onBack = vi.fn();
		render(ConnectionHarness, {
			ping: vi.fn().mockResolvedValue(false),
			onRestored: vi.fn(),
			intervalMs: 60_000,
			onBack
		});
		await expect.element(page.getByRole('contentinfo')).toHaveTextContent('Volver');
		await userEvent.keyboard('{Escape}');
		expect(onBack).toHaveBeenCalledOnce();
	});

	it('shows no Volver without onBack', async () => {
		render(ConnectionHarness, {
			ping: vi.fn().mockResolvedValue(false),
			onRestored: vi.fn(),
			intervalMs: 60_000
		});
		await expect.element(page.getByRole('contentinfo')).toHaveTextContent('Reintentar');
		await expect.element(page.getByRole('contentinfo')).not.toHaveTextContent('Volver');
	});

	it('whenNoError waits for the connection to come back', async () => {
		errorState.connectionLost = false;
		let up = false;
		const onRestored = vi.fn();
		render(ConnectionHarness, { ping: async () => up, onRestored, intervalMs: 50 });
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}));
		await expect.element(page.getByRole('dialog')).toBeVisible();
		const then = vi.fn();
		whenNoError(then);

		// The connection screen takes the dialog's place: what waited for it waits on.
		showError(networkError(new Error('Failed to fetch')));
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
		await new Promise((r) => setTimeout(r, 200));
		expect(then).not.toHaveBeenCalled();

		up = true;
		await vi.waitFor(() => expect(onRestored).toHaveBeenCalledOnce());
		await vi.waitFor(() => expect(then).toHaveBeenCalledOnce());
		expect(errorState.connectionLost).toBe(false);
	});

	it('a hanging ping does not block later retries', async () => {
		const ping = vi
			.fn()
			.mockImplementationOnce(() => new Promise<boolean>(() => {}))
			.mockResolvedValue(true);
		const onRestored = vi.fn();
		render(ConnectionHarness, { ping, onRestored, intervalMs: 50 });
		await new Promise((r) => setTimeout(r, 300));
		expect(ping).toHaveBeenCalledOnce();
		expect(onRestored).not.toHaveBeenCalled();
	});
});
