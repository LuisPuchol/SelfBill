import { beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ErrorHarness from '#lib/test/ErrorHarness.svelte';
import {
	dismissError,
	errorState,
	showError,
	showMessage,
	whenNoError
} from '#lib/components/errors.svelte.js';
import { ApiError, networkError } from '#lib/api/errors.js';
import { errorMessages } from '#lib/api/messages.js';

beforeEach(() => {
	errorState.view = null;
	errorState.connectionLost = false;
});

describe('ErrorDialog', () => {
	it('explains a business error in spanish', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'Detail EN', {}));
		await expect.element(page.getByRole('dialog')).toBeVisible();
		await expect.element(page.getByText('No se ha podido hacer')).toBeVisible();
		await expect.element(page.getByText(errorMessages.INVOICE_MIXED_MONTHS)).toBeVisible();
		expect(document.body.textContent).not.toContain('Detail EN');
	});

	it('closes with Enter', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'Detail EN', {}));
		await expect.element(page.getByRole('dialog')).toBeVisible();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
		expect(errorState.view).toBeNull();
	});

	it('hides technical details of unexpected errors', async () => {
		render(ErrorHarness);
		showError(new ApiError(500, 'INTERNAL_ERROR', 'Internal server error', {}));
		await expect.element(page.getByText(/Ha ocurrido un error inesperado/)).toBeVisible();
		await expect.element(page.getByText('Detalles técnicos')).toBeVisible();
		const detail = page.getByText(/Internal server error/);
		await expect.element(detail).toBeInTheDocument();
		await expect.element(detail).not.toBeVisible();
	});

	it('connection errors raise the connection screen flag', async () => {
		render(ErrorHarness);
		showError(networkError(new Error('Failed to fetch')));
		expect(errorState.connectionLost).toBe(true);
		expect(errorState.view).toBeNull();
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
	});

	it('a second error replaces the first', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}));
		showError(new ApiError(409, 'INVOICE_ALREADY_ISSUED', 'x', {}));
		await expect.element(page.getByText(errorMessages.INVOICE_ALREADY_ISSUED)).toBeVisible();
		dismissError();
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
	});

	it('losing the connection closes an open error dialog', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}));
		await expect.element(page.getByRole('dialog')).toBeVisible();
		showError(networkError(new Error('Failed to fetch')));
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
		expect(errorState.connectionLost).toBe(true);
		expect(errorState.view).toBeNull();
	});

	it('a lead goes before the spanish text of a business error', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}), { lead: 'Guardado, pero:' });
		await expect
			.element(page.getByText(`Guardado, pero: ${errorMessages.INVOICE_MIXED_MONTHS}`))
			.toBeVisible();
	});

	it('a lead goes before the text of an unexpected error too, details kept', async () => {
		render(ErrorHarness);
		showError(new ApiError(500, 'INTERNAL_ERROR', 'Internal server error', {}), {
			lead: 'Guardado, pero:'
		});
		await expect.element(page.getByRole('heading', { name: 'Error inesperado' })).toBeVisible();
		await expect
			.element(page.getByText(`Guardado, pero: ${errorMessages.INTERNAL_ERROR}`))
			.toBeVisible();
		await expect.element(page.getByText('Detalles técnicos')).toBeVisible();
	});

	it('another problem of the same go shows in the same dialog', async () => {
		render(ErrorHarness);
		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}), {
			lead: 'Guardado, pero:',
			also: 'Y tampoco se ha impreso.'
		});
		await expect
			.element(page.getByText(`Guardado, pero: ${errorMessages.INVOICE_MIXED_MONTHS}`))
			.toBeVisible();
		await expect.element(page.getByText('Y tampoco se ha impreso.')).toBeVisible();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
	});

	it("shows a text of the screen's own", async () => {
		render(ErrorHarness);
		showMessage('No se ha podido imprimir el albarán');
		await expect.element(page.getByText('No se ha podido hacer')).toBeVisible();
		await expect.element(page.getByText('No se ha podido imprimir el albarán')).toBeVisible();
	});

	it('whenNoError runs straight away without a dialog, else once it has closed', async () => {
		render(ErrorHarness);
		const now = vi.fn();
		whenNoError(now);
		expect(now).toHaveBeenCalledOnce();

		showError(new ApiError(409, 'INVOICE_MIXED_MONTHS', 'x', {}));
		await expect.element(page.getByRole('dialog')).toBeVisible();
		const dialogOpen: boolean[] = [];
		whenNoError(() => dialogOpen.push(document.querySelector('dialog')!.open));
		expect(dialogOpen).toEqual([]);
		await userEvent.keyboard('{Enter}');
		await vi.waitFor(() => expect(dialogOpen).toEqual([false]));
	});
});
