import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import GridHarness from '#lib/test/GridHarness.svelte';
import { emptyLine, type LineValues } from '#lib/deliverynotes/form.js';

const catalog: Record<number, string> = { 12: 'Tomate', 15: 'Pepino' };

function props(overrides: Record<string, unknown> = {}) {
	return {
		initial: [emptyLine()],
		lookupArticle: vi.fn(async (code: number) => catalog[code] ?? null),
		articleName: (code: number) => catalog[code],
		proposePrice: vi.fn<(code: number) => Promise<number | null>>().mockResolvedValue(null),
		...overrides
	};
}

function line(articleCode: number, lot1: string, quantity: number, price: number): LineValues {
	return { articleCode, lot1, lot2: '', lot3: '', quantity, price };
}

const tomate = () => line(12, '01181026', 25, 1.2);

function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

type Column = 'Código' | 'Lote' | 'Cantidad' | 'Precio';
const cell = (row: number, column: Column) => page.getByLabelText(column, { exact: true }).nth(row);
const linesNow = (): LineValues[] =>
	JSON.parse(page.getByTestId('lines').element().textContent ?? '[]');
const dialog = () => page.getByRole('dialog');
const lot = (n: number) => dialog().getByLabelText(`Lote ${n}`, { exact: true });
const bar = () => page.getByRole('contentinfo');

