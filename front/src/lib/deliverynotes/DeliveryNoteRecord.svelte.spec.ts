import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import DeliveryNoteHarness from '#lib/test/DeliveryNoteHarness.svelte';
import type { api } from '#lib/api/client.js';
import { ApiError } from '#lib/api/errors.js';
import { errorState } from '#lib/components/errors.svelte.js';
import type { components } from '../api/schema';
import { blankNote, emptyLine, noteValues, type LineValues } from '#lib/deliverynotes/form.js';
import { createLookups } from '#lib/deliverynotes/lookups.js';

type Note = components['schemas']['DeliveryNoteResponse'];
type Body = components['schemas']['DeliveryNoteRequest'];

const garcia = {
	id: 7,
	code: 3,
	name: 'Frutería García',
	nif: 'B12345674',
	address: 'Calle Mayor 1',
	postalCode: '46001',
	city: 'Valencia',
	vatType: 'WITH_VAT',
	surchargeType: 'WITH_SURCHARGE',
	defaultDeliveryNoteFormat: 'VALUED'
} as const;
/** Without NIF nor postal code: no invoice for it. */
const pepe = {
	id: 8,
	code: 4,
	name: 'Bar Pepe',
	address: 'Plaza Mayor 2',
	city: 'Valencia',
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

const pdf = new Blob(['%PDF-1.4'], { type: 'application/pdf' });

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
		hasProfile: true,
		issueInvoice: vi
			.fn<(clientId: number, noteId: number) => Promise<{ number: string }>>()
			.mockResolvedValue({ number: '23' }),
		fetchPdf: vi.fn<(noteId: number) => Promise<Blob>>().mockResolvedValue(pdf),
		print: vi.fn<(pdf: Blob) => Promise<void>>().mockResolvedValue(undefined),
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
const printBox = () => page.getByRole('checkbox', { name: /Imprimir albarán/ });
const issueBox = () => page.getByRole('checkbox', { name: /Emitir factura/ });
/** Where an element sits in the page, whatever the scroll. */
const pageTop = (el: Element) => el.getBoundingClientRect().top + window.scrollY;
/** Each mock was first called after the one before it. */
function inOrder(...mocks: { mock: { invocationCallOrder: number[] } }[]) {
	const order = mocks.map((m) => m.mock.invocationCallOrder[0]);
	expect(order).toEqual([...order].sort((a, b) => a - b));
}

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

	it('sit in the header, above the lines, and stay put as lines are added', async () => {
		await render(DeliveryNoteHarness, props({ initial: written() }));
		const box = totals().element();
		expect(
			box.compareDocumentPosition(cell(0, 'Código').element()) & Node.DOCUMENT_POSITION_FOLLOWING
		).toBeTruthy();
		const top = pageTop(box);
		await cell(1, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(2, 'Código')).toHaveFocus();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(2, 'Lote')).toHaveFocus();
		expect(pageTop(totals().element())).toBe(top);
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

	it('another client proposes its own last price where the price is still the proposed one', async () => {
		const p = props({
			proposePrice: vi.fn(async (clientId: number) => (clientId === garcia.id ? 1.2 : 1.5))
		});
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('3{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		await cliente().click();
		await userEvent.keyboard('4{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,50');
		expect(p.proposePrice).toHaveBeenLastCalledWith(pepe.id, 120);
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

describe('DeliveryNoteRecord, on saving', () => {
	// The error dialog is closed while its component is still there: one closing after the unmount
	// would dismiss the next test's error.
	afterEach(async () => {
		if (errorState.view === null) return;
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
	});

	it('both boxes start unticked; F9 and F3 tick and untick them like a click', async () => {
		await render(DeliveryNoteHarness, props({ initial: written() }));
		await expect.element(page.getByText('Al guardar:')).toBeVisible();
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(bar()).toHaveTextContent('F3 Emitir factura');
		await expect.element(bar()).toHaveTextContent('F9 Imprimir albarán');
		await expect.element(cliente()).toHaveFocus();

		await userEvent.keyboard('{F9}');
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'true');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		await userEvent.keyboard('{F3}');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'true');
		await userEvent.keyboard('{F9}');
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'false');

		await issueBox().click();
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		await printBox().click();
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'true');
		// Out of the Enter walk: the cursor stays where it was.
		await expect.element(cliente()).toHaveFocus();
	});

	it('an opened note starts with both unticked', async () => {
		await render(DeliveryNoteHarness, props(editing()));
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'false');
	});

	it('"Emitir factura" is greyed, with the reason, for a client without NIF nor postal code', async () => {
		await render(DeliveryNoteHarness, props({ initial: { ...written(), clientCode: 4 } }));
		await expect
			.element(page.getByText('Faltan datos del cliente para facturar: NIF y código postal'))
			.toBeVisible();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');
		await expect.element(bar()).not.toHaveTextContent('Emitir factura');
		await userEvent.keyboard('{F3}');
		await issueBox().click({ force: true });
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		// Printing needs none of it.
		await expect.element(printBox()).toHaveAttribute('aria-disabled', 'false');
	});

	it('"Emitir factura" is greyed without the owner\'s data', async () => {
		await render(DeliveryNoteHarness, props({ initial: written(), hasProfile: false }));
		await expect.element(page.getByText('Faltan los datos del titular (Ajustes)')).toBeVisible();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F3}');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
	});

	it('"Emitir factura" is greyed for a note already in an invoice', async () => {
		await render(
			DeliveryNoteHarness,
			props(editing({ ...saved, invoiceId: 3, invoiceStatus: 'DRAFT' }))
		);
		await expect.element(page.getByText('Ya está en una factura')).toBeVisible();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F3}');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
	});

	it('"Emitir factura" greyed out unticks: back to a client that allows it, it is unticked', async () => {
		await render(DeliveryNoteHarness, props({ initial: written() }));
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('{F3}');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'true');

		await cliente().fill('4');
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');

		await cliente().fill('3');
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByText('Frutería García')).toBeVisible();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
	});

	it('"Emitir factura" is greyed, with nothing to say, until there is a client', async () => {
		await render(DeliveryNoteHarness, props());
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');
		await expect.element(page.getByText(/^Falta/)).not.toBeInTheDocument();
	});

	it('a read-only note: both greyed, and neither the keys nor a click tick them', async () => {
		await render(DeliveryNoteHarness, props(editing(issued())));
		await expect.element(printBox()).toHaveAttribute('aria-disabled', 'true');
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'true');
		await expect.element(bar()).not.toHaveTextContent(/Imprimir|Emitir/);
		await userEvent.keyboard('{F9}{F3}');
		await printBox().click({ force: true });
		await issueBox().click({ force: true });
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
	});

	it('with both ticked: saves, issues, prints, in that order; the next note starts unticked', async () => {
		const p = props({ initial: written() });
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F9}{F3}{NumpadAdd}');

		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado · Factura 23 emitida')
		);
		expect(p.issueInvoice).toHaveBeenCalledExactlyOnceWith(7, 99);
		expect(p.fetchPdf).toHaveBeenCalledExactlyOnceWith(99);
		expect(p.print).toHaveBeenCalledExactlyOnceWith(pdf);
		inOrder(p.create, p.issueInvoice, p.fetchPdf, p.print, p.toast);

		// The next note of the chain, for a client that can be invoiced: still unticked.
		await expect.element(cliente()).toHaveValue('');
		await expect.element(cliente()).toHaveFocus();
		await userEvent.keyboard('3{Enter}');
		await expect.element(fecha()).toHaveFocus();
		await expect.element(issueBox()).toHaveAttribute('aria-disabled', 'false');
		await expect.element(issueBox()).toHaveAttribute('aria-checked', 'false');
		await expect.element(printBox()).toHaveAttribute('aria-checked', 'false');
		expect(p.goToList).not.toHaveBeenCalled();
	});

	it('unticked, saving neither issues nor prints', async () => {
		const p = props({ initial: written() });
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado')
		);
		expect(p.issueInvoice).not.toHaveBeenCalled();
		expect(p.fetchPdf).not.toHaveBeenCalled();
		expect(p.print).not.toHaveBeenCalled();
	});

	it('an edit that prints: prints, then back to the list', async () => {
		const p = props(editing());
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F9}{NumpadAdd}');
		await vi.waitFor(() => expect(p.goToList).toHaveBeenCalledExactlyOnceWith(99));
		expect(p.fetchPdf).toHaveBeenCalledExactlyOnceWith(99);
		inOrder(p.update, p.fetchPdf, p.print, p.toast, p.goToList);
		expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado');
		expect(p.issueInvoice).not.toHaveBeenCalled();
	});

	it('the invoice refused: the dialog says the note was saved and why, and the chain goes on', async () => {
		const p = props({ initial: written() });
		p.issueInvoice.mockRejectedValue(
			new ApiError(400, 'CLIENT_FISCAL_DATA_MISSING', 'missing', {})
		);
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F3}{F9}{NumpadAdd}');

		await expect
			.element(
				dialog().getByText(
					/^El albarán se ha guardado, pero no se ha podido emitir la factura: Al cliente le faltan datos para emitir la factura/
				)
			)
			.toBeVisible();
		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado')
		);
		// Printing goes on: the note is saved.
		expect(p.print).toHaveBeenCalledOnce();
		await expect.element(cliente()).toHaveValue('');
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(cliente()).toHaveFocus();
	});

	it('the PDF not coming: the dialog says it could not print', async () => {
		const p = props({ initial: written() });
		p.fetchPdf.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F9}{NumpadAdd}');
		await expect.element(dialog()).toHaveTextContent('No se ha podido imprimir el albarán');
		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado')
		);
		expect(p.print).not.toHaveBeenCalled();
	});

	it('issuing and printing both failing: one dialog says both', async () => {
		const p = props({ initial: written() });
		p.issueInvoice.mockRejectedValue(new ApiError(400, 'CLIENT_FISCAL_DATA_MISSING', 'x', {}));
		p.fetchPdf.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F3}{F9}{NumpadAdd}');

		await expect
			.element(dialog().getByText(/^El albarán se ha guardado, pero no se ha podido emitir/))
			.toBeVisible();
		await expect.element(dialog().getByText('No se ha podido imprimir el albarán.')).toBeVisible();
		await vi.waitFor(() =>
			expect(p.toast).toHaveBeenCalledExactlyOnceWith('Albarán 1045 guardado')
		);
		// One Enter and it is gone: no second dialog behind it.
		await userEvent.keyboard('{Enter}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(cliente()).toHaveFocus();
	});

	it('an unexpected error issuing still says the note was saved', async () => {
		const p = props({ initial: written() });
		p.issueInvoice.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		await render(DeliveryNoteHarness, p);
		await userEvent.keyboard('{F3}{NumpadAdd}');
		await expect
			.element(
				dialog().getByText(
					/^El albarán se ha guardado, pero no se ha podido emitir la factura: Ha ocurrido un error inesperado/
				)
			)
			.toBeVisible();
		await expect.element(dialog().getByText('Detalles técnicos')).toBeVisible();
	});
});
