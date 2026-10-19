import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import DeliveryNoteHarness from '#lib/test/DeliveryNoteHarness.svelte';
import type { api } from '#lib/api/client.js';
import { ApiError } from '#lib/api/errors.js';
import type { components } from '../api/schema';
import { blankNote, emptyLine, noteValues, type LineValues } from '#lib/deliverynotes/form.js';
import { createLookups } from '#lib/deliverynotes/lookups.js';

type Note = components['schemas']['DeliveryNoteResponse'];
type Body = components['schemas']['DeliveryNoteRequest'];

const garcia = {
	id: 7,
	code: 3,
	name: 'Frutería García',
	vatType: 'WITH_VAT',
	surchargeType: 'WITH_SURCHARGE',
	defaultDeliveryNoteFormat: 'VALUED'
} as const;
const pepe = {
	id: 8,
	code: 4,
	name: 'Bar Pepe',
	vatType: 'WITH_VAT',
	surchargeType: 'WITHOUT_SURCHARGE',
	defaultDeliveryNoteFormat: 'PLAIN'
} as const;
/** García's regime, another client. */
const ana = {
	id: 9,
	code: 5,
	name: 'Frutas Ana',
	vatType: 'WITH_VAT',
	surchargeType: 'WITH_SURCHARGE',
	defaultDeliveryNoteFormat: 'VALUED'
} as const;
const period = { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 };

/** The real cache, seeded with the catalog; any other code is a 404. */
function lookups() {
	const GET = vi.fn(async () => ({ error: {}, response: new Response(null, { status: 404 }) }));
	const found = createLookups({ GET } as unknown as typeof api);
	found.seed({
		clients: [garcia, pepe, ana],
		articles: [
			{ id: 120, code: 12, name: 'Tomate' },
			{ id: 150, code: 15, name: 'Pepino' }
		]
	});
	return found;
}

function props(overrides: Record<string, unknown> = {}) {
	return {
		id: null as number | null,
		initial: blankNote('2026-10-18'),
		periods: [period],
		lookups: lookups(),
		create: vi.fn<(body: Body) => Promise<Note>>().mockResolvedValue({ id: 99, code: 1045 }),
		update: vi
			.fn<(id: number, body: Body) => Promise<Note>>()
			.mockResolvedValue({ id: 99, code: 1045 }),
		remove: vi.fn<(id: number) => Promise<void>>().mockResolvedValue(undefined),
		proposePrice: vi
			.fn<(clientId: number, articleId: number) => Promise<number | null>>()
			.mockResolvedValue(null),
		goToList: vi.fn<(savedId?: number) => void>(),
		toast: vi.fn<(message: string) => void>(),
		...overrides
	};
}

function line(articleCode: number, lot1: string, quantity: number, price: number): LineValues {
	return { articleCode, lot1, lot2: '', lot3: '', quantity, price };
}

const tomate = () => line(12, '01181026', 25, 1.2);
const pepino = () => line(15, '02181026', 3, 0.33);
/** A new note for García with the two lines of the totals example. */
const written = () => ({ ...blankNote('2026-10-18'), clientCode: 3, lines: [tomate(), pepino()] });

/** Note 1045, pending, for García, with the same two lines. */
const saved: Note = {
	id: 99,
	code: 1045,
	date: '2026-10-17T08:30:00',
	clientId: 7,
	clientName: 'Frutería García',
	format: 'VALUED',
	vatPercentage: 4,
	surchargePercentage: 0.5,
	lines: [
		{
			articleId: 120,
			articleName: 'Tomate',
			traceabilityCode1: '01181026',
			quantity: 25,
			price: 1.2
		},
		{
			articleId: 150,
			articleName: 'Pepino',
			traceabilityCode1: '02181026',
			quantity: 3,
			price: 0.33
		}
	]
};

/** Saved at the rates of an older period, and in the plain format García no longer has. */
const frozen = (): Note => ({
	...saved,
	vatPercentage: 10,
	surchargePercentage: 1.4,
	format: 'PLAIN'
});
const issued = (): Note => ({
	...saved,
	invoiceId: 3,
	invoiceStatus: 'ISSUED',
	invoiceNumber: '23'
});

/** What the [id] page passes for a saved note. */
function editing(note: Note = saved) {
	const codes = { client: 3, article: (articleId: number) => articleId / 10 };
	return { id: note.id, saved: note, initial: noteValues(note, codes) };
}

