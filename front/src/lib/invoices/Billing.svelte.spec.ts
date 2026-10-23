import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import BillingHarness from '#lib/test/BillingHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import { errorMessages } from '#lib/api/messages.js';
import { errorState } from '#lib/components/errors.svelte.js';
import type { PendingInvoice } from '#lib/invoices/billing.js';

const note = (id: number, code: number, date: string, total: number) => ({
	id,
	code,
	date: `${date}T09:00:00`,
	total
});

/** One note, no surcharge. */
const a: PendingInvoice = {
	clientId: 11,
	clientCode: 5,
	clientName: 'INDUS TIPICOS MALLORCA',
	month: '2026-10',
	vatPercentage: 4,
	surchargePercentage: 0,
	subtotal: 40.8,
	vatAmount: 1.63,
	surchargeAmount: 0,
	total: 42.43,
	clientDeleted: false,
	missingFiscalData: [],
	deliveryNotes: [note(101, 31801, '2026-10-05', 40.8)]
};
/** Four notes, with surcharge. */
const b: PendingInvoice = {
	clientId: 12,
	clientCode: 6,
	clientName: 'GRUPO FERRER GESTION SL',
	month: '2026-10',
	vatPercentage: 4,
	surchargePercentage: 0.5,
	subtotal: 1187.71,
	vatAmount: 47.51,
	surchargeAmount: 5.94,
	total: 1241.16,
	clientDeleted: false,
	missingFiscalData: [],
	deliveryNotes: [
		note(201, 31790, '2026-10-02', 310.2),
		note(202, 31795, '2026-10-09', 290),
		note(203, 31810, '2026-10-16', 300.51),
		note(204, 31822, '2026-10-23', 287)
	]
};
/** Cannot be issued: the client lacks fiscal data. */
const c: PendingInvoice = {
	clientId: 13,
	clientCode: 120,
	clientName: 'VARIOS',
	month: '2026-10',
	vatPercentage: 4,
	surchargePercentage: 0,
	subtotal: 665.13,
	vatAmount: 26.61,
	surchargeAmount: 0,
	total: 691.74,
	clientDeleted: false,
	missingFiscalData: ['nif', 'postalCode'],
	deliveryNotes: [note(301, 31799, '2026-10-07', 665.13)]
};

function props(overrides: Record<string, unknown> = {}) {
	return {
		today: '2026-10-23',
		hasProfile: true,
		loadPending: vi
			.fn<(dateTo: string) => Promise<PendingInvoice[]>>()
			.mockResolvedValue([a, b, c]),
		issue: vi.fn<(clientId: number, deliveryNoteIds: number[]) => Promise<{ number: string }>>(),
		toast: vi.fn<(message: string) => void>(),
		onExit: vi.fn<() => void>(),
		...overrides
	};
}

const dateField = () => page.getByLabelText('Facturar hasta');
const list = () => page.getByRole('table').first();
const listRow = (i: number) =>
	list()
		.getByRole('row')
		.nth(i + 1);
const cell = (row: number, column: number) => listRow(row).getByRole('cell').nth(column);
const panel = () => page.getByRole('region');
const dialog = () => page.getByRole('dialog');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

/** Opens the screen and loads today's list with Enter. */
async function loaded(p: ReturnType<typeof props>) {
	await render(BillingHarness, p);
	await expect.element(dateField()).toHaveFocus();
	await userEvent.keyboard('{Enter}');
	await expect.element(listRow(0)).toBeVisible();
}

function selection(): [number | null, number | null] {
	const input = dateField().element() as HTMLInputElement;
	return [input.selectionStart, input.selectionEnd];
}

// A desktop window, as the app's: two tables side by side do not fit the default phone width.
beforeEach(() => page.viewport(1280, 800));

/** A promise the test settles when it wants. */
function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

