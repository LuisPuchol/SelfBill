import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import { ApiError } from '#lib/api/errors.js';
import TaxRecordHarness from '#lib/test/TaxRecordHarness.svelte';

const created = { id: 7, validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 };
const blank = { validFrom: '2026-01-01', vatPercentage: null, surchargePercentage: null };

function props(overrides: Record<string, unknown> = {}) {
	return {
		mode: 'new' as 'new' | 'edit',
		initial: blank,
		create: vi.fn().mockResolvedValue(created),
		update: vi.fn().mockResolvedValue(created),
		goToList: vi.fn(),
		toast: vi.fn(),
		...overrides
	};
}

const vat = () => page.getByLabelText('IVA');
const surcharge = () => page.getByLabelText('Recargo');

describe('TaxRecord, new', () => {
	it('shows the proposed date', async () => {
		await render(TaxRecordHarness, props({ initial: { ...blank, validFrom: '2026-10-18' } }));
		await expect.element(page.getByRole('heading', { name: 'Tipo nuevo' })).toBeVisible();
		await expect.element(page.getByLabelText('Desde')).toHaveValue('18/10/2026');
	});

	it('refuses an IVA above 100', async () => {
		const p = props();
		await render(TaxRecordHarness, p);
		await vat().fill('150');
		await surcharge().fill('0,5');
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Tiene que estar entre 0 y 100')).toBeVisible();
		expect(p.create).not.toHaveBeenCalled();
	});

	it('sends the rates as numbers and goes back to the list', async () => {
		const p = props();
		await render(TaxRecordHarness, p);
		await vat().fill('4');
		await surcharge().fill('0,5');
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(p.goToList).toHaveBeenCalledWith(7));
		expect(p.create).toHaveBeenCalledWith({
			validFrom: '2026-01-01',
			vatPercentage: 4,
			surchargePercentage: 0.5
		});
		expect(p.toast).toHaveBeenCalledWith('Tipo desde el 01/01/2026 guardado');
	});

	it('a repeated date shows under Desde', async () => {
		const p = props({
			create: vi
				.fn()
				.mockRejectedValue(new ApiError(409, 'TAX_CONFIG_ALREADY_EXISTS', 'exists', {}))
		});
		await render(TaxRecordHarness, p);
		await vat().fill('4');
		await surcharge().fill('0,5');
		await userEvent.keyboard('{NumpadAdd}');
		await expect
			.element(page.getByText('Ya hay un tipo que empieza ese día: corrígelo desde la lista.'))
			.toBeVisible();
		await expect.element(page.getByLabelText('Desde')).toHaveFocus();
		expect(p.goToList).not.toHaveBeenCalled();
	});
});

describe('TaxRecord, edit', () => {
	it('corrects the rates of the period, whose date is not a field', async () => {
		const p = props({
			mode: 'edit',
			initial: { validFrom: '2026-01-01', vatPercentage: 4, surchargePercentage: 0.5 }
		});
		await render(TaxRecordHarness, p);
		await expect
			.element(page.getByRole('heading', { name: 'Tipo desde el 01/01/2026' }))
			.toBeVisible();
		expect(page.getByLabelText('Desde').elements()).toHaveLength(0);
		await expect
			.element(
				page.getByText(
					'Los albaranes y facturas ya hechos conservan el tipo con el que se valoraron.'
				)
			)
			.toBeVisible();
		await vat().fill('10');
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(p.goToList).toHaveBeenCalledWith(7));
		expect(p.update).toHaveBeenCalledWith({
			validFrom: '2026-01-01',
			vatPercentage: 10,
			surchargePercentage: 0.5
		});
		expect(p.create).not.toHaveBeenCalled();
	});
});
