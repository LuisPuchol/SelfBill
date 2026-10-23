import { describe, expect, it } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import type { Column } from '#lib/components/table.js';
import StaticTable from '#lib/components/StaticTable.svelte';

// The component is generic: the test passes its columns as the unknown row type it infers.
const columns: Column<unknown>[] = [
	{ label: 'Cliente', value: (r) => (r as { name: string }).name }
];
const rows = [{ name: 'Frutas Paco' }, { name: 'Bar Pepe' }];

describe('StaticTable', () => {
	it('renders the titles and the cells', async () => {
		render(StaticTable, { columns, rows });
		await expect.element(page.getByRole('columnheader', { name: 'Cliente' })).toBeVisible();
		await expect.element(page.getByText('Bar Pepe')).toBeVisible();
	});

	it('has no selection and no keys', async () => {
		render(StaticTable, { columns, rows });
		await userEvent.keyboard('{ArrowDown}');
		expect(document.querySelector('[aria-selected="true"]')).toBeNull();
	});

	it('says so when there are no rows', async () => {
		render(StaticTable, { columns, rows: [], emptyText: 'Sin líneas.' });
		await expect.element(page.getByText('Sin líneas.')).toBeVisible();
	});
});