describe('Billing, the list', () => {
	it('opens in the date with today selected', async () => {
		const p = props();
		await render(BillingHarness, p);
		await expect.element(dateField()).toHaveFocus();
		await expect.element(dateField()).toHaveValue('23/10/2026');
		expect(selection()).toEqual([0, 10]);
		expect(p.loadPending).not.toHaveBeenCalled();
	});

	it('Enter loads and moves to the table', async () => {
		const p = props();
		await loaded(p);
		expect(p.loadPending).toHaveBeenCalledExactlyOnceWith('2026-10-23');
		await expect.element(dateField()).not.toHaveFocus();
		await expect.element(cell(0, 0)).toHaveTextContent('5');
		await expect.element(cell(0, 1)).toHaveTextContent('INDUS TIPICOS MALLORCA');
		await expect.element(cell(0, 2)).toHaveTextContent('10/2026');
		await expect.element(cell(0, 3)).toHaveTextContent('1');
		await expect.element(cell(0, 4)).toHaveTextContent('40,80 €');
		await expect.element(cell(0, 5)).toHaveTextContent('');
		await expect.element(cell(1, 3)).toHaveTextContent('4');
		await expect.element(cell(1, 4)).toHaveTextContent('1.187,71 €');
		await expect.element(cell(2, 5)).toHaveTextContent('Faltan datos');
		await expect
			.element(page.getByText(/^3 facturas · /))
			.toHaveTextContent('3 facturas · 1.893,64 €');
		// The arrows are the table's now.
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(listRow(1)).toHaveAttribute('aria-selected', 'true');

		await userEvent.keyboard('{Escape}');
		await expect.element(dateField()).toHaveFocus();
		await userEvent.keyboard('311026{Enter}');
		await vi.waitFor(() => expect(p.loadPending).toHaveBeenLastCalledWith('2026-10-31'));
	});

	it('the right panel follows the marked row', async () => {
		await loaded(props());
		await expect.element(panel()).toHaveTextContent('INDUS TIPICOS MALLORCA · octubre 2026');
		await expect
			.element(panel())
			.toHaveTextContent('Base 40,80 € · IVA 4 % 1,63 € · Total 42,43 €');
		await expect.element(panel()).not.toHaveTextContent('Recargo');

		await userEvent.keyboard('{ArrowDown}');
		await expect.element(panel()).toHaveTextContent('GRUPO FERRER GESTION SL · octubre 2026');
		await expect
			.element(panel())
			.toHaveTextContent(
				'Base 1.187,71 € · IVA 4 % 47,51 € · Recargo 0,5 % 5,94 € · Total 1.241,16 €'
			);
		const notes = panel().getByRole('row');
		await expect.element(notes.nth(1)).toHaveTextContent('02/10/2026');
		await expect.element(notes.nth(1)).toHaveTextContent('31790');
		await expect.element(notes.nth(1)).toHaveTextContent('310,20 €');
		await expect.element(notes.nth(4)).toHaveTextContent('31822');
		expect(notes.elements()).toHaveLength(5);
		await expect.element(panel()).not.toHaveTextContent('Faltan datos');

		await userEvent.keyboard('{ArrowDown}');
		await expect.element(panel()).toHaveTextContent('VARIOS · octubre 2026');
		await expect
			.element(panel())
			.toHaveTextContent(
				'Faltan datos del cliente para facturar: NIF y código postal (se corrigen en Clientes)'
			);
	});

	it('editing the date empties the list', async () => {
		const p = props();
		await loaded(p);
		await expect.element(button('Facturar todos')).toHaveAttribute('aria-disabled', 'false');
		await userEvent.keyboard('{Escape}3');
		await expect.element(list()).not.toBeInTheDocument();
		await expect.element(page.getByText(/facturas · /)).not.toBeInTheDocument();
		await expect.element(button('Facturar todos')).toHaveAttribute('aria-disabled', 'true');
		await expect.element(button('Facturar este')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{NumpadAdd}{F3}');
		await expect.element(dialog()).not.toBeInTheDocument();

		await userEvent.keyboard('11026{Enter}');
		await expect.element(listRow(0)).toBeVisible();
		expect(p.loadPending).toHaveBeenLastCalledWith('2026-10-31');
		await expect.element(button('Facturar todos')).toHaveAttribute('aria-disabled', 'false');
	});

	it('a list that comes after the date was edited is dropped', async () => {
		const late = deferred<PendingInvoice[]>();
		const p = props();
		p.loadPending.mockReturnValueOnce(late.promise);
		await render(BillingHarness, p);
		await userEvent.keyboard('{Enter}');
		await expect.element(dateField()).toHaveFocus();
		await userEvent.keyboard('3');
		late.resolve([a, b, c]);
		await new Promise((r) => setTimeout(r, 50));
		await expect.element(list()).not.toBeInTheDocument();
	});

	it('Esc goes back to the date, then out', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Escape}');
		await expect.element(dateField()).toHaveFocus();
		expect(selection()).toEqual([0, 10]);
		expect(p.onExit).not.toHaveBeenCalled();
		await userEvent.keyboard('{Escape}');
		expect(p.onExit).toHaveBeenCalledOnce();
	});

	it('empty list text', async () => {
		const p = props();
		p.loadPending.mockResolvedValue([]);
		await render(BillingHarness, p);
		await userEvent.keyboard('{Enter}');
		await expect
			.element(page.getByText('No hay albaranes pendientes hasta el 23/10/2026.'))
			.toBeVisible();
		await expect.element(page.getByText(/facturas · /)).not.toBeInTheDocument();
		await expect.element(button('Facturar todos')).toHaveAttribute('aria-disabled', 'true');
		await expect.element(button('Facturar este')).toHaveAttribute('aria-disabled', 'true');
	});

	it('without the owner data everything is greyed', async () => {
		const p = props({ hasProfile: false });
		await loaded(p);
		await expect.element(page.getByText('Faltan los datos del titular (Ajustes)')).toBeVisible();
		await expect.element(button('Facturar todos')).toHaveAttribute('aria-disabled', 'true');
		await expect.element(button('Facturar este')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{NumpadAdd}{F3}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.issue).not.toHaveBeenCalled();
	});

	it('with the owner data there is no notice', async () => {
		await loaded(props());
		await expect
			.element(page.getByText('Faltan los datos del titular (Ajustes)'))
			.not.toBeInTheDocument();
	});

	it('a long list scrolls inside its box: the count stays in sight', async () => {
		const many = Array.from({ length: 60 }, (_, i) => ({
			...a,
			clientId: 100 + i,
			clientCode: 100 + i
		}));
		await loaded(props({ loadPending: vi.fn().mockResolvedValue(many) }));
		await expect.element(page.getByText(/^60 facturas · /)).toBeInViewport({ ratio: 1 });
		expect(document.documentElement.scrollHeight).toBeLessThanOrEqual(window.innerHeight);
	});
});

