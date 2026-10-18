import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import TaxListHarness from '#lib/test/TaxListHarness.svelte';

const periods = [
	{ id: 3, validFrom: '2027-01-01', vatPercentage: 10, surchargePercentage: 1.4 },
	{ id: 2, validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 },
	{ id: 1, validFrom: '2024-01-01', vatPercentage: 21, surchargePercentage: 5.2 }
];

function props(overrides: Record<string, unknown> = {}) {
	return {
		periods,
		today: '2026-10-18',
		onNew: vi.fn<() => void>(),
		onOpen: vi.fn<(period: unknown) => void>(),
		onExit: vi.fn<() => void>(),
		...overrides
	};
}

const bar = () => page.getByRole('contentinfo');
const button = (label: string) => page.getByRole('button', { name: new RegExp(label) });

describe('TaxList', () => {
	it('lists the periods newest first with their state', async () => {
		await render(TaxListHarness, props());
		const rows = page.getByRole('row');
		await expect.element(rows.nth(1)).toHaveTextContent('01/01/2027');
		await expect.element(rows.nth(1)).toHaveTextContent('10 %');
		await expect.element(rows.nth(1)).toHaveTextContent('1,4 %');
		await expect.element(rows.nth(1)).toHaveTextContent('A partir del 01/01/2027');
		await expect.element(rows.nth(2)).toHaveTextContent('4 %');
		await expect.element(rows.nth(2)).toHaveTextContent('0,5 %');
		await expect.element(rows.nth(2)).toHaveTextContent('Vigente');
		await expect.element(rows.nth(3)).not.toHaveTextContent('Vigente');
		await expect.element(rows.nth(3)).not.toHaveTextContent('A partir');
	});

	it('marks the period in force on entry, or the one asked for', async () => {
		await render(TaxListHarness, props());
		await expect.element(page.getByRole('row').nth(2)).toHaveAttribute('aria-selected', 'true');
	});

	it('marks selectId when given', async () => {
		await render(TaxListHarness, props({ selectId: 1 }));
		await expect.element(page.getByRole('row').nth(3)).toHaveAttribute('aria-selected', 'true');
	});

	it('marks the first row when none is in force', async () => {
		await render(TaxListHarness, props({ today: '2020-01-01' }));
		await expect.element(page.getByRole('row').nth(1)).toHaveAttribute('aria-selected', 'true');
	});

	it('says what to do when empty and greys Corregir out', async () => {
		const p = props({ periods: [] });
		await render(TaxListHarness, p);
		await expect
			.element(page.getByText('Todavía no hay ningún tipo. Pulsa F2 para añadir el primero.'))
			.toBeVisible();
		await expect.element(button('Corregir')).toHaveAttribute('aria-disabled', 'true');
		await button('Corregir').click({ force: true });
		expect(p.onOpen).not.toHaveBeenCalled();
	});

	it('F2 and Nuevo ask for a new period', async () => {
		const p = props();
		await render(TaxListHarness, p);
		await userEvent.keyboard('{F2}');
		expect(p.onNew).toHaveBeenCalledTimes(1);
		await button('Nuevo').click();
		expect(p.onNew).toHaveBeenCalledTimes(2);
	});

	it('Enter and Corregir open the selected period', async () => {
		const p = props();
		await render(TaxListHarness, p);
		await userEvent.keyboard('{Enter}');
		expect(p.onOpen).toHaveBeenCalledTimes(1);
		expect(p.onOpen).toHaveBeenLastCalledWith(periods[1]);
		await button('Corregir').click();
		expect(p.onOpen).toHaveBeenCalledTimes(2);
		expect(p.onOpen).toHaveBeenLastCalledWith(periods[1]);
	});

	it('shows the keys and Esc leaves', async () => {
		const p = props();
		await render(TaxListHarness, p);
		await expect.element(bar()).toHaveTextContent('Enter Corregir');
		await expect.element(bar()).toHaveTextContent('Esc Volver');
		await userEvent.keyboard('{Escape}');
		expect(p.onExit).toHaveBeenCalledOnce();
	});
});
