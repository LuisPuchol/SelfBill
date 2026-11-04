import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ClientSummaryHarness from '#lib/test/ClientSummaryHarness.svelte';
import { ApiError, networkError } from '#lib/api/errors.js';
import type { components } from '#lib/api/schema.js';
import { errorState } from '#lib/components/errors.svelte.js';

type ClientTotals = components['schemas']['ClientSummaryResponse'];
type Invoice = components['schemas']['InvoiceResponse'];

const indus: ClientTotals = {
	clientId: 1,
	clientCode: 5,
	clientName: 'INDUS TIPICOS',
	invoiceCount: 2,
	subtotal: 300,
	vatAmount: 12,
	surchargeAmount: 0,
	total: 312,
	clientDeleted: false
};
const ferrer: ClientTotals = {
	clientId: 2,
	clientCode: 6,
	clientName: 'GRUPO FERRER',
	invoiceCount: 1,
	subtotal: 100,
	vatAmount: 4,
	surchargeAmount: 0,
	total: 104,
	clientDeleted: false
};
const invoice = (id: number, number: string, total: number, clientCode = 5): Invoice => ({
	id,
	number,
	date: '2026-10-05T10:00:00',
	clientCode,
	clientName: 'X',
	subtotal: total,
	vatAmount: 0,
	surchargeAmount: 0,
	total
});
const invoicesOf: Record<number, Invoice[]> = {
	1: [invoice(11, '3719', 100), invoice(12, '3720', 200)],
	2: [invoice(21, 'S-7', 104, 6)]
};
const detailOf = (id: number): Invoice => ({
	...invoice(id, String(id), 1),
	sections: [
		{
			deliveryNoteId: id * 10,
			deliveryNoteCode: id * 10,
			deliveryNoteDate: '2026-10-01T09:00:00',
			subtotal: 10.07
		},
		{
			deliveryNoteId: id * 10 + 1,
			deliveryNoteCode: id * 10 + 1,
			deliveryNoteDate: '2026-10-02T09:00:00',
			subtotal: 5
		}
	]
});

function props(overrides: Record<string, unknown> = {}) {
	return {
		today: '2026-10-27',
		// Each call answers a new array, as a fetch does
		loadSummary: vi
			.fn<(from: string, to: string) => Promise<ClientTotals[]>>()
			.mockImplementation(async () => [indus, ferrer]),
		loadInvoices: vi
			.fn<(clientId: number, from: string, to: string) => Promise<Invoice[]>>()
			.mockImplementation(async (id) => invoicesOf[id]),
		loadInvoice: vi
			.fn<(id: number) => Promise<Invoice>>()
			.mockImplementation(async (id) => detailOf(id)),
		fetchPdf: vi.fn<(id: number) => Promise<Blob>>().mockResolvedValue(new Blob(['pdf'])),
		print: vi.fn<(pdf: Blob) => Promise<void>>().mockResolvedValue(undefined),
		onExit: vi.fn<() => void>(),
		...overrides
	};
}

const fromField = () => page.getByLabelText('Desde');
const toField = () => page.getByLabelText('Hasta');
const clientsTable = () => page.getByRole('table').nth(0);
const invoicesTable = () => page.getByRole('table').nth(1);
const notesTable = () => page.getByRole('table').nth(2);
const row = (table: ReturnType<typeof clientsTable>, i: number) =>
	table.getByRole('row').nth(i + 1);
const dialog = () => page.getByRole('dialog');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

async function loaded(p: ReturnType<typeof props>) {
	await render(ClientSummaryHarness, p);
	await expect.element(row(notesTable(), 0)).toBeVisible();
}

const selection = (field: HTMLInputElement) => [field.selectionStart, field.selectionEnd];

// A desktop window, as the app's: the clients' seven columns do not fit the default phone width.
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

