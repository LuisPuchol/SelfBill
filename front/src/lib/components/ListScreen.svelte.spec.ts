import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ListHarness from '#lib/test/ListHarness.svelte';

const rows = [
	{ id: 11, code: 1, name: 'Frutas Paco' },
	{ id: 12, code: 2, name: 'Bar Pepe' },
	{ id: 13, code: 3, name: 'Hotel Sol' }
];
const seven = { id: 17, code: 7, name: 'Fruta Siete' };

function spies() {
	return { onSearch: vi.fn(), onOpen: vi.fn(), onNew: vi.fn(), onExit: vi.fn(), onPage: vi.fn() };
}

const searchBox = () => page.getByLabelText('Buscar (código o nombre)');
const bar = () => page.getByRole('contentinfo');
/** Data rows start at 1: the first row of the table is its header. */
const row = (n: number) => page.getByRole('row').nth(n);
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

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
		// Each key restarts the wait: 400 ms in all, but never 300 ms without typing.
		await userEvent.keyboard('g');
		await wait(200);
		await userEvent.keyboard('a');
		await wait(200);
		await userEvent.keyboard('r');
		expect(s.onSearch).not.toHaveBeenCalled();
		await vi.waitFor(() => expect(s.onSearch).toHaveBeenCalledExactlyOnceWith('gar'));
		await wait(400);
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

	it('a search still waiting is dropped on leaving', async () => {
		const s = spies();
		const { unmount } = await render(ListHarness, s);
		await userEvent.keyboard('gar');
		await unmount();
		await wait(400);
		expect(s.onSearch).not.toHaveBeenCalled();
	});
});