/** The same, once its client and articles have been deleted (404 by id): no codes. */
function orphan(note: Note = saved) {
	const codes = { client: null, article: () => null };
	return { id: note.id, saved: note, initial: noteValues(note, codes) };
}

const sentLine = (articleId: number, lot: string, quantity: number, price: number) => ({
	articleId,
	traceabilityCode1: lot,
	traceabilityCode2: null,
	traceabilityCode3: null,
	quantity,
	price
});

const cliente = () => page.getByLabelText('Cliente', { exact: true });
const fecha = () => page.getByLabelText('Fecha', { exact: true });
type Column = 'Código' | 'Lote' | 'Cantidad' | 'Precio';
const cell = (row: number, column: Column) => page.getByLabelText(column, { exact: true }).nth(row);
const totals = () => page.getByRole('region', { name: 'Totales' });
const bar = () => page.getByRole('contentinfo');
const dialog = () => page.getByRole('dialog');
const dirty = () => page.getByTestId('dirty');

describe('DeliveryNoteRecord, header', () => {
	it("finds the client by code and shows its trade name and its note's format", async () => {
		await render(DeliveryNoteHarness, props());
		await expect
			.element(page.getByRole('heading', { level: 1, name: 'Albarán nuevo' }))
			.toBeVisible();
		await expect.element(cliente()).toHaveFocus();
		await expect.element(fecha()).toHaveValue('18/10/2026');
		await userEvent.keyboard('3{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await expect.element(page.getByText('Frutería García')).toBeVisible();
		await expect.element(page.getByText('Albarán valorado')).toBeVisible();
		await cliente().click();
		await userEvent.keyboard('4{Enter}');
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
		await expect.element(page.getByText('Albarán simple')).toBeVisible();
	});

	it('an unknown client code keeps the cursor', async () => {
		await render(DeliveryNoteHarness, props());
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('9{Enter}');
		await expect.element(page.getByText('No existe ningún cliente con ese código')).toBeVisible();
		await expect.element(cliente()).toHaveFocus();
	});
});

describe('DeliveryNoteRecord, totals', () => {
	it("adds the period's VAT and surcharge with the client's regime", async () => {
		await render(DeliveryNoteHarness, props({ initial: written() }));
		await expect.element(totals().getByText('30,99 €')).toBeVisible();
		await expect.element(totals().getByText('IVA (4 %)')).toBeVisible();
		await expect.element(totals().getByText('1,24 €')).toBeVisible();
		await expect.element(totals().getByText('Recargo (0,5 %)')).toBeVisible();
		await expect.element(totals().getByText('0,15 €')).toBeVisible();
		await expect.element(totals().getByText('32,38 €')).toBeVisible();
	});

	it('says so when no VAT period covers the date', async () => {
		await render(DeliveryNoteHarness, props({ initial: written(), periods: [] }));
		await expect.element(totals().getByText('30,99 €')).toBeVisible();
		await expect.element(totals().getByText('No hay IVA configurado para esa fecha')).toBeVisible();
		await expect.element(totals().getByText(/IVA \(/)).not.toBeInTheDocument();
	});

	it('a saved note keeps its frozen rates and format when only its date changes', async () => {
		// Not even a period is needed: nothing is reapplied.
		await render(DeliveryNoteHarness, props({ ...editing(frozen()), periods: [] }));
		await expect.element(totals().getByText('IVA (10 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (1,4 %)')).toBeVisible();
		await expect.element(page.getByText('Albarán simple')).toBeVisible();

		await fecha().fill('010226');
		await expect.element(totals().getByText('IVA (10 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (1,4 %)')).toBeVisible();
		await expect
			.element(totals().getByText('No hay IVA configurado para esa fecha'))
			.not.toBeInTheDocument();
		await expect.element(page.getByText('Albarán simple')).toBeVisible();
	});

	it('another client with the same regime keeps both frozen rates, with its own format', async () => {
		await render(DeliveryNoteHarness, props(editing(frozen())));
		// The cursor starts in Cliente, its code selected.
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('5{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await expect.element(page.getByText('Frutas Ana')).toBeVisible();
		await expect.element(page.getByText('Albarán valorado')).toBeVisible();
		await expect.element(totals().getByText('IVA (10 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (1,4 %)')).toBeVisible();
	});

	it('a client without surcharge reapplies only the surcharge', async () => {
		await render(DeliveryNoteHarness, props(editing(frozen())));
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('4{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
		await expect.element(totals().getByText('IVA (10 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (0 %)')).toBeVisible();
	});

	it('an issued note always shows its frozen rates', async () => {
		await render(DeliveryNoteHarness, props({ ...editing(issued()), periods: [] }));
		await expect.element(totals().getByText('IVA (4 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (0,5 %)')).toBeVisible();
	});
});

describe('DeliveryNoteRecord, client and articles deleted since', () => {
	it('a pending note shows their names, and needs new codes to be saved', async () => {
		const p = props(orphan(frozen()));
		await render(DeliveryNoteHarness, p);
		await expect.element(page.getByText('Frutería García')).toBeVisible();
		await expect.element(page.getByRole('cell', { name: 'Tomate', exact: true })).toBeVisible();
		await expect.element(page.getByRole('cell', { name: 'Pepino', exact: true })).toBeVisible();
		await expect.element(cliente()).toHaveValue('');
		await expect.element(cell(0, 'Código')).toHaveValue('');
		await expect.element(totals().getByText('IVA (10 %)')).toBeVisible();
		await expect.element(dirty()).toHaveTextContent('false');

		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(cliente()).toHaveAccessibleDescription('Este dato es obligatorio');
		await expect.element(cell(0, 'Código')).toHaveAccessibleDescription('Este dato es obligatorio');
		expect(p.update).not.toHaveBeenCalled();

		// Another client: the name goes, and with the note's regime unknown both rates are reapplied.
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('4');
		await expect.element(page.getByText('Frutería García')).not.toBeInTheDocument();
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
		await expect.element(totals().getByText('IVA (4 %)')).toBeVisible();
		await expect.element(totals().getByText('Recargo (0 %)')).toBeVisible();
	});

	it('an issued note shows their names, and Enter walks through without asking for any', async () => {
		await render(DeliveryNoteHarness, props(orphan(issued())));
		await expect.element(page.getByText('Frutería García')).toBeVisible();
		await expect.element(page.getByRole('cell', { name: 'Tomate', exact: true })).toBeVisible();
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(0, 'Código')).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await expect.element(page.getByText('Este dato es obligatorio')).not.toBeInTheDocument();
	});
});

describe('DeliveryNoteRecord, saving', () => {
	it('a new note by keyboard saves and chains into a blank one with the same date', async () => {
		const p = props();
		await render(DeliveryNoteHarness, p);
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('3{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(0, 'Código')).toHaveFocus();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await userEvent.keyboard('01181026{Enter}25{Enter}1,2{Enter}');
		// Enter past the last price: a new line.
		await expect.element(cell(1, 'Código')).toHaveFocus();
		await userEvent.keyboard('15{Enter}');
		await expect.element(cell(1, 'Lote')).toHaveFocus();
		await userEvent.keyboard('02181026{Enter}3{Enter}0,33{NumpadAdd}');

		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado')
		);
		expect(p.create).toHaveBeenCalledExactlyOnceWith({
			clientId: 7,
			date: expect.stringMatching(/^2026-10-18T\d{2}:\d{2}:\d{2}$/),
			lines: [sentLine(120, '01181026', 25, 1.2), sentLine(150, '02181026', 3, 0.33)]
		});
		expect('code' in p.create.mock.calls[0][0]).toBe(false);
		await expect.element(cliente()).toHaveFocus();
		await expect.element(cliente()).toHaveValue('');
		await expect.element(page.getByText('Frutería García')).not.toBeInTheDocument();
		await expect.element(fecha()).toHaveValue('18/10/2026');
		await expect.element(cell(0, 'Código')).toHaveValue('');
		await expect.element(cell(1, 'Código')).not.toBeInTheDocument();
		await expect.element(dirty()).toHaveTextContent('false');
		expect(p.goToList).not.toHaveBeenCalled();
	});

	it('proposes the last price of the article to the client', async () => {
		const p = props({ proposePrice: vi.fn(async () => 1.2) });
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('3{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		expect(p.proposePrice).toHaveBeenCalledExactlyOnceWith(7, 120);
	});

	it('an edit sends the number and the original time, then goes back to the list', async () => {
		const p = props(editing());
		await render(DeliveryNoteHarness, p);
		await expect
			.element(page.getByRole('heading', { level: 1, name: 'Albarán 1045' }))
			.toBeVisible();
		await expect.element(page.getByText('Frutería García')).toBeVisible();
		await cell(1, 'Cantidad').click();
		await userEvent.keyboard('4{NumpadAdd}');
		await vi.waitFor(() => expect(p.goToList).toHaveBeenCalledExactlyOnceWith(99));
		expect(p.update).toHaveBeenCalledExactlyOnceWith(99, {
			code: 1045,
			clientId: 7,
			date: '2026-10-17T08:30:00',
			lines: [sentLine(120, '01181026', 25, 1.2), sentLine(150, '02181026', 4, 0.33)]
		});
		expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado');
	});

	it("the API's line errors show under the cell of the line they came from", async () => {
		const p = props({
			initial: { ...written(), lines: [tomate(), emptyLine(), pepino()] }
		});
		// The empty line is not sent: the API's lines[1] is the grid's third line.
		p.create.mockRejectedValue(
			new ApiError(400, 'VALIDATION_ERROR', '', { 'lines[1].quantity': 'too much' })
		);
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(cell(2, 'Cantidad')).toHaveAccessibleDescription('Revisa este dato');
		await expect.element(cell(2, 'Cantidad')).toHaveFocus();
	});

	it('a number already taken goes to the error dialog: a note has no number field', async () => {
		const p = props({ initial: written() });
		p.create.mockRejectedValue(new ApiError(409, 'DELIVERY_NOTE_DUPLICATE_CODE', 'taken', {}));
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('Ya existe un albarán con ese número.');
		expect(p.toast).not.toHaveBeenCalled();
	});

	it('an empty line added at the end is not a change', async () => {
		await render(DeliveryNoteHarness, props(editing()));
		await cell(1, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(2, 'Código')).toHaveFocus();
		await expect.element(dirty()).toHaveTextContent('false');
		await cell(0, 'Cantidad').click();
		await userEvent.keyboard('3');
		await expect.element(dirty()).toHaveTextContent('true');
	});
});

describe('DeliveryNoteRecord, invoice state', () => {
	it('in a draft invoice: a notice, and it can be changed but not deleted', async () => {
		const p = props(editing({ ...saved, invoiceId: 3, invoiceStatus: 'DRAFT' }));
		await render(DeliveryNoteHarness, p);
		await expect
			.element(
				page.getByText('Está en una factura en borrador: si lo cambias, la factura se recalcula.')
			)
			.toBeVisible();
		await expect.element(page.getByRole('button', { name: /Borrar/ })).toBeDisabled();
		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await cell(1, 'Cantidad').click();
		await userEvent.keyboard('4{NumpadAdd}');
		await vi.waitFor(() => expect(p.update).toHaveBeenCalledOnce());
	});

	it('in an issued invoice: a notice, and no key or field changes it', async () => {
		const p = props(editing(issued()));
		await render(DeliveryNoteHarness, p);
		await expect
			.element(page.getByText('Está en la factura nº 23, emitida: no se puede modificar.'))
			.toBeVisible();
		await expect.element(bar()).toHaveTextContent('Esc Volver');
		await expect.element(bar()).not.toHaveTextContent(/Guardar|Borrar|Lotes|Quitar línea/);
		await expect.element(cliente()).toHaveAttribute('readonly');
		await expect.element(fecha()).toHaveAttribute('readonly');
		await cell(1, 'Cantidad').click();
		await userEvent.keyboard('9{NumpadAdd}{F4}{F7}{F8}');
		await expect.element(cell(1, 'Cantidad')).toHaveValue('3,00');
		await expect.element(cell(1, 'Código')).toHaveValue('15');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(dirty()).toHaveTextContent('false');
		expect(p.update).not.toHaveBeenCalled();
		expect(p.remove).not.toHaveBeenCalled();
	});

	it('in an issued invoice, Enter after the last price goes back to Cliente', async () => {
		await render(DeliveryNoteHarness, props(editing(issued())));
		await cell(1, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cliente()).toHaveFocus();
		await expect.element(cell(2, 'Código')).not.toBeInTheDocument();
	});

	it('F8 on a pending note asks with the client name, then deletes it', async () => {
		const p = props(editing());
		await render(DeliveryNoteHarness, p);
		await expect.element(bar()).toHaveTextContent('F8 Borrar');
		await userEvent.keyboard('{F8}');
		await expect.element(dialog()).toHaveTextContent('¿Borrar el albarán 1045?');
		await expect.element(dialog()).toHaveTextContent('Frutería García');
		await userEvent.keyboard('{F8}');
		await vi.waitFor(() => expect(p.goToList).toHaveBeenCalledExactlyOnceWith());
		expect(p.remove).toHaveBeenCalledExactlyOnceWith(99);
		expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 borrado');
	});
});
