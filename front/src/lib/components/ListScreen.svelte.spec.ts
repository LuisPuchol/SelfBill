import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ListHarness from '#lib/test/ListHarness.svelte';
import { ApiError } from '#lib/api/errors.js';
import { errorState } from '#lib/components/errors.svelte.js';

const rows = [
	{ id: 11, code: 1, name: 'Frutas Paco' },
	{ id: 12, code: 2, name: 'Bar Pepe' },
	{ id: 13, code: 3, name: 'Hotel Sol' }
];
const seven = { id: 17, code: 7, name: 'Fruta Siete' };
type Row = (typeof rows)[number];

function spies() {
	return { onSearch: vi.fn(), onOpen: vi.fn(), onNew: vi.fn(), onExit: vi.fn(), onPage: vi.fn() };
}

const pending = 'Tiene 1 albarán sin facturar. Si lo borras no se podrán editar ni facturar.';

/** What a list page passes to delete its rows. */
function deletion() {
	return {
		options: {
			entity: { one: 'cliente', Label: 'Cliente' },
			remove: vi.fn<(id: number) => Promise<void>>().mockResolvedValue(undefined),
			pendingForDelete: vi.fn<(id: number) => Promise<string | null>>().mockResolvedValue(pending)
		},
		describe: (r: Row) => r.name,
		code: (r: Row) => r.code,
		toast: vi.fn<(message: string) => void>(),
		reload: vi.fn<() => void>(),
		onDeleted: vi.fn<(id: number) => void>()
	};
}

const searchBox = () => page.getByLabelText('Buscar (código o nombre)');
const bar = () => page.getByRole('contentinfo');
const dialog = () => page.getByRole('dialog');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });
/** Data rows start at 1: the first row of the table is its header. */
const row = (n: number) => page.getByRole('row').nth(n);
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
/**
 * The search's pause on a fake clock, moved with `vi.advanceTimersByTime`: what a test proves no
 * longer depends on how long each key takes to arrive. Only the timers the pause uses: the keys go
 * through Playwright, and the runner keeps real timers for itself.
 */
const fakePause = () => vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
/** Calls to `spy` made after the first call to `first`: on a slow run the search may go out before. */
const callsAfter = (spy: Mock, first: Mock) =>
	spy.mock.invocationCallOrder.filter((n) => n > first.mock.invocationCallOrder[0]).length;

beforeEach(() => {
	errorState.view = null;
});

afterEach(() => {
	vi.useRealTimers();
});