describe('Billing, Facturar todos', () => {
	afterEach(() => {
		errorState.connectionLost = false;
		errorState.view = null;
	});

	it('+ asks before issuing', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('¿Emitir 2 facturas?');
		await expect.element(dialog()).toHaveTextContent('Con fecha de hoy, 23/10/2026.');
		await expect.element(dialog()).toHaveTextContent('Se deja sin facturar 1 fila con aviso.');
		await expect.element(dialog()).toHaveTextContent('+ Emitir');
		await expect.element(dialog()).toHaveTextContent('Enter No emitir');
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.issue).not.toHaveBeenCalled();

		// Esc says no as well, and stays on the screen.
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{Escape}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.issue).not.toHaveBeenCalled();
		expect(p.onExit).not.toHaveBeenCalled();
	});

	it('+ + issues every issuable row in order and lists what was left', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b, c]).mockResolvedValueOnce([c]);
		p.issue.mockResolvedValueOnce({ number: '101' }).mockResolvedValueOnce({ number: '102' });
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{NumpadAdd}');

		await expect.element(dialog()).toHaveTextContent('Facturas emitidas');
		expect(p.issue).toHaveBeenCalledTimes(2);
		expect(p.issue).toHaveBeenNthCalledWith(1, 11, [101]);
		expect(p.issue).toHaveBeenNthCalledWith(2, 12, [201, 202, 203, 204]);
		expect(p.loadPending).toHaveBeenCalledTimes(2);
		expect(p.loadPending).toHaveBeenLastCalledWith('2026-10-23');
		await expect.element(dialog()).toHaveTextContent('2 facturas emitidas: 101 a 102');
		await expect.element(dialog()).toHaveTextContent('No se han facturado:');
		await expect.element(dialog()).toHaveTextContent('VARIOS');
		await expect.element(dialog()).toHaveTextContent('10/2026');
		await expect
			.element(dialog())
			.toHaveTextContent(
				'Faltan datos del cliente para facturar: NIF y código postal (se corrigen en Clientes)'
			);
		expect(p.toast).not.toHaveBeenCalled();
		// The issued rows are gone.
		await expect.element(listRow(0)).toHaveTextContent('VARIOS');
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('with nothing left out, a toast says what was issued', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b]).mockResolvedValueOnce([]);
		p.issue.mockResolvedValueOnce({ number: '101' }).mockResolvedValueOnce({ number: '102' });
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('¿Emitir 2 facturas?');
		await expect.element(dialog()).not.toHaveTextContent('sin facturar');
		await userEvent.keyboard('{NumpadAdd}');

		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('2 facturas emitidas: 101 a 102')
		);
		await expect
			.element(page.getByText('No hay albaranes pendientes hasta el 23/10/2026.'))
			.toBeVisible();
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('a failing row does not stop the rest', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b]).mockResolvedValueOnce([a]);
		p.issue
			.mockRejectedValueOnce(new ApiError(409, 'INVOICE_MIXED_TAX_RATES', 'mixed', {}))
			.mockResolvedValueOnce({ number: '102' });
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');

		await expect.element(dialog()).toHaveTextContent('Facturas emitidas');
		expect(p.issue).toHaveBeenCalledTimes(2);
		expect(p.issue).toHaveBeenLastCalledWith(12, [201, 202, 203, 204]);
		await expect.element(dialog()).toHaveTextContent('Factura 102 emitida');
		await expect.element(dialog()).toHaveTextContent('INDUS TIPICOS MALLORCA');
		await expect.element(dialog()).toHaveTextContent(errorMessages.INVOICE_MIXED_TAX_RATES);
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('an unexpected error is named as such', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a]).mockResolvedValueOnce([a]);
		p.issue.mockRejectedValueOnce(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');

		await expect.element(dialog()).toHaveTextContent('No se ha emitido ninguna factura');
		await expect.element(dialog()).toHaveTextContent('INDUS TIPICOS MALLORCA');
		await expect.element(dialog()).toHaveTextContent('Error inesperado');
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('a lost connection stops the batch', async () => {
		const p = props();
		p.issue.mockRejectedValueOnce(new ApiError(0, 'NETWORK', '', {}));
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');

		await vi.waitFor(() => expect(errorState.connectionLost).toBe(true));
		expect(p.issue).toHaveBeenCalledOnce();
		// What is on screen may be issued already: the list waits for the next Enter.
		await expect.element(list()).not.toBeInTheDocument();
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.loadPending).toHaveBeenCalledOnce();
	});

	it('ignores keys while issuing, and counts only the rows it issues', async () => {
		const first = deferred<{ number: string }>();
		const second = deferred<{ number: string }>();
		const p = props();
		// A row left out comes first: the count skips it.
		p.loadPending.mockResolvedValueOnce([c, a, b]).mockResolvedValueOnce([c]);
		p.issue.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
		await loaded(p);
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('Emitiendo facturas…');
		await expect.element(dialog()).toHaveTextContent('1 de 2');
		await userEvent.keyboard('{NumpadAdd}{NumpadAdd}{F3}{F3}');
		first.resolve({ number: '101' });
		await expect.element(dialog()).toHaveTextContent('2 de 2');
		await userEvent.keyboard('{NumpadAdd}{F3}');
		second.resolve({ number: '102' });

		await expect.element(dialog()).toHaveTextContent('Facturas emitidas');
		expect(p.issue).toHaveBeenCalledTimes(2);
		expect(p.issue).toHaveBeenNthCalledWith(1, 11, [101]);
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});
});

