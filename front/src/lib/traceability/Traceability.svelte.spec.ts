import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import TraceabilityHarness from '#lib/test/TraceabilityHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import type { components } from '#lib/api/schema.js';
import { errorState } from '#lib/components/errors.svelte.js';

type Article = components['schemas']['TraceabilityArticleResponse'];
type Lot = components['schemas']['TraceabilityLotResponse'];
type Delivery = components['schemas']['TraceabilityDeliveryResponse'];

const acelgas: Article = { articleId: 1, articleCode: 100, articleName: 'ACELGAS' };
const espinaca: Article = { articleId: 2, articleCode: 101, articleName: 'ESPINACA BOLSA 250GR' };
const lotsOf: Record<number, Lot[]> = {
	1: [{ code: '01010526', quantity: 228 }],
	2: [
		{ code: '02200524', quantity: 151 },
		{ code: '03300524', quantity: 30 }
	]
};
const delivery = (note: number, quantity: number, client = 'GRUPO FERRER GESTION'): Delivery => ({
	deliveryNoteId: note,
	deliveryNoteCode: note,
	date: '2026-05-20T10:00:00',
	clientId: 9,
	clientCode: 9,
	clientName: client,
	quantity
});
const deliveriesOf = (articleId: number, code: string): Delivery[] =>
	articleId === 2 && code === '03300524'
		? [delivery(31700, 30, 'BAR PEPE')]
		: [delivery(31594, 10), delivery(31596, 20)];

function props(overrides: Record<string, unknown> = {}) {
	return {
		today: '2026-10-24',
		loadArticles: vi
			.fn<(from: string, to: string) => Promise<Article[]>>()
			.mockResolvedValue([acelgas, espinaca]),
		loadLots: vi
			.fn<(articleId: number, from: string, to: string) => Promise<Lot[]>>()
			.mockImplementation(async (id) => lotsOf[id]),
		loadDeliveries: vi
			.fn<(articleId: number, code: string, from: string, to: string) => Promise<Delivery[]>>()
			.mockImplementation(async (id, code) => deliveriesOf(id, code)),
		print: vi.fn<(doc: Blob) => Promise<void>>().mockResolvedValue(undefined),
		onExit: vi.fn<() => void>(),
		...overrides
	};
}

const fromField = () => page.getByLabelText('Desde');
const toField = () => page.getByLabelText('Hasta');
const articlesTable = () => page.getByRole('table').nth(0);
const lotsTable = () => page.getByRole('table').nth(1);
const notesTable = () => page.getByRole('table').nth(2);
const row = (table: ReturnType<typeof articlesTable>, i: number) =>
	table.getByRole('row').nth(i + 1);
const dialog = () => page.getByRole('dialog');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

async function loaded(p: ReturnType<typeof props>) {
	await render(TraceabilityHarness, p);
	await expect.element(row(notesTable(), 0)).toBeVisible();
}

const selection = (field: HTMLInputElement) => [field.selectionStart, field.selectionEnd];

// A desktop window, as the app's: three tables side by side do not fit the default phone width.
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