describe('ListScreen', () => {
	it('the search box has the cursor on entry', async () => {
		await render(ListHarness, spies());
		await expect.element(page.getByRole('heading', { level: 1, name: 'Clientes' })).toBeVisible();
		await expect.element(searchBox()).toHaveFocus();
		await expect.element(searchBox()).toHaveAttribute('autocomplete', 'off');
	});

	it('searches after a pause', async () => {
		const s = spies();
		await render(ListHarness, s);
		fakePause();
		// Each key restarts the wait: 400 ms in all, but never 300 ms without typing.
		await userEvent.keyboard('g');
		vi.advanceTimersByTime(200);
		await userEvent.keyboard('a');
		vi.advanceTimersByTime(200);
		await userEvent.keyboard('r');
		expect(s.onSearch).not.toHaveBeenCalled();
		vi.advanceTimersByTime(299);
		expect(s.onSearch).not.toHaveBeenCalled();
		vi.advanceTimersByTime(1);
		expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('gar');
		vi.advanceTimersByTime(1000);
		expect(s.onSearch).toHaveBeenCalledOnce();
	});

	it('Enter in the search box opens the selected row', async () => {
		const s = spies();
		await render(ListHarness, s);
		await expect.element(searchBox()).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[0]);
	});

	it('arrows move the selection from the search box', async () => {
		const s = spies();
		await render(ListHarness, s);
		await expect.element(searchBox()).toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');
		await expect.element(searchBox()).toHaveFocus();
		await userEvent.keyboard('{Enter}');
		expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
		await userEvent.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{ArrowUp}{Enter}');
		expect(s.onOpen).toHaveBeenLastCalledWith(rows[1]);
	});

	it('Esc clears the search first, then exits', async () => {
		const s = spies();
		await render(ListHarness, s);
		await userEvent.keyboard('gar{Escape}');
		await expect.element(searchBox()).toHaveValue('');
		expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('');
		expect(s.onExit).not.toHaveBeenCalled();
		// The search that was waiting for the pause is dropped.
		await wait(400);
		expect(s.onSearch).toHaveBeenCalledOnce();
		await userEvent.keyboard('{Escape}');
		expect(s.onExit).toHaveBeenCalledOnce();
		expect(s.onSearch).toHaveBeenCalledOnce();
	});

	it('F2 asks for a new record', async () => {
		const s = spies();
		await render(ListHarness, s);
		await expect.element(bar()).toHaveTextContent('F2 Nuevo');
		await userEvent.keyboard('{F2}');
		expect(s.onNew).toHaveBeenCalledOnce();
	});

	it('says why the list is empty', async () => {
		const s = spies();
		const { rerender } = await render(ListHarness, { ...s, rows: [] });
		await expect
			.element(page.getByText('Todavía no hay clientes. Pulsa F2 para dar de alta el primero.'))
			.toBeVisible();
		await rerender({ search: '7' });
		await expect.element(page.getByText('No existe ningún cliente con el código 7.')).toBeVisible();
		await userEvent.keyboard('{Enter}');
		expect(s.onOpen).not.toHaveBeenCalled();
		await rerender({ search: 'garcía' });
		await expect
			.element(page.getByText('No hay ningún cliente que coincida con «garcía».'))
			.toBeVisible();
	});

	it('selects the remembered row', async () => {
		const s = spies();
		const { rerender } = await render(ListHarness, { ...s, selectId: 13 });
		await expect.element(row(3)).toHaveAttribute('aria-selected', 'true');
		// Also when the rows change, if it is still on the page.
		await rerender({ rows: [rows[0], rows[2]] });
		await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Enter}');
		expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[2]);
	});

	it('names Esc after what it will do', async () => {
		await render(ListHarness, spies());
		await expect.element(bar()).toHaveTextContent('Esc Salir');
		await userEvent.keyboard('g');
		await expect.element(bar()).toHaveTextContent('Esc Borrar búsqueda');
		await expect.element(bar()).not.toHaveTextContent('Salir');
	});

	it('a code typed fast with Enter opens that record when it arrives', async () => {
		const s = spies();
		const { rerender } = await render(ListHarness, s);
		await userEvent.keyboard('7{Enter}');
		// Not a row of the old list: the search goes out at once, without waiting for the pause.
		expect(s.onOpen).not.toHaveBeenCalled();
		expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('7');
		await rerender({ search: '7', rows: [seven] });
		await vi.waitFor(() => expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(seven));
		await wait(400);
		expect(s.onSearch).toHaveBeenCalledOnce();
		expect(s.onOpen).toHaveBeenCalledOnce();
	});

	it('a code that does not exist opens nothing', async () => {
		const s = spies();
		// An empty list still takes the Enter while the search is on its way.
		const { rerender } = await render(ListHarness, { ...s, rows: [] });
		await userEvent.keyboard('7{Enter}');
		expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('7');
		await rerender({ search: '7', rows: [] });
		await expect.element(page.getByText('No existe ningún cliente con el código 7.')).toBeVisible();
		// The Enter is spent: rows arriving later for the same search are not opened.
		await rerender({ rows: [seven] });
		await expect.element(row(1)).toHaveAttribute('aria-selected', 'true');
		expect(s.onOpen).not.toHaveBeenCalled();
	});

	it('typing again cancels an Enter that was waiting', async () => {
		const s = spies();
		const { rerender } = await render(ListHarness, s);
		await userEvent.keyboard('7{Enter}1');
		await rerender({ search: '7', rows: [seven] });
		await expect.element(row(1)).toHaveTextContent('Fruta Siete');
		await vi.waitFor(() => expect(s.onSearch).toHaveBeenLastCalledWith('71'));
		expect(s.onOpen).not.toHaveBeenCalled();
	});

	it('page keys turn the page from the search box', async () => {
		const s = spies();
		await render(ListHarness, { ...s, page: { number: 1, totalPages: 3 } });
		await expect.element(searchBox()).toHaveFocus();
		await userEvent.keyboard('{PageDown}');
		expect(s.onPage).toHaveBeenCalledExactlyOnceWith(2);
		await userEvent.keyboard('{PageUp}');
		expect(s.onPage).toHaveBeenLastCalledWith(0);
	});

	it('the search box follows a search it did not send', async () => {
		const s = spies();
		// The list as left with `?q=gar`; then the top bar's "Clientes" link, with no search.
		const { rerender } = await render(ListHarness, { ...s, search: 'gar', rows: [rows[0]] });
		await expect.element(searchBox()).toHaveValue('gar');
		await rerender({ search: '', rows });
		await expect.element(searchBox()).toHaveValue('');
		await expect.element(bar()).toHaveTextContent('Esc Salir');
		await userEvent.keyboard('{ArrowDown}{Enter}');
		expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
		expect(s.onSearch).not.toHaveBeenCalled();
	});

	it('a page turned while a code search is on its way does not leave Enter stuck', async () => {
		const s = spies();
		const { rerender } = await render(ListHarness, { ...s, page: { number: 0, totalPages: 3 } });
		await userEvent.keyboard('7{Enter}');
		expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('7');
		// The page replaces the search on its way: the rows that come are page 2 of the old list.
		await userEvent.keyboard('{PageDown}');
		expect(s.onPage).toHaveBeenCalledExactlyOnceWith(1);
		await rerender({ page: { number: 1, totalPages: 3 } });
		await userEvent.keyboard('{Enter}');
		expect(s.onSearch).toHaveBeenCalledTimes(2);
		expect(s.onSearch).toHaveBeenLastCalledWith('7');
		await rerender({ search: '7', rows: [seven], page: { number: 0, totalPages: 1 } });
		await vi.waitFor(() => expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(seven));
	});

	it('leaving with Esc or F2 drops a search still waiting for the pause', async () => {
		const s = spies();
		await render(ListHarness, s);
		// Typed and erased: Esc already reads "Salir" while the search for '' waits.
		await userEvent.keyboard('g{Backspace}{Escape}');
		expect(s.onExit).toHaveBeenCalledOnce();
		await wait(400);
		expect(callsAfter(s.onSearch, s.onExit)).toBe(0);

		await userEvent.keyboard('gar{F2}');
		expect(s.onNew).toHaveBeenCalledOnce();
		await wait(400);
		expect(callsAfter(s.onSearch, s.onNew)).toBe(0);
	});

	it('opening a row drops a search still waiting for the pause', async () => {
		const s = spies();
		await render(ListHarness, s);
		await userEvent.keyboard('gar');
		await row(2).dblClick();
		expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
		await wait(400);
		expect(callsAfter(s.onSearch, s.onOpen)).toBe(0);
	});

	it('a search still waiting is dropped on leaving', async () => {
		const s = spies();
		const { unmount } = await render(ListHarness, s);
		fakePause();
		await userEvent.keyboard('gar');
		await unmount();
		vi.advanceTimersByTime(1000);
		expect(s.onSearch).not.toHaveBeenCalled();
	});

	describe('buttons', () => {
		it('the buttons sit on the title line', async () => {
			await render(ListHarness, { ...spies(), deletion: deletion() });
			const line = page.getByRole('heading', { level: 1 }).element().parentElement!;
			for (const label of ['Nuevo', 'Abrir', 'Borrar']) {
				expect(line).toContainElement(button(label).element() as HTMLElement);
			}
		});

		it('Nuevo and Abrir do what F2 and Enter do', async () => {
			const s = spies();
			const { rerender } = await render(ListHarness, s);
			await button('Nuevo').click();
			expect(s.onNew).toHaveBeenCalledOnce();
			await button('Abrir').click();
			expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[0]);
			await rerender({ rows: [] });
			await expect.element(button('Abrir')).toBeDisabled();
			await expect.element(button('Nuevo')).toBeEnabled();
		});

		it('Abrir does not look at the focus', async () => {
			const s = spies();
			await render(ListHarness, s);
			await row(2).click();
			await expect.element(searchBox()).not.toHaveFocus();
			await expect.element(button('Abrir')).toBeEnabled();
			await button('Abrir').click();
			expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
		});

		it('the page row shows the page and its buttons', async () => {
			const s = spies();
			const { rerender } = await render(ListHarness, { ...s, page: { number: 1, totalPages: 3 } });
			await expect.element(page.getByText('Página 2 de 3')).toBeVisible();
			await button('Página siguiente').click();
			expect(s.onPage).toHaveBeenCalledExactlyOnceWith(2);
			await button('Página anterior').click();
			expect(s.onPage).toHaveBeenLastCalledWith(0);

			// Each button greys out at its end.
			await rerender({ page: { number: 0, totalPages: 3 } });
			await expect.element(button('Página anterior')).toBeDisabled();
			await expect.element(button('Página siguiente')).toBeEnabled();
			await rerender({ page: { number: 2, totalPages: 3 } });
			await expect.element(button('Página anterior')).toBeEnabled();
			await expect.element(button('Página siguiente')).toBeDisabled();

			await rerender({ page: { number: 0, totalPages: 1 } });
			await expect.element(page.getByText(/^Página \d+ de/)).not.toBeInTheDocument();
			await expect.element(button('Página anterior')).not.toBeInTheDocument();
			await expect.element(button('Página siguiente')).not.toBeInTheDocument();
		});

		it('page keys work with the focus outside the search box', async () => {
			const s = spies();
			await render(ListHarness, { ...s, page: { number: 1, totalPages: 3 } });
			await row(2).click();
			await expect.element(searchBox()).not.toHaveFocus();
			await userEvent.keyboard('{PageDown}');
			expect(s.onPage).toHaveBeenCalledExactlyOnceWith(2);
			await userEvent.keyboard('{PageUp}');
			expect(s.onPage).toHaveBeenLastCalledWith(0);
		});
	});

	describe('delete', () => {
		it('Borrar asks about the marked row', async () => {
			const d = deletion();
			await render(ListHarness, { ...spies(), deletion: d });
			await userEvent.keyboard('{ArrowDown}');
			await button('Borrar').click();
			await expect.element(dialog()).toHaveTextContent('¿Borrar el cliente 2?');
			const lines = [...dialog().element().querySelectorAll('p')].map((p) => p.textContent?.trim());
			expect(lines).toEqual(['Bar Pepe', pending]);
			expect(d.options.pendingForDelete).toHaveBeenCalledExactlyOnceWith(12);
			expect(d.options.remove).not.toHaveBeenCalled();
		});

		it('F8 deletes the marked row and reloads', async () => {
			const d = deletion();
			await render(ListHarness, { ...spies(), deletion: d });
			await userEvent.keyboard('{ArrowDown}{F8}');
			await expect.element(dialog()).toHaveTextContent('¿Borrar el cliente 2?');
			await userEvent.keyboard('{F8}');
			await vi.waitFor(() => expect(d.reload).toHaveBeenCalledOnce());
			expect(d.options.remove).toHaveBeenCalledExactlyOnceWith(12);
			expect(d.toast).toHaveBeenCalledExactlyOnceWith('Cliente 2 borrado');
			expect(d.onDeleted).toHaveBeenCalledExactlyOnceWith(12);
			// Told before the reload: the page forgets the remembered row first.
			expect(d.onDeleted.mock.invocationCallOrder[0]).toBeLessThan(
				d.reload.mock.invocationCallOrder[0]
			);
			await expect.element(dialog()).not.toBeInTheDocument();
		});

		it('F8 waits for the rows a delete reloads', async () => {
			const d = deletion();
			const { rerender } = await render(ListHarness, { ...spies(), deletion: d });
			await userEvent.keyboard('{F8}');
			await expect.element(dialog()).toBeVisible();
			await userEvent.keyboard('{F8}');
			await vi.waitFor(() => expect(d.reload).toHaveBeenCalledOnce());
			// The deleted row is still on screen until its rows come back: no question about it.
			await userEvent.keyboard('{F8}');
			expect(d.options.pendingForDelete).toHaveBeenCalledOnce();
			await rerender({ rows: [rows[1], rows[2]] });
			await userEvent.keyboard('{F8}');
			await expect.element(dialog()).toHaveTextContent('¿Borrar el cliente 2?');
		});

		it('Enter or Esc keeps the row', async () => {
			const s = spies();
			const d = deletion();
			await render(ListHarness, { ...s, deletion: d });
			for (const key of ['{Enter}', '{Escape}']) {
				await userEvent.keyboard('{F8}');
				await expect.element(dialog()).toBeVisible();
				await userEvent.keyboard(key);
				await expect.element(dialog()).not.toBeInTheDocument();
			}
			expect(d.options.remove).not.toHaveBeenCalled();
			expect(d.reload).not.toHaveBeenCalled();
			expect(s.onOpen).not.toHaveBeenCalled();
			expect(s.onExit).not.toHaveBeenCalled();
		});

		it('the cancelled question gives the cursor back to the search box', async () => {
			const s = spies();
			await render(ListHarness, { ...s, deletion: deletion() });
			await expect.element(searchBox()).toHaveFocus();
			await userEvent.keyboard('{ArrowDown}');
			await button('Borrar').click();
			await expect.element(dialog()).toBeVisible();
			await userEvent.keyboard('{Enter}');
			await expect.element(dialog()).not.toBeInTheDocument();
			await expect.element(searchBox()).toHaveFocus();
			await userEvent.keyboard('{Enter}');
			expect(s.onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
		});

		it('after a delete the mark stays in place', async () => {
			const d = deletion();
			const four = [...rows, { id: 14, code: 4, name: 'Casa Luis' }];
			// The remembered row is the first one: after a delete, the mark's place wins over it.
			const { rerender } = await render(ListHarness, {
				...spies(),
				deletion: d,
				rows: four,
				selectId: 11
			});
			const deleteMarked = async (title: string) => {
				await userEvent.keyboard('{F8}');
				await expect.element(dialog()).toHaveTextContent(title);
				await userEvent.keyboard('{F8}');
				await expect.element(dialog()).not.toBeInTheDocument();
			};

			// A middle row: the next one takes its place, and the mark with it.
			await userEvent.keyboard('{ArrowDown}');
			await deleteMarked('¿Borrar el cliente 2?');
			await rerender({ rows: [four[0], four[2], four[3]] });
			await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');

			// The last row: the mark goes to the new last one.
			await userEvent.keyboard('{ArrowDown}');
			await deleteMarked('¿Borrar el cliente 4?');
			await rerender({ rows: [four[0], four[2]] });
			await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');

			// The first row.
			await userEvent.keyboard('{ArrowUp}');
			await deleteMarked('¿Borrar el cliente 1?');
			await rerender({ rows: [four[2]] });
			await expect.element(row(1)).toHaveAttribute('aria-selected', 'true');
			expect(d.reload).toHaveBeenCalledTimes(3);
		});

		it('deleting the only row of the last page goes to the previous page', async () => {
			const s = spies();
			const d = deletion();
			const { rerender } = await render(ListHarness, {
				...s,
				deletion: d,
				rows: [rows[2]],
				page: { number: 2, totalPages: 3 }
			});
			await userEvent.keyboard('{F8}');
			await expect.element(dialog()).toBeVisible();
			await userEvent.keyboard('{F8}');
			await vi.waitFor(() => expect(d.reload).toHaveBeenCalledOnce());
			expect(s.onPage).not.toHaveBeenCalled();
			// The same page again, now empty and past the last one.
			await rerender({ rows: [], page: { number: 2, totalPages: 2 } });
			await vi.waitFor(() => expect(s.onPage).toHaveBeenCalledExactlyOnceWith(1));
		});

		it('a failed delete still reloads', async () => {
			const d = deletion();
			d.options.remove.mockRejectedValue(new ApiError(404, 'CLIENT_NOT_FOUND', 'Not found', {}));
			await render(ListHarness, { ...spies(), deletion: d });
			await userEvent.keyboard('{F8}');
			await expect.element(dialog()).toHaveTextContent('¿Borrar el cliente 1?');
			await userEvent.keyboard('{F8}');
			await expect
				.element(dialog())
				.toHaveTextContent('No existe ese cliente. Puede que se haya borrado.');
			await vi.waitFor(() => expect(d.reload).toHaveBeenCalledOnce());
			expect(d.onDeleted).not.toHaveBeenCalled();
			expect(d.toast).not.toHaveBeenCalled();
		});

		it('a failed delete keeps the mark in place', async () => {
			const d = deletion();
			d.options.remove.mockRejectedValue(new ApiError(404, 'CLIENT_NOT_FOUND', 'Not found', {}));
			const { rerender } = await render(ListHarness, { ...spies(), deletion: d });
			await userEvent.keyboard('{ArrowDown}{F8}');
			await expect.element(dialog()).toHaveTextContent('¿Borrar el cliente 2?');
			await userEvent.keyboard('{F8}');
			await vi.waitFor(() => expect(d.reload).toHaveBeenCalledOnce());
			// The reload brings the same rows back.
			await rerender({ rows: [...rows] });
			await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');
		});

		it('a double click on the question does not open the row behind it', async () => {
			const s = spies();
			// Enough rows to have one under the question's buttons once it closes.
			const many = Array.from({ length: 20 }, (_, i) => ({
				id: 100 + i,
				code: i + 1,
				name: `Cliente ${i + 1}`
			}));
			await render(ListHarness, { ...s, deletion: deletion(), rows: many });
			await userEvent.keyboard('{ArrowDown}{F8}');
			await expect.element(dialog()).toBeVisible();
			await dialog()
				.getByRole('button', { name: /No borrar/ })
				.dblClick();
			await expect.element(dialog()).not.toBeInTheDocument();
			expect(s.onOpen).not.toHaveBeenCalled();
			await expect.element(row(2)).toHaveAttribute('aria-selected', 'true');
		});

		it('without deletion there is no Borrar', async () => {
			await render(ListHarness, spies());
			await expect.element(button('Nuevo')).toBeVisible();
			await expect.element(button('Borrar')).not.toBeInTheDocument();
			await expect.element(bar()).not.toHaveTextContent('F8');
		});

		it('F8 is in the key bar', async () => {
			const { rerender } = await render(ListHarness, { ...spies(), deletion: deletion() });
			await expect.element(bar()).toHaveTextContent('F8 Borrar');
			await expect.element(button('Borrar')).toBeEnabled();
			await rerender({ rows: [] });
			await expect.element(bar()).not.toHaveTextContent('F8');
			await expect.element(button('Borrar')).toBeDisabled();
		});
	});
});