describe('LinesGrid', () => {
	it("Enter walks code → lot → quantity → price, then the next line's code", async () => {
		await render(GridHarness, props({ initial: [emptyLine(), emptyLine()] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await userEvent.keyboard('01181026{Enter}');
		await expect.element(cell(0, 'Cantidad')).toHaveFocus();
		await userEvent.keyboard('25{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveFocus();
		await userEvent.keyboard('1,2{Enter}');
		await expect.element(cell(1, 'Código')).toHaveFocus();
		expect(linesNow()).toEqual([tomate(), emptyLine()]);
		// The amount: 25 × 1,20.
		await expect.element(page.getByRole('cell', { name: '30,00', exact: true })).toBeVisible();
	});

	it('Enter on the last price adds a line and puts the cursor in its code', async () => {
		await render(GridHarness, props({ initial: [tomate()] }));
		await cell(0, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(1, 'Código')).toHaveFocus();
		expect(linesNow()).toEqual([tomate(), emptyLine()]);
	});

	it('Enter past an empty last line goes back to its code instead of adding another', async () => {
		await render(GridHarness, props({ initial: [tomate(), emptyLine()] }));
		await cell(1, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(1, 'Código')).toHaveFocus();
		expect(linesNow()).toHaveLength(2);
	});

	it('the found article name shows in its column', async () => {
		await render(GridHarness, props());
		await cell(0, 'Código').click();
		await userEvent.keyboard('15{Enter}');
		await expect.element(page.getByRole('cell', { name: 'Pepino', exact: true })).toBeVisible();
	});

	it('the last price is proposed when the price is empty', async () => {
		const proposePrice = vi.fn(async () => 1.2);
		await render(GridHarness, props({ proposePrice }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		expect(proposePrice).toHaveBeenCalledExactlyOnceWith(12);
		expect(linesNow()[0].price).toBe(1.2);
	});

	it('not when a price is already typed', async () => {
		const proposePrice = vi.fn(async () => 1.2);
		await render(GridHarness, props({ proposePrice }));
		await cell(0, 'Precio').click();
		await userEvent.keyboard('2');
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await expect.element(cell(0, 'Precio')).toHaveValue('2,00');
		expect(proposePrice).not.toHaveBeenCalled();
	});

	it('a price typed while the proposal is on its way wins', async () => {
		const pending = deferred<number | null>();
		await render(GridHarness, props({ proposePrice: () => pending.promise }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await cell(0, 'Precio').click();
		await userEvent.keyboard('3');
		pending.resolve(1.2);
		await pending.promise;
		await expect.element(cell(0, 'Precio')).toHaveValue('3');
		expect(linesNow()[0].price).toBe(3);
	});

	it('an untouched proposal follows the article when its code is corrected', async () => {
		const prices: Record<number, number> = { 12: 1.2, 15: 0.8 };
		await render(GridHarness, props({ proposePrice: async (code: number) => prices[code] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		// Back to the code: it was another article.
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}15{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('0,80');
		expect(linesNow()[0]).toMatchObject({ articleCode: 15, price: 0.8 });
	});

	it('and the price is left empty when the new article has no last price', async () => {
		const prices: Record<number, number> = { 12: 1.2 };
		await render(
			GridHarness,
			props({ proposePrice: async (code: number) => prices[code] ?? null })
		);
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await userEvent.keyboard('{Shift>}{Enter}{/Shift}15{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('');
		expect(linesNow()[0]).toMatchObject({ articleCode: 15, price: null });
	});

	it('typing another code empties an untouched proposal at once: "+" before the lookup ends stops at the price', async () => {
		const pepino = deferred<string | null>();
		const pepinoPrice = deferred<number | null>();
		const o = props({
			lookupArticle: vi.fn(async (code: number) => (code === 15 ? pepino.promise : catalog[code])),
			proposePrice: vi.fn(async (code: number) => (code === 15 ? pepinoPrice.promise : 1.2))
		});
		await render(GridHarness, o);
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}01181026{Enter}25{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveFocus();
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		await cell(0, 'Código').click();
		await userEvent.keyboard('15');
		// The price was the replaced article's: gone before the new one is even looked up.
		await expect.element(cell(0, 'Precio')).toHaveValue('');
		expect(linesNow()[0].price).toBeNull();
		await userEvent.keyboard('{NumpadAdd}');
		pepino.resolve('Pepino');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('false');
		await expect.element(cell(0, 'Precio')).toHaveFocus();
		await expect.element(cell(0, 'Precio')).toHaveAccessibleDescription('Este dato es obligatorio');
		// Found by then: the new article's last price comes.
		pepinoPrice.resolve(0.8);
		await expect.element(cell(0, 'Precio')).toHaveValue('0,80');
		expect(linesNow()[0]).toMatchObject({ articleCode: 15, price: 0.8 });
		expect(o.proposePrice).toHaveBeenLastCalledWith(15);
	});

	it('a price typed over the proposal stays when the code is corrected, even the same one', async () => {
		const prices: Record<number, number> = { 12: 1.2, 15: 0.8 };
		const proposePrice = vi.fn(async (code: number) => prices[code]);
		await render(GridHarness, props({ proposePrice }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		await cell(0, 'Precio').click();
		await userEvent.keyboard('1,2');
		await cell(0, 'Código').click();
		await userEvent.keyboard('15{Enter}');
		await expect.element(page.getByRole('cell', { name: 'Pepino', exact: true })).toBeVisible();
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		expect(linesNow()[0].price).toBe(1.2);
		expect(proposePrice).toHaveBeenCalledExactlyOnceWith(12);
	});

	it('another client proposes again the prices still untouched, not the typed ones', async () => {
		let prices: Record<number, number> = { 12: 1.2 };
		const proposePrice = async (code: number) => prices[code] ?? null;
		await render(
			GridHarness,
			props({ initial: [emptyLine(), emptyLine(), emptyLine()], proposePrice })
		);
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Precio')).toHaveValue('1,20');
		// No last price for this client: empty, and still the grid's to fill.
		await cell(1, 'Código').click();
		await userEvent.keyboard('15{Enter}');
		await expect.element(cell(1, 'Lote')).toHaveFocus();
		await cell(2, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(2, 'Precio')).toHaveValue('1,20');
		await cell(2, 'Precio').click();
		await userEvent.keyboard('1');
		prices = { 12: 1.5, 15: 0.9 };
		await page.getByRole('button', { name: 'Otro cliente' }).click();
		await expect.element(cell(0, 'Precio')).toHaveValue('1,50');
		await expect.element(cell(1, 'Precio')).toHaveValue('0,90');
		await expect.element(cell(2, 'Precio')).toHaveValue('1,00');
		expect(linesNow().map((l) => l.price)).toEqual([1.5, 0.9, 1]);
	});

	it("a proposal still on its way when the client changes gives way to the new client's", async () => {
		const before = deferred<number | null>();
		const after = deferred<number | null>();
		const proposePrice = vi
			.fn<(code: number) => Promise<number | null>>()
			.mockReturnValueOnce(before.promise)
			.mockReturnValueOnce(after.promise);
		await render(GridHarness, props({ proposePrice }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await page.getByRole('button', { name: 'Otro cliente' }).click();
		expect(proposePrice).toHaveBeenCalledTimes(2);
		// The old client's answer comes first: it is not the price anymore.
		before.resolve(1.2);
		await before.promise;
		await expect.element(cell(0, 'Precio')).toHaveValue('');
		after.resolve(1.5);
		await after.promise;
		await expect.element(cell(0, 'Precio')).toHaveValue('1,50');
		expect(linesNow()[0].price).toBe(1.5);
	});

	it('a saved line keeps its price: on opening, for another client, and for another article', async () => {
		const o = props({
			initial: [line(12, '01181026', 25, 2.5)],
			proposePrice: vi.fn(async () => 1.2)
		});
		await render(GridHarness, o);
		await expect.poll(() => o.lookupArticle.mock.results.length).toBe(1);
		await o.lookupArticle.mock.results[0].value;
		await expect.element(cell(0, 'Precio')).toHaveValue('2,50');
		await page.getByRole('button', { name: 'Otro cliente' }).click();
		await expect.element(cell(0, 'Precio')).toHaveValue('2,50');
		await cell(0, 'Código').click();
		await userEvent.keyboard('1');
		await expect.element(cell(0, 'Precio')).toHaveValue('2,50');
		await userEvent.keyboard('5{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await expect.element(cell(0, 'Precio')).toHaveValue('2,50');
		expect(linesNow()[0]).toMatchObject({ articleCode: 15, price: 2.5 });
		expect(o.proposePrice).not.toHaveBeenCalled();
	});

	it('↑ and ↓ keep the column', async () => {
		await render(GridHarness, props({ initial: [tomate(), line(15, '02181026', 3, 0.33)] }));
		await cell(1, 'Cantidad').click();
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(cell(0, 'Cantidad')).toHaveFocus();
		await userEvent.keyboard('{ArrowUp}');
		await expect.element(cell(0, 'Cantidad')).toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(cell(1, 'Cantidad')).toHaveFocus();
		await userEvent.keyboard('{ArrowDown}');
		await expect.element(cell(1, 'Cantidad')).toHaveFocus();
		expect(linesNow()).toHaveLength(2);
	});

	it('F7 removes the line with the cursor and keeps the column', async () => {
		const a = line(12, '00000001', 1, 1);
		const b = line(15, '00000002', 2, 2);
		const c = line(12, '00000003', 3, 3);
		await render(GridHarness, props({ initial: [a, b, c] }));
		await cell(1, 'Lote').click();
		await expect.element(bar()).toHaveTextContent('F7 Quitar línea');
		await userEvent.keyboard('{F7}');
		expect(linesNow()).toEqual([a, c]);
		// The next line moved up into the same place.
		await expect.element(cell(1, 'Lote')).toHaveFocus();
		await expect.element(cell(1, 'Lote')).toHaveValue('00000003');
		// The last one: back to the previous line.
		await userEvent.keyboard('{F7}');
		expect(linesNow()).toEqual([a]);
		await expect.element(cell(0, 'Lote')).toHaveFocus();
	});

	it('F7 on the only line leaves it empty', async () => {
		await render(GridHarness, props({ initial: [tomate()] }));
		await cell(0, 'Cantidad').click();
		await userEvent.keyboard('{F7}');
		await expect.poll(linesNow).toEqual([emptyLine()]);
		await expect.element(cell(0, 'Código')).toHaveValue('');
		await expect.element(cell(0, 'Lote')).toHaveValue('');
		await expect.element(cell(0, 'Cantidad')).toHaveFocus();
	});

	it('F4 opens the lots; + saves them into the line and "+1" shows', async () => {
		await render(GridHarness, props({ initial: [tomate()] }));
		await cell(0, 'Código').click();
		await expect.element(bar()).toHaveTextContent('F4 Lotes');
		await userEvent.keyboard('{F4}');
		await expect.element(dialog()).toBeVisible();
		await expect.element(lot(1)).toHaveValue('01181026');
		await expect.element(lot(1)).toHaveFocus();
		await expect.element(bar()).toHaveTextContent('+ Aceptar');
		await expect.element(bar()).toHaveTextContent('Esc Cancelar');
		await userEvent.keyboard('{Enter}');
		await expect.element(lot(2)).toHaveFocus();
		await userEvent.keyboard('02181026{NumpadAdd}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(linesNow()[0]).toEqual({ ...tomate(), lot2: '02181026' });
		await expect.element(page.getByText('+1', { exact: true })).toBeVisible();
		await expect.element(cell(0, 'Código')).toHaveFocus();
	});

	it('the lots dialog changes the first lot of the line too', async () => {
		await render(GridHarness, props({ initial: [{ ...tomate(), lot2: '02181026' }] }));
		await expect.element(page.getByText('+1', { exact: true })).toBeVisible();
		await cell(0, 'Lote').click();
		await userEvent.keyboard('{F4}');
		await userEvent.keyboard('11111111{Enter}{Enter}33333333{NumpadAdd}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(cell(0, 'Lote')).toHaveValue('11111111');
		expect(linesNow()[0]).toMatchObject({ lot1: '11111111', lot2: '02181026', lot3: '33333333' });
		await expect.element(page.getByText('+2', { exact: true })).toBeVisible();
	});

	it('Esc cancels', async () => {
		await render(GridHarness, props({ initial: [tomate()] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('{F4}');
		await expect.element(lot(1)).toHaveFocus();
		await userEvent.keyboard('{Enter}02181026{Escape}');
		await expect.element(dialog()).not.toBeInTheDocument();
		expect(linesNow()).toEqual([tomate()]);
		await expect.element(page.getByText('+1', { exact: true })).not.toBeInTheDocument();
		// Opened again: what the line has, not what was typed and cancelled.
		await userEvent.keyboard('{F4}');
		await expect.element(lot(2)).toHaveValue('');
	});

	it('the lots need the first one, and 8 digits each', async () => {
		await render(GridHarness, props({ initial: [tomate()] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('{F4}');
		await expect.element(lot(1)).toHaveFocus();
		await userEvent.keyboard('{Enter}123{Shift>}{Enter}{/Shift}{Backspace}{NumpadAdd}');
		await expect.element(lot(1)).toHaveAccessibleDescription('Este dato es obligatorio');
		await expect.element(lot(2)).toHaveAccessibleDescription('Tienen que ser 8 cifras');
		await expect.element(dialog()).toBeVisible();
		expect(linesNow()).toEqual([tomate()]);
	});

	it('an empty trailing line passes validateAll', async () => {
		await render(GridHarness, props({ initial: [tomate(), emptyLine()] }));
		await cell(1, 'Código').click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('true');
	});

	it('a half-filled line fails validateAll', async () => {
		await render(GridHarness, props({ initial: [{ ...emptyLine(), articleCode: 12 }] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('false');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await expect.element(cell(0, 'Lote')).toHaveAccessibleDescription('Este dato es obligatorio');
		await expect
			.element(cell(0, 'Cantidad'))
			.toHaveAccessibleDescription('Este dato es obligatorio');
		await expect.element(cell(0, 'Precio')).toHaveAccessibleDescription('Este dato es obligatorio');
	});

	it('with every line empty the first one is required: a note needs a line', async () => {
		await render(GridHarness, props({ initial: [emptyLine(), emptyLine()] }));
		await cell(1, 'Código').click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('false');
		await expect.element(cell(0, 'Código')).toHaveFocus();
		await expect.element(cell(0, 'Código')).toHaveAccessibleDescription('Este dato es obligatorio');
		await expect.element(cell(1, 'Código')).toHaveAccessibleDescription('');
	});

	it('a code typed without Enter already makes its line need the rest', async () => {
		await render(GridHarness, props());
		await cell(0, 'Código').click();
		await userEvent.keyboard('12{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('false');
		await expect.element(cell(0, 'Lote')).toHaveAccessibleDescription('Este dato es obligatorio');
	});

	it('the quantity has to be more than zero', async () => {
		await render(GridHarness, props({ initial: [line(12, '01181026', 0, 1.2)] }));
		await cell(0, 'Código').click();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('false');
		await expect
			.element(cell(0, 'Cantidad'))
			.toHaveAccessibleDescription('Tiene que ser mayor que cero');
	});

	it("the API's errors show under their cell", async () => {
		await render(
			GridHarness,
			props({ initial: [tomate()], errors: { 'lines[0].quantity': 'Revisa la cantidad' } })
		);
		await expect.element(cell(0, 'Cantidad')).toHaveAccessibleDescription('Revisa la cantidad');
	});

	it('readOnly: no F4/F7, inputs read-only', async () => {
		await render(GridHarness, props({ initial: [tomate()], readOnly: true }));
		for (const column of ['Código', 'Lote', 'Cantidad', 'Precio'] as const) {
			await expect.element(cell(0, column)).toHaveAttribute('readonly');
		}
		await cell(0, 'Cantidad').click();
		await expect.element(bar()).not.toHaveTextContent('Lotes');
		await expect.element(bar()).not.toHaveTextContent('Quitar línea');
		await expect
			.element(page.getByRole('button', { name: /Lotes|Quitar línea/ }))
			.not.toBeInTheDocument();
		await userEvent.keyboard('9{F4}{F7}');
		await expect.element(dialog()).not.toBeInTheDocument();
		// The numpad's decimal key writes its comma by hand: not into a read-only cell either.
		cell(0, 'Cantidad')
			.element()
			.dispatchEvent(
				new KeyboardEvent('keydown', {
					key: '.',
					code: 'NumpadDecimal',
					bubbles: true,
					cancelable: true
				})
			);
		await expect.element(cell(0, 'Cantidad')).toHaveValue('25,00');
		expect(linesNow()).toEqual([tomate()]);
		// Past the last price no line is added either.
		await cell(0, 'Precio').click();
		await userEvent.keyboard('{Enter}');
		expect(linesNow()).toEqual([tomate()]);
	});

	it('readOnly: an empty cell is not required, so Enter walks on', async () => {
		const gone = { ...tomate(), articleCode: null, savedArticleName: 'Tomate' };
		await render(GridHarness, props({ initial: [gone], readOnly: true }));
		await expect.element(page.getByRole('cell', { name: 'Tomate', exact: true })).toBeVisible();
		await cell(0, 'Código').click();
		await userEvent.keyboard('{Enter}');
		await expect.element(cell(0, 'Lote')).toHaveFocus();
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByTestId('valid')).toHaveTextContent('true');
		await expect.element(page.getByText('Este dato es obligatorio')).not.toBeInTheDocument();
	});

	it('the name a line was saved with shows until a code is typed (its article was deleted)', async () => {
		const gone = { ...tomate(), articleCode: null, savedArticleName: 'Berenjena' };
		await render(GridHarness, props({ initial: [gone] }));
		const saved = () => page.getByRole('cell', { name: 'Berenjena', exact: true });
		await expect.element(saved()).toBeVisible();
		await cell(0, 'Código').click();
		await userEvent.keyboard('1');
		await expect.element(saved()).not.toBeInTheDocument();
		await userEvent.keyboard('5{Enter}');
		await expect.element(page.getByRole('cell', { name: 'Pepino', exact: true })).toBeVisible();
	});

	it('buttons Lotes and Quitar línea act on the line with the cursor', async () => {
		const b = line(15, '02181026', 3, 0.33);
		await render(GridHarness, props({ initial: [tomate(), b] }));
		const lotes = page.getByRole('button', { name: /Lotes/ });
		const quitar = page.getByRole('button', { name: /Quitar línea/ });
		// No line has had the cursor yet.
		await expect.element(lotes).toBeDisabled();
		await expect.element(quitar).toBeDisabled();
		await cell(1, 'Precio').click();
		await lotes.click();
		await expect.element(lot(1)).toHaveValue('02181026');
		await userEvent.keyboard('{Escape}');
		await expect.element(dialog()).not.toBeInTheDocument();
		await expect.element(cell(1, 'Precio')).toHaveFocus();
		await quitar.click();
		expect(linesNow()).toEqual([tomate()]);
		await expect.element(cell(0, 'Precio')).toHaveFocus();
	});
});