describe('Traceability', () => {
	it('loads the default period on entry', async () => {
		const p = props();
		await loaded(p);
		expect(p.loadArticles).toHaveBeenCalledExactlyOnceWith('2026-09-24', '2026-10-24');
		await expect.element(fromField()).toHaveFocus();
		expect(selection(fromField().element() as HTMLInputElement)).toEqual([0, 10]);
		await expect.element(row(articlesTable(), 0)).toHaveTextContent('100');
		await expect.element(row(articlesTable(), 0)).toHaveTextContent('ACELGAS');
		await expect.element(row(articlesTable(), 1)).toHaveTextContent('ESPINACA BOLSA 250GR');
		expect(p.loadLots).toHaveBeenCalledWith(1, '2026-09-24', '2026-10-24');
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('01010526');
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('228,00');
		expect(p.loadDeliveries).toHaveBeenCalledWith(1, '01010526', '2026-09-24', '2026-10-24');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('20/05/2026');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('GRUPO FERRER GESTION');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('31594');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('10,00');
		await expect.element(page.getByText(/albaranes ·/)).toHaveTextContent('2 albaranes · 30,00');
	});

	it('says 1 albarán in the singular', async () => {
		const p = props({
			loadDeliveries: vi.fn().mockResolvedValue([delivery(31594, 20)])
		});
		await loaded(p);
		await expect.element(page.getByText(/albarán ·/)).toHaveTextContent('1 albarán · 20,00');
	});

	it('Enter, Enter reloads and moves to the articles', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('01042026{Enter}');
		await expect.element(toField()).toHaveFocus();
		await userEvent.keyboard('300426{Enter}');
		await vi.waitFor(() =>
			expect(p.loadArticles).toHaveBeenLastCalledWith('2026-04-01', '2026-04-30')
		);
		await expect.element(row(articlesTable(), 0)).toBeVisible();
		await expect.element(toField()).not.toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(articlesTable(), 1)).toHaveAttribute('aria-selected', 'true');
	});

	it('an empty answer keeps the cursor in Hasta, selected', async () => {
		const p = props();
		await loaded(p);
		p.loadArticles.mockResolvedValue([]);
		await userEvent.keyboard('01042026{Enter}300426{Enter}');
		await expect
			.element(page.getByText('No se ha vendido nada entre el 01/04/2026 y el 30/04/2026.'))
			.toBeVisible();
		await expect.element(toField()).toHaveFocus();
		expect(selection(toField().element() as HTMLInputElement)).toEqual([0, 10]);
	});

	it('the columns follow the marked rows', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{ArrowDown}');
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('02200524');
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('151,00');
		await expect.element(row(lotsTable(), 1)).toHaveTextContent('03300524');
		expect(p.loadLots).toHaveBeenLastCalledWith(2, '2026-09-24', '2026-10-24');
		await userEvent.keyboard('{Enter}{ArrowDown}');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('BAR PEPE');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('30,00');
		expect(p.loadDeliveries).toHaveBeenLastCalledWith(2, '03300524', '2026-09-24', '2026-10-24');
		await expect.element(row(articlesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(lotsTable(), 1)).toHaveAttribute('aria-selected', 'true');
	});

	it('a late lots answer is not shown', async () => {
		const late = deferred<Lot[]>();
		const p = props();
		p.loadLots.mockReset();
		p.loadLots.mockReturnValueOnce(late.promise).mockResolvedValueOnce(lotsOf[2]);
		await render(TraceabilityHarness, p);
		await expect.element(row(articlesTable(), 1)).toBeVisible();
		await row(articlesTable(), 1).click();
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('02200524');
		late.resolve(lotsOf[1]);
		await new Promise((r) => setTimeout(r, 50));
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('02200524');
		await expect.element(lotsTable()).not.toHaveTextContent('01010526');
	});

	it('a late notes answer is not shown', async () => {
		const late = deferred<Delivery[]>();
		const p = props();
		p.loadDeliveries.mockReturnValueOnce(late.promise);
		await render(TraceabilityHarness, p);
		await expect.element(row(lotsTable(), 0)).toBeVisible();
		await row(articlesTable(), 1).click();
		await expect.element(row(lotsTable(), 1)).toBeVisible();
		await row(lotsTable(), 1).click();
		await expect.element(row(notesTable(), 0)).toHaveTextContent('BAR PEPE');
		late.resolve([delivery(31594, 10)]);
		await new Promise((r) => setTimeout(r, 50));
		await expect.element(row(notesTable(), 0)).toHaveTextContent('BAR PEPE');
		await expect.element(notesTable()).not.toHaveTextContent('31594');
	});

	it('a lots answer for the old period is not cached after a reload', async () => {
		const late = deferred<Lot[]>();
		const p = props();
		let first = true;
		p.loadLots.mockImplementation(async (id) => {
			if (id === 2 && first) {
				first = false;
				return late.promise;
			}
			return lotsOf[id];
		});
		await render(TraceabilityHarness, p);
		await expect.element(row(articlesTable(), 1)).toBeVisible();
		await row(articlesTable(), 1).click();
		await userEvent.keyboard('{Escape}');
		await userEvent.keyboard('01042026{Enter}300426{Enter}');
		await vi.waitFor(() =>
			expect(p.loadArticles).toHaveBeenLastCalledWith('2026-04-01', '2026-04-30')
		);
		await expect.element(toField()).not.toHaveFocus();
		late.resolve([{ code: '99999999', quantity: 1 }]);
		await new Promise((r) => setTimeout(r, 50));
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(lotsTable(), 0)).toHaveTextContent('02200524');
		expect(p.loadLots).toHaveBeenLastCalledWith(2, '2026-04-01', '2026-04-30');
		await expect.element(lotsTable()).not.toHaveTextContent('99999999');
	});

	it('a notes answer for the old period is not cached after a reload', async () => {
		const late = deferred<Delivery[]>();
		const p = props();
		let first = true;
		p.loadDeliveries.mockImplementation(async (id, code) => {
			if (id === 2 && first) {
				first = false;
				return late.promise;
			}
			return deliveriesOf(id, code);
		});
		await render(TraceabilityHarness, p);
		await expect.element(row(articlesTable(), 1)).toBeVisible();
		await row(articlesTable(), 1).click();
		await userEvent.keyboard('{Escape}');
		await userEvent.keyboard('01042026{Enter}300426{Enter}');
		await vi.waitFor(() =>
			expect(p.loadArticles).toHaveBeenLastCalledWith('2026-04-01', '2026-04-30')
		);
		await expect.element(toField()).not.toHaveFocus();
		late.resolve([delivery(11111, 1, 'TARDE')]);
		await new Promise((r) => setTimeout(r, 50));
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('31594');
		expect(p.loadDeliveries).toHaveBeenLastCalledWith(2, '02200524', '2026-04-01', '2026-04-30');
		await expect.element(notesTable()).not.toHaveTextContent('TARDE');
	});

	it('F9 waits for the notes of the marked lot', async () => {
		const pending = deferred<Delivery[]>();
		const p = props();
		p.loadDeliveries.mockReturnValueOnce(pending.promise);
		await render(TraceabilityHarness, p);
		await expect.element(row(lotsTable(), 0)).toBeVisible();
		await expect.element(button('Imprimir')).toHaveAttribute('aria-disabled', 'true');
		await row(lotsTable(), 0).click();
		await userEvent.keyboard('{F9}');
		expect(p.print).not.toHaveBeenCalled();
		pending.resolve([delivery(31594, 10)]);
		await expect.element(row(notesTable(), 0)).toBeVisible();
		await expect.element(button('Imprimir')).not.toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F9}');
		await vi.waitFor(() => expect(p.print).toHaveBeenCalledOnce());
	});

	it('Enter and Esc move between columns', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await userEvent.keyboard('{ArrowDown}{Enter}');
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(lotsTable(), 1)).toHaveAttribute('aria-selected', 'true');
		// The articles do not answer the arrows while the lots are active
		await userEvent.keyboard('{ArrowUp}{ArrowUp}');
		await expect.element(row(lotsTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(articlesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}');
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(row(articlesTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Escape}');
		await expect.element(fromField()).toHaveFocus();
		expect(selection(fromField().element() as HTMLInputElement)).toEqual([0, 10]);
		expect(p.onExit).not.toHaveBeenCalled();
		await userEvent.keyboard('{Escape}');
		expect(p.onExit).toHaveBeenCalledOnce();
	});

	it('Enter moves to the lots even before they arrive', async () => {
		const late = deferred<Lot[]>();
		const p = props();
		// The first request is for the list the Enters reload: the second is the one that stays pending
		p.loadLots.mockReturnValueOnce(new Promise(() => {})).mockReturnValueOnce(late.promise);
		// Each load is a new list, as a fetch gives
		p.loadArticles.mockImplementation(async () => [acelgas, espinaca]);
		await render(TraceabilityHarness, p);
		await expect.element(row(articlesTable(), 1)).toBeVisible();
		// Desde and Hasta, then the articles take the cursor with their lots still pending
		await userEvent.keyboard('{Enter}{Enter}');
		await expect.element(fromField()).not.toHaveFocus();
		await userEvent.keyboard('{Enter}');
		late.resolve(lotsOf[2]);
		await expect.element(row(lotsTable(), 1)).toBeVisible();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(lotsTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(articlesTable(), 0)).toHaveAttribute('aria-selected', 'true');
	});

	it('a click on a lot marks it and activates the lots', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{ArrowDown}');
		await expect.element(row(lotsTable(), 1)).toBeVisible();
		await row(lotsTable(), 1).click();
		await expect.element(row(lotsTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(row(lotsTable(), 0)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(articlesTable(), 1)).toHaveAttribute('aria-selected', 'true');
		await expect.element(row(notesTable(), 0)).toHaveTextContent('31594');
	});

	it('editing a date empties the columns and greys F9', async () => {
		const p = props();
		await loaded(p);
		await expect.element(button('Imprimir')).not.toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('01042026');
		await expect.element(page.getByRole('table')).not.toBeInTheDocument();
		await expect.element(button('Imprimir')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F9}');
		expect(p.print).not.toHaveBeenCalled();
		await userEvent.keyboard('{Enter}{Enter}');
		await expect.element(row(articlesTable(), 0)).toBeVisible();
		await expect.element(button('Imprimir')).not.toHaveAttribute('aria-disabled', 'true');
	});

	it('F9 prints the marked lot', async () => {
		const p = props();
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}{ArrowDown}{Enter}{F9}');
		await vi.waitFor(() => expect(p.print).toHaveBeenCalledOnce());
		const doc = p.print.mock.calls[0][0];
		expect(doc.type).toBe('text/html');
		const html = await doc.text();
		expect(html).toContain('Trazabilidad 02200524 ESPINACA BOLSA 250GR');
		expect(html).toContain('Total cantidad 30,00');
		expect(html).toContain('Del 24/09/2026 al 24/10/2026');
	});

	it('F9 reports a listing that does not print', async () => {
		const p = props();
		p.print.mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'boom', {}));
		// Each load is a new list, as a fetch gives
		p.loadArticles.mockImplementation(async () => [acelgas, espinaca]);
		await loaded(p);
		await userEvent.keyboard('{Enter}{Enter}');
		await expect.element(row(notesTable(), 0)).toBeVisible();
		await userEvent.keyboard('{F9}');
		await expect.element(dialog().getByText('No se ha podido imprimir el listado.')).toBeVisible();
	});

	it('F9 is greyed with no lot', async () => {
		const p = props({ loadArticles: vi.fn().mockResolvedValue([]) });
		await render(TraceabilityHarness, p);
		await expect.element(page.getByText(/No se ha vendido nada/)).toBeVisible();
		await expect.element(button('Imprimir')).toHaveAttribute('aria-disabled', 'true');
		await userEvent.keyboard('{F9}');
		expect(p.print).not.toHaveBeenCalled();
		await expect.element(dialog()).not.toBeInTheDocument();
	});
});
