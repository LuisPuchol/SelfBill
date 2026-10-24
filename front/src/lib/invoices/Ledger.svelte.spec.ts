import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import LedgerHarness from '#lib/test/LedgerHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import { errorMessages } from '#lib/api/messages.js';
import type { components } from '#lib/api/schema.js';
import { errorState } from '#lib/components/errors.svelte.js';
import { invoiceDeletion } from '#lib/invoices/deletion.js';

type Invoice = components['schemas']['InvoiceResponse'];

const first: Invoice = {
	id: 1,
	number: '3847',
	date: '2026-10-05T10:00:00',
	clientCode: 37,
	clientName: 'JULIO TUNDIDOR',
	subtotal: 732.23,
	vatAmount: 29.29,
	surchargeAmount: 0,
	total: 761.52
};
const second: Invoice = {
	id: 2,
	number: 'S-12',
	date: '2026-10-12T10:00:00',
	clientCode: 40,
	clientName: 'BAR PEPE',
	subtotal: 100,
	vatAmount: 0,
	surchargeAmount: 0,
	total: 100
};
const detailOf = (invoice: Invoice, code: number, subtotal: number): Invoice => ({
	...invoice,
	sections: [
		{
			deliveryNoteId: code,
			deliveryNoteCode: code,
			deliveryNoteDate: '2026-10-01T09:00:00',
			subtotal
		}
	]
});

function props(overrides: Record<string, unknown> = {}) {
	return {
		today: '2026-10-23',
		loadInvoices: vi
			.fn<(from: string, to: string) => Promise<{ invoices: Invoice[]; truncated: boolean }>>()
			.mockResolvedValue({ invoices: [first, second], truncated: false }),
		loadInvoice: vi
			.fn<(id: number) => Promise<Invoice>>()
			.mockImplementation(async (id) =>
				id === 1 ? detailOf(first, 31804, 40.8) : detailOf(second, 31900, 100)
			),
		fetchPdf: vi.fn<(id: number) => Promise<Blob>>().mockResolvedValue(new Blob(['pdf'])),
		print: vi.fn<(pdf: Blob) => Promise<void>>().mockResolvedValue(undefined),
		sendEmail: vi.fn<(id: number) => Promise<void>>().mockResolvedValue(undefined),
		deletion: {
			...invoiceDeletion,
			remove: vi.fn<(id: number) => Promise<void>>().mockResolvedValue(undefined)
		},
		toast: vi.fn<(message: string) => void>(),
		onExit: vi.fn<() => void>(),
		...overrides
	};
}

const fromField = () => page.getByLabelText('Desde');
const toField = () => page.getByLabelText('Hasta');
const list = () => page.getByRole('table').first();
const listRow = (i: number) =>
	list()
		.getByRole('row')
		.nth(i + 1);
const cell = (row: number, column: number) => listRow(row).getByRole('cell').nth(column);
const panel = () => page.getByRole('region');
const dialog = () => page.getByRole('dialog');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

async function loaded(p: ReturnType<typeof props>) {
	await render(LedgerHarness, p);
	await expect.element(listRow(0)).toBeVisible();
}

// A desktop window, as the app's: two tables side by side do not fit the default phone width.
beforeEach(() => page.viewport(1280, 800));

afterEach(() => {
	errorState.connectionLost = false;
	errorState.view = null;
});

function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

