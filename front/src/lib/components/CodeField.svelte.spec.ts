import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import CodeHarness from '#lib/test/CodeHarness.svelte';

const articulo = () => page.getByLabelText('Artículo');
const cantidad = () => page.getByLabelText('Cantidad');

function deferred<T>() {
	let resolve!: (value: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
}

describe('CodeField', () => {
	it('shows the name and moves on when the code exists', async () => {
		const lookup = vi.fn(async (code: number) => (code === 12 ? 'Tomate' : null));
		render(CodeHarness, { lookup });
		await articulo().click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(page.getByText('Tomate')).toBeVisible();
		await expect.element(cantidad()).toHaveFocus();
		expect(lookup).toHaveBeenCalledTimes(1);
		expect(lookup).toHaveBeenCalledWith(12);
		await expect.element(page.getByTestId('articleId')).toHaveTextContent('12');
	});

	it('keeps the cursor when the code does not exist', async () => {
		render(CodeHarness, { lookup: async () => null });
		await articulo().click();
		await userEvent.keyboard('99{Enter}');
		await expect.element(page.getByText('No existe ningún artículo con ese código')).toBeVisible();
		await expect.element(articulo()).toHaveFocus();
	});

	it('keeps only digits', async () => {
		render(CodeHarness, { lookup: async () => null });
		await articulo().click();
		await userEvent.keyboard('1a2');
		await expect.element(articulo()).toHaveValue('12');
	});

	it('a second Enter during the lookup does nothing', async () => {
		const pending = deferred<string | null>();
		const lookup = vi.fn(() => pending.promise);
		render(CodeHarness, { lookup });
		await articulo().click();
		await userEvent.keyboard('5{Enter}{Enter}');
		expect(lookup).toHaveBeenCalledTimes(1);
		await expect.element(articulo()).toHaveFocus();
		pending.resolve('Pepino');
		await expect.element(cantidad()).toHaveFocus();
		await expect.element(page.getByText('Pepino')).toBeVisible();
		expect(lookup).toHaveBeenCalledTimes(1);
	});

	it('editing the code clears the old name', async () => {
		render(CodeHarness, { lookup: async () => 'Tomate' });
		await articulo().click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(page.getByText('Tomate')).toBeVisible();
		await articulo().click();
		await userEvent.keyboard('3');
		await expect.element(page.getByText('Tomate')).not.toBeInTheDocument();
	});

	it('an initial value shows its name', async () => {
		const lookup = vi.fn(async () => 'Lechuga');
		render(CodeHarness, { lookup, initial: 7 });
		await expect.element(page.getByText('Lechuga')).toBeVisible();
		expect(lookup).toHaveBeenCalledWith(7);
	});

	it('the bound value stays null when the code does not exist', async () => {
		render(CodeHarness, { lookup: async () => null });
		await articulo().click();
		await userEvent.keyboard('99{Enter}');
		await expect.element(page.getByText('No existe ningún artículo con ese código')).toBeVisible();
		await expect.element(page.getByTestId('articleId')).toHaveTextContent('null');
	});

	it('typing a new code after a found one clears the bound value', async () => {
		render(CodeHarness, { lookup: async () => 'Tomate' });
		await articulo().click();
		await userEvent.keyboard('12{Enter}');
		await expect.element(page.getByTestId('articleId')).toHaveTextContent('12');
		await articulo().click();
		await userEvent.keyboard('3');
		await expect.element(page.getByTestId('articleId')).toHaveTextContent('null');
	});

	it('a slow lookup resolving after the text changed sets nothing', async () => {
		const pending = deferred<string | null>();
		render(CodeHarness, { lookup: () => pending.promise });
		await articulo().click();
		await userEvent.keyboard('5{Enter}6');
		pending.resolve('Pepino');
		await expect.element(articulo()).toHaveValue('56');
		await expect.element(articulo()).toHaveFocus();
		await expect.element(page.getByText('Pepino')).not.toBeInTheDocument();
		await expect.element(page.getByTestId('articleId')).toHaveTextContent('null');
	});
});