describe('ClientSummary', () => {
	it('loads the default period on entry', async () => {
		const p = props();
		await loaded(p);
		expect(p.loadSummary).toHaveBeenCalledExactlyOnceWith('2026-09-27', '2026-10-27');
		await expect.element(fromField()).toHaveFocus();
		expect(selection(fromField().element() as HTMLInputElement)).toEqual([0, 10]);
		await expect.element(row(clientsTable(), 0)).toHaveTextContent('INDUS TIPICOS');
		await expect.element(row(clientsTable(), 0)).toHaveTextContent('312,00');
		expect(p.loadInvoices).toHaveBeenCalledWith(1, '2026-09-27', '2026-10-27');
		await expect.element(row(invoicesTable(), 1)).toHaveTextContent('3720');
		expect(p.loadInvoice).toHaveBeenCalledWith(11);
		await expect.element(row(notesTable(), 0)).toHaveTextContent('10,07');
	});

	it('marks a deleted client in its row', async () => {
		const garcia: ClientTotals = { ...ferrer, clientName: 'García', clientDeleted: true };
		await loaded(props({ loadSummary: vi.fn().mockResolvedValue([indus, garcia]) }));
		await expect.element(row(clientsTable(), 0)).toHaveTextContent('INDUS TIPICOS');
		await expect.element(row(clientsTable(), 0)).not.toHaveTextContent('(borrado)');
		await expect.element(row(clientsTable(), 1)).toHaveTextContent('García (borrado)');
	});

	it('Enter, Enter loads and goes to the clients', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('01042026{Enter}');
		await expect.element(toField()).toHaveFocus();
		await userEvent.keyboard('300426{Enter}');
		await vi.waitFor(() =>
			expect(p.loadSummary).toHaveBeenLastCalledWith('2026-04-01', '2026-04-30')
		);
		await expect.element(row(clientsTable(), 0)).toBeVisible();
		await expect.element(toField()).not.toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(clientsTable(), 1)).toHaveAttribute('aria-selected', 'true');
	});

	it('the panels follow the marked rows', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{ArrowDown}');
		await expect.element(row(invoicesTable(), 0)).toHaveTextContent('S-7');
		expect(p.loadInvoices).toHaveBeenLastCalledWith(2, '2026-09-27', '2026-10-27');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('10,07');
		await userEvent.keyboard('{ArrowUp}{Enter}{ArrowDown}');
		await expect.element(row(invoicesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		expect(p.loadInvoice).toHaveBeenLastCalledWith(12);
		await expect.element(row(notesTable(), 0)).toHaveTextContent('120');
	});

	it('a late answer is not shown', async () => {
		const late = deferred<Invoice[]>();
		const p = props();
		p.loadInvoices.mockReturnValueOnce(late.promise).mockResolvedValueOnce(invoicesOf[2]);
		await render(ClientSummaryHarness, p);
		await expect.element(row(clientsTable(), 1)).toBeVisible();
		await row(clientsTable(), 1).click();
		await expect.element(row(invoicesTable(), 0)).toHaveTextContent('S-7');
		late.resolve(invoicesOf[1]);
		await new Promise((r) => setTimeout(r, 50));
		await expect.element(row(invoicesTable(), 0)).toHaveTextContent('S-7');
		await expect.element(invoicesTable()).not.toHaveTextContent('3719');
	});

	it('Enter and Esc move between columns', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await userEvent.keyboard('{Enter}{ArrowDown}');
		await expect.element(row(invoicesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		// The clients do not answer the arrows while the invoices are active
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(clientsTable(), 0)).toHaveAttribute('aria-selected', 'true');
		// Enter in the invoices: the notes, whose marks answer the arrows
		await userEvent.keyboard('{Enter}{ArrowDown}');
		await expect.element(row(notesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(invoicesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(row(notesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		// Enter does nothing in the notes
		await userEvent.keyboard('{Enter}{ArrowDown}');
		await expect.element(row(notesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}{ArrowUp}');
		await expect.element(row(invoicesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}{ArrowDown}');
		await expect.element(row(clientsTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}');
		await expect.element(fromField()).toHaveFocus();
		expect(selection(fromField().element() as HTMLInputElement)).toEqual([0, 10]);
		expect(p.onExit).not.toHaveBeenCalled();
		await userEvent.keyboard('{Escape}');
		expect(p.onExit).toHaveBeenCalledOnce();
	});

	it('the mark goes back to the first note with another invoice', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{Enter}{Enter}{ArrowDown}');
		await expect.element(row(notesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}{ArrowDown}');
		await expect.element(row(invoicesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('120');
		await expect.element(row(notesTable(), 0)).toHaveAttribute('aria-selected', 'true');
	});

	it('clicking an invoice row activates the invoices', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await row(invoicesTable(), 1).click();
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(row(invoicesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(clientsTable(), 0)).toHaveAttribute('aria-selected', 'true');
	});

	it('clicking a note row activates the notes', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await row(notesTable(), 1).click();
		await expect.element(row(notesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(row(notesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(invoicesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}{ArrowDown}');
		await expect.element(row(invoicesTable(), 1)).toHaveAttribute('aria-selected', 'true');
	});

	it('the summary failing leaves the list empty and says so', async () => {
		const p = props({
			loadSummary: vi.fn().mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}))
		});
		await render(ClientSummaryHarness, p);
		await expect.element(dialog()).toBeVisible();
		await expect.element(page.getByRole('table')).not.toBeInTheDocument();
	});

	it('new dates drop the old panels', async () => {
		const late = deferred<Invoice[]>();
		const p = props();
		let first = true;
		p.loadInvoices.mockImplementation(async (id) => {
			if (id === 2 && first) {
				first = false;
				return late.promise;
			}
			return invoicesOf[id];
		});
		await render(ClientSummaryHarness, p);
		await expect.element(row(clientsTable(), 1)).toBeVisible();
		await row(clientsTable(), 1).click();
		await userEvent.keyboard('{Escape}');
		await userEvent.keyboard('01042026');
		await expect.element(page.getByRole('table')).not.toBeInTheDocument();
		await expect.element(button('Imprimir factura')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F9}');
		expect(p.fetchPdf).not.toHaveBeenCalled();
		await userEvent.keyboard('{Enter}300426{Enter}');
		await vi.waitFor(() =>
			expect(p.loadSummary).toHaveBeenLastCalledWith('2026-04-01', '2026-04-30')
		);
		await expect.element(toField()).not.toHaveFocus();
		late.resolve([invoice(99, 'TARDE', 1, 6)]);
		await new Promise((r) => setTimeout(r, 50));
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(invoicesTable(), 0)).toHaveTextContent('S-7');
		await expect.element(invoicesTable()).not.toHaveTextContent('TARDE');
	});

	it('footers', async () => {
		const p = props();
		await loaded(p);
		await expect
			.element(page.getByText(/clientes ·/))
			.toHaveTextContent('2 clientes · 3 facturas · 416,00');
		await expect.element(page.getByText(/^2 facturas ·/)).toHaveTextContent('2 facturas · 300,00');
		await expect.element(page.getByText(/albaranes ·/)).toHaveTextContent('2 albaranes · 15,07');
	});

	it('footers in the singular', async () => {
		const p = props({
			loadSummary: vi.fn().mockImplementation(async () => [ferrer]),
			loadInvoice: vi.fn().mockImplementation(async (id: number) => ({
				...detailOf(id),
				sections: [{ deliveryNoteId: 1, deliveryNoteCode: 1, subtotal: 104 }]
			}))
		});
		await loaded(p);
		await expect
			.element(page.getByText(/cliente ·/))
			.toHaveTextContent('1 cliente · 1 factura · 104,00');
		await expect.element(page.getByText(/^1 factura ·/)).toHaveTextContent('1 factura · 104,00');
		await expect.element(page.getByText(/albarán ·/)).toHaveTextContent('1 albarán · 104,00');
	});

	it('footers sum in cents', async () => {
		const a: ClientTotals = { ...indus, invoiceCount: 1, total: 0.1 };
		const b: ClientTotals = { ...ferrer, invoiceCount: 1, total: 0.2 };
		const p = props({
			loadSummary: vi.fn().mockImplementation(async () => [a, b]),
			loadInvoices: vi
				.fn()
				.mockImplementation(async () => [invoice(1, '1', 0.1), invoice(2, '2', 0.2)]),
			loadInvoice: vi.fn().mockImplementation(async () => ({
				sections: [
					{ deliveryNoteId: 1, deliveryNoteCode: 1, subtotal: 0.1 },
					{ deliveryNoteId: 2, deliveryNoteCode: 2, subtotal: 0.2 }
				]
			}))
		});
		await loaded(p);
		await expect
			.element(page.getByText(/clientes ·/))
			.toHaveTextContent('2 clientes · 2 facturas · 0,30');
		await expect.element(page.getByText(/^2 facturas ·/)).toHaveTextContent('2 facturas · 0,30');
		await expect.element(page.getByText(/albaranes ·/)).toHaveTextContent('2 albaranes · 0,30');
	});

	it('an empty period keeps the cursor in Hasta, selected', async () => {
		const p = props();
		await loaded(p);
		p.loadSummary.mockImplementation(async () => []);
		await userEvent.keyboard('01042026{Enter}300426{Enter}');
		await expect
			.element(page.getByText('No hay facturas entre el 01/04/2026 y el 30/04/2026.'))
			.toBeVisible();
		await expect.element(toField()).toHaveFocus();
		expect(selection(toField().element() as HTMLInputElement)).toEqual([0, 10]);
		await expect.element(button('Imprimir factura')).toHaveAttribute('aria-disabled', 'true');
	});

	it('F9 prints the marked invoice', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{Enter}{ArrowDown}{F9}');
		await vi.waitFor(() => expect(p.fetchPdf).toHaveBeenCalledExactlyOnceWith(12));
		await vi.waitFor(() => expect(p.print).toHaveBeenCalledOnce());
		expect(p.print.mock.calls[0][0]).toBeInstanceOf(Blob);
	});

	it('F9 prints from the clients column too', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F9}');
		await vi.waitFor(() => expect(p.fetchPdf).toHaveBeenCalledExactlyOnceWith(11));
	});

	it('F9 prints the marked invoice from the notes column', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{Enter}{ArrowDown}{Enter}{F9}');
		await vi.waitFor(() => expect(p.fetchPdf).toHaveBeenCalledExactlyOnceWith(12));
	});

	it('F9 is disabled with no invoice marked', async () => {
		const p = props({ loadSummary: vi.fn().mockImplementation(async () => []) });
		await render(ClientSummaryHarness, p);
		await expect.element(page.getByText(/No hay facturas/)).toBeVisible();
		await userEvent.keyboard('{F9}');
		expect(p.fetchPdf).not.toHaveBeenCalled();
		expect(p.print).not.toHaveBeenCalled();
	});

	it('F9 reports an invoice that does not print', async () => {
		const p = props();
		p.print.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F9}');
		await expect.element(dialog().getByText('No se ha podido imprimir la factura.')).toBeVisible();
	});

	it('F9 with the connection lost shows the connection screen, not the print message', async () => {
		const p = props();
		p.fetchPdf.mockRejectedValue(networkError(new Error('Failed to fetch')));
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{F9}');
		await vi.waitFor(() => expect(errorState.connectionLost).toBe(true));
		expect(errorState.view).toBeNull();
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.print).not.toHaveBeenCalled();
	});
});