describe('Ledger', () => {
	it('an empty list keeps the cursor in the date, selected', async () => {
		const p = props({
			loadInvoices: vi.fn().mockResolvedValue({ invoices: [], truncated: false })
		});
		await render(LedgerHarness, p);
		await expect.element(page.getByText(/No hay facturas entre/)).toBeVisible();
		await toField().click();
		await userEvent.keyboard('{Enter}');
		await expect.element(toField()).toHaveFocus();
		const input = toField().element() as HTMLInputElement;
		expect([input.selectionStart, input.selectionEnd]).toEqual([0, 10]);
	});

	it('the panel shows no empty text while an invoice is loading', async () => {
		const pending = deferred<Invoice>();
		const p = props({ loadInvoice: vi.fn().mockReturnValue(pending.promise) });
		await loaded(p);
		await expect.element(panel()).toBeVisible();
		await expect.element(panel().getByText('No hay nada que mostrar.')).not.toBeInTheDocument();
		pending.resolve(detailOf(first, 31804, 40.8));
		await expect.element(panel().getByRole('table')).toBeVisible();
	});

	it('loads the default period on entry', async () => {
		const p = props();
		await loaded(p);
		expect(p.loadInvoices).toHaveBeenCalledExactlyOnceWith('2026-09-23', '2026-10-23');
		await expect.element(fromField()).toHaveFocus();
		const input = fromField().element() as HTMLInputElement;
		expect([input.selectionStart, input.selectionEnd]).toEqual([0, 10]);
		await expect.element(cell(0, 0)).toHaveTextContent('05/10/2026');
		await expect.element(cell(1, 1)).toHaveTextContent('S-12');
		await expect.element(cell(0, 2)).toHaveTextContent('37');
		await expect.element(cell(0, 3)).toHaveTextContent('JULIO TUNDIDOR');
		await expect.element(cell(0, 4)).toHaveTextContent('732,23 €');
		await expect.element(cell(0, 5)).toHaveTextContent('29,29 €');
		await expect.element(cell(0, 6)).toHaveTextContent('0,00 €');
		await expect.element(cell(0, 7)).toHaveTextContent('761,52 €');
		await expect
			.element(page.getByText(/^2 facturas · /))
			.toHaveTextContent(
				'2 facturas · Base 832,23 € · IVA 29,29 € · Recargo 0,00 € · Total 861,52 €'
			);
	});

	it('Enter, Enter reloads and moves to the table', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('01102026{Enter}');
		await expect.element(toField()).toHaveFocus();
		await userEvent.keyboard('311026{Enter}');
		await vi.waitFor(() =>
			expect(p.loadInvoices).toHaveBeenLastCalledWith('2026-10-01', '2026-10-31')
		);
		await expect.element(listRow(0)).toBeVisible();
		await expect.element(toField()).not.toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(listRow(1)).toHaveAttribute('aria-selected', 'true');
	});

	it('the panel follows the marked row', async () => {
		const p = props();
		await loaded(p);
		await expect.element(panel()).toHaveTextContent('Factura 3847 · 05/10/2026');
		await expect.element(panel()).toHaveTextContent('JULIO TUNDIDOR');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('01/10/2026');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('31804');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('40,80 €');
		await userEvent.keyboard('{Escape}');
		await userEvent.keyboard('{Enter}{Enter}');
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(panel()).toHaveTextContent('Factura S-12');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('31900');
		expect(p.loadInvoice).toHaveBeenLastCalledWith(2);
	});

	it('the panel ignores a late answer', async () => {
		const late = deferred<Invoice>();
		const p = props();
		p.loadInvoice.mockReset();
		p.loadInvoice
			.mockReturnValueOnce(late.promise)
			.mockResolvedValueOnce(detailOf(second, 31900, 100));
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{ArrowDown}');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('31900');
		late.resolve(detailOf(first, 31804, 40.8));
		await new Promise((r) => setTimeout(r, 50));
		await expect.element(panel()).toHaveTextContent('Factura S-12');
		await expect.element(panel().getByRole('row').nth(1)).toHaveTextContent('31900');
		await expect.element(panel()).not.toHaveTextContent('31804');
	});

	it('F9 prints', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F9}');
		await vi.waitFor(() => expect(p.print).toHaveBeenCalledOnce());
		expect(p.fetchPdf).toHaveBeenCalledWith(1);
		expect(p.print.mock.calls[0][0]).toBeInstanceOf(Blob);
	});

	it('F9 reports a PDF that does not come', async () => {
		const p = props();
		p.fetchPdf.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F9}');
		await expect.element(dialog().getByText('No se ha podido imprimir la factura.')).toBeVisible();
		expect(p.print).not.toHaveBeenCalled();
	});

	it('F4 sends by email', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F4}');
		await vi.waitFor(() => expect(p.sendEmail).toHaveBeenCalledWith(1));
		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledWith('Factura 3847 enviada por correo'));
	});

	it('F4 shows why it could not send', async () => {
		const p = props();
		p.sendEmail.mockRejectedValue(new ApiError(400, 'CLIENT_EMAIL_NOT_CONFIGURED', 'no email', {}));
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F4}');
		await expect.element(dialog()).toHaveTextContent(errorMessages.CLIENT_EMAIL_NOT_CONFIGURED);
		expect(p.toast).not.toHaveBeenCalled();
	});

	it('F8 deletes after confirming', async () => {
		const p = props();
		p.loadInvoices
			.mockResolvedValueOnce({ invoices: [first, second], truncated: false })
			.mockResolvedValueOnce({ invoices: [first, second], truncated: false })
			.mockResolvedValueOnce({ invoices: [first], truncated: false });
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await expect.element(toField()).not.toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(listRow(1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toHaveTextContent('¿Borrar la factura S-12?');
		await expect.element(dialog()).toHaveTextContent('BAR PEPE');
		await expect
			.element(dialog())
			.toHaveTextContent(
				'Sus albaranes vuelven a quedar pendientes de facturar. Su número no se vuelve a usar.'
			);
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.deletion.remove).not.toHaveBeenCalled();

		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{F8}');
		await vi.waitFor(() => expect(p.deletion.remove).toHaveBeenCalledWith(2));
		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledWith('Factura S-12 borrada'));
		await vi.waitFor(() => expect(p.loadInvoices).toHaveBeenCalledTimes(3));
		await expect.element(listRow(1)).not.toBeInTheDocument();
		await expect.element(listRow(0)).toHaveAttribute('aria-selected', 'true');
		await expect.element(panel()).toHaveTextContent('Factura 3847');
	});

	it('says when the list was cut', async () => {
		const p = props();
		p.loadInvoices.mockResolvedValue({ invoices: [first], truncated: true });
		await loaded(p);
		await expect
			.element(page.getByText('Solo se muestran las 2000 primeras: acorta las fechas'))
			.toBeVisible();
	});

	it('empty period: text, and the keys greyed', async () => {
		const p = props();
		p.loadInvoices.mockResolvedValue({ invoices: [], truncated: false });
		await render(LedgerHarness, p);
		await expect
			.element(page.getByText('No hay facturas entre el 23/09/2026 y el 23/10/2026.'))
			.toBeVisible();
		await expect.element(button('Imprimir')).toHaveAttribute('aria-disabled', 'true');
		await expect.element(button('Enviar por correo')).toHaveAttribute('aria-disabled', 'true');
		await expect.element(button('Borrar')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F9}{F4}{F8}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.fetchPdf).not.toHaveBeenCalled();
		expect(p.sendEmail).not.toHaveBeenCalled();
	});

	it('Esc goes back to Desde, then out', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await expect.element(fromField()).not.toHaveFocus();
		await userEvent.keyboard('{Escape}');
		await expect.element(fromField()).toHaveFocus();
		const input = fromField().element() as HTMLInputElement;
		expect([input.selectionStart, input.selectionEnd]).toEqual([0, 10]);
		expect(p.onExit).not.toHaveBeenCalled();
		await userEvent.keyboard('{Escape}');
		expect(p.onExit).toHaveBeenCalledOnce();
	});
});
