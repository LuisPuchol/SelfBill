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

	it('Enter does not open a row while a button has focus', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		page.getByRole('button', { name: 'Otro' }).element().focus();
		await userEvent.keyboard('{Enter}');
		expect(onOpen).not.toHaveBeenCalled();
	});

	it('bind:selected follows the arrows', async () => {
		render(TableHarness, { onOpen: vi.fn() });
		await expect.element(page.getByTestId('selected')).toHaveTextContent('0');
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(page.getByTestId('selected')).toHaveTextContent('1');
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

	it('arrows are left to a focused text input', async () => {
		render(TableHarness, { onOpen: vi.fn() });
		const input = page.getByLabelText('Filtro');
		await input.click();
		expect(press(input.element(), 'ArrowDown')).toBe(false);
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
		// The click left the cursor on the button, which would keep Enter for itself.
		(document.activeElement as HTMLElement).blur();
		await userEvent.keyboard('{Enter}');
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[2]);
	});
});
