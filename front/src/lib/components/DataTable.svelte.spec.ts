import { tick } from 'svelte';
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
	it('without onOpen, Enter does nothing', async () => {
		render(TableHarness, {});
		await userEvent.keyboard('{Enter}');
		await expect.element(page.getByRole('button', { name: 'Abrir' })).not.toBeInTheDocument();
		await page.getByText('Hotel Sol').dblClick();
		await expect.element(page.getByTestId('selected')).toHaveTextContent('2');
	});

	it('scroll mode keeps the titles in a scroll box', async () => {
		render(TableHarness, { onOpen: vi.fn(), scroll: true });
		const box = page.getByRole('table').element().parentElement as HTMLElement;
		expect(box.style.maxHeight).not.toBe('');
		expect(getComputedStyle(box).overflowY).toBe('auto');
		const th = page.getByRole('columnheader').first().element();
		expect(getComputedStyle(th).position).toBe('sticky');
	});

	it('arrows still move in scroll mode', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen, scroll: true });
		await userEvent.keyboard('{ArrowDown}{Enter}');
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[1]);
	});
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
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[2]);
		await expect.element(page.getByTestId('selected')).toHaveTextContent('2');
	});

	/** One press of the mouse, numbered as the browser numbers the presses of a double click. */
	function pressOn(target: Element, detail: 1 | 2) {
		for (const type of ['mousedown', 'mouseup', 'click', ...(detail === 2 ? ['dblclick'] : [])]) {
			target.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, detail }));
		}
	}

	it('a double click that began outside the table neither marks nor opens a row', async () => {
		const onOpen = vi.fn();
		render(TableHarness, { onOpen });
		// Both presses on the row: a double click.
		pressOn(page.getByText('Hotel Sol').element(), 1);
		pressOn(page.getByText('Hotel Sol').element(), 2);
		expect(onOpen).toHaveBeenCalledExactlyOnceWith(rows[2]);

		// The first press on a dialog's button, which closes: the second lands on the row behind it.
		pressOn(page.getByRole('button', { name: 'Otro' }).element(), 1);
		pressOn(page.getByText('Bar Pepe').element(), 2);
		await tick();
		await expect.element(page.getByTestId('selected')).toHaveTextContent('2');
		expect(onOpen).toHaveBeenCalledOnce();
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
