import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import TableHarness from '#lib/test/TableHarness.svelte';

const rows = [
	{ name: 'Frutas Paco', amount: 84 },
	{ name: 'Bar Pepe', amount: 32 },
	{ name: 'Hotel Sol', amount: 210 }
];

describe('DataTable', () => {
	it('renders the rows', async () => {
		render(TableHarness, { onOpen: vi.fn() });
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
		await expect.element(page.getByText('32,00\u00a0€')).toBeVisible();
	});

	it('arrows move the selection and Enter opens it', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		await userEvent.keyboard('{ArrowDown}{Enter}');
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
	});

	it('the selection stays inside the list', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		await userEvent.keyboard('{ArrowUp}{Enter}');
		expect(onOpen).toHaveBeenLastCalledWith(rows[0]);
		await userEvent.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{ArrowDown}{ArrowDown}{Enter}');
		expect(onOpen).toHaveBeenLastCalledWith(rows[2]);
	});

	it('double click opens a row', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		await page.getByText('Hotel Sol').dblClick();
		expect(onOpen).toHaveBeenCalledWith(rows[2]);
	});

	it('page keys ask for the next and previous page', async () => {
		const onPage = vi.fn();
		render(TableHarness, {
			onOpen: vi.fn(),
			onPage,
			page: { number: 0, totalPages: 3 }
		});
		await expect.element(page.getByText('Página 1 de 3')).toBeVisible();
		await userEvent.keyboard('{PageDown}');
		expect(onPage).toHaveBeenCalledExactlyOnceWith(1);
		await userEvent.keyboard('{PageUp}');
		expect(onPage).toHaveBeenCalledTimes(1);
	});

	it('an empty list says so', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen, rows: [] });
		await expect.element(page.getByText('No hay nada que mostrar.')).toBeVisible();
		await userEvent.keyboard('{Enter}');
		expect(onOpen).not.toHaveBeenCalled();
	});

	function press(target: Element, key: string): boolean {
		const event = new KeyboardEvent('keydown', { key, code: key, bubbles: true, cancelable: true });
		target.dispatchEvent(event);
		return event.defaultPrevented;
	}

	it('arrows and page keys are left to a focused text input', async () => {
		const onPage = vi.fn();
		render(TableHarness, { onOpen: vi.fn(), onPage, page: { number: 1, totalPages: 3 } });
		const input = page.getByLabelText('Filtro');
		await input.click();
		const el = input.element();
		expect(press(el, 'ArrowDown')).toBe(false);
		expect(press(el, 'PageDown')).toBe(false);
		expect(onPage).not.toHaveBeenCalled();
		await expect.element(page.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'true');
	});

	it('arrows are not handled on an empty list', async () => {
		render(TableHarness, { onOpen: vi.fn(), rows: [] });
		expect(press(document.body, 'ArrowDown')).toBe(false);
	});

	it('the selection goes back to the first row when the rows change', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		await userEvent.keyboard('{ArrowDown}{ArrowDown}');
		await page.getByRole('button', { name: 'Cambiar filas' }).click();
		await expect.element(page.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'true');
		await userEvent.keyboard('{Enter}');
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[2]);
	});
});