describe('Billing, Facturar este', () => {
	it('F3 issues one row and keeps the cursor', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b, c]).mockResolvedValueOnce([a, c]);
		p.issue.mockResolvedValueOnce({ number: '23' });
		await loaded(p);
		await userEvent.keyboard('{ArrowDown}{F3}');
		await expect
			.element(dialog())
			.toHaveTextContent('¿Emitir la factura de GRUPO FERRER GESTION SL?');
		await expect.element(dialog()).toHaveTextContent('octubre 2026 · 4 albaranes · 1.241,16 €');
		await expect.element(dialog()).toHaveTextContent('F3 Emitir');
		await expect.element(dialog()).toHaveTextContent('Enter No emitir');
		await userEvent.keyboard('{F3}');

		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledExactlyOnceWith('Factura 23 emitida'));
		expect(p.issue).toHaveBeenCalledExactlyOnceWith(12, [201, 202, 203, 204]);
		await expect.element(listRow(1)).toHaveTextContent('VARIOS');
		await expect.element(listRow(1)).toHaveAttribute('aria-selected', 'true');
		expect(p.loadPending).toHaveBeenCalledTimes(2);
	});

	it('the cursor stays on the last row when fewer are left', async () => {
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b]).mockResolvedValueOnce([a]);
		p.issue.mockResolvedValueOnce({ number: '23' });
		await loaded(p);
		await userEvent.keyboard('{ArrowDown}{F3}{F3}');
		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledOnce());
		await expect.element(listRow(1)).not.toBeInTheDocument();
		await expect.element(listRow(0)).toHaveAttribute('aria-selected', 'true');
	});

	it('Enter says no', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{F3}');
		await expect.element(dialog()).toBeVisible();
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.issue).not.toHaveBeenCalled();
	});

	it('F3 on a row that cannot be issued does nothing', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{ArrowDown}{ArrowDown}');
		await expect.element(panel()).toHaveTextContent('VARIOS');
		await expect.element(button('Facturar este')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F3}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(p.issue).not.toHaveBeenCalled();
	});

	it('a refused invoice opens the error dialog with its reason', async () => {
		const p = props();
		p.issue.mockRejectedValueOnce(new ApiError(400, 'CLIENT_FISCAL_DATA_MISSING', 'x', {}));
		await loaded(p);
		await userEvent.keyboard('{F3}{F3}');
		await expect.element(dialog()).toHaveTextContent(errorMessages.CLIENT_FISCAL_DATA_MISSING);
		expect(p.toast).not.toHaveBeenCalled();
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('ignores keys while issuing', async () => {
		const first = deferred<{ number: string }>();
		const p = props();
		p.loadPending.mockResolvedValueOnce([a, b]).mockResolvedValueOnce([b]);
		p.issue.mockReturnValueOnce(first.promise);
		await loaded(p);
		await userEvent.keyboard('{F3}{F3}');
		await vi.waitFor(() => expect(p.issue).toHaveBeenCalledOnce());
		await userEvent.keyboard('{F3}{F3}{NumpadAdd}{NumpadAdd}');
		await expect.element(dialog()).not.toBeInTheDocument();
		first.resolve({ number: '101' });

		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledExactlyOnceWith('Factura 101 emitida'));
		expect(p.issue).toHaveBeenCalledOnce();
	});
});
