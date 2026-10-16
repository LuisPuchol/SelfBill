import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import DialogHarness from '#lib/test/DialogHarness.svelte';

async function open(onSave = vi.fn(), onOk = vi.fn()) {
	render(DialogHarness, { onSave, onOk });
	await page.getByRole('button', { name: 'Abrir' }).click();
	await expect.element(page.getByRole('dialog')).toBeVisible();
	return { onSave, onOk };
}

describe('Dialog', () => {
	it('shows its actions as buttons and in the key bar', async () => {
		await open();
		await expect.element(page.getByRole('button', { name: /Vale/ })).toBeVisible();
		const bar = page.getByRole('contentinfo');
		await expect.element(bar).toHaveTextContent('Vale');
		await expect.element(bar).not.toHaveTextContent('Guardar');
	});

	it('Enter runs the dialog action', async () => {
		const { onOk } = await open();
		await userEvent.keyboard('{Enter}');
		expect(onOk).toHaveBeenCalledOnce();
		await expect.element(page.getByRole('dialog')).not.toBeInTheDocument();
	});

	it('page keys do not reach the page while open', async () => {
		const { onSave } = await open();
		await userEvent.keyboard('{NumpadAdd}');
		expect(onSave).not.toHaveBeenCalled();
	});

	it('focus returns to where it was', async () => {
		render(DialogHarness, { onSave: vi.fn(), onOk: vi.fn() });
		const nombre = page.getByLabelText('Nombre');
		await nombre.click();
		await expect.element(nombre).toHaveFocus();
		(document.querySelector('button') as HTMLButtonElement).click();
		await expect.element(page.getByRole('dialog')).toBeVisible();
		await userEvent.keyboard('{Escape}');
		await expect.element(nombre).toHaveFocus();
	});

	it('survives repeated Esc when it has no Escape action', async () => {
		const onSave = vi.fn();
		const onOk = vi.fn();
		render(DialogHarness, { onSave, onOk, withEscape: false });
		await page.getByRole('button', { name: 'Abrir' }).click();
		await expect.element(page.getByRole('dialog')).toBeVisible();
		await userEvent.keyboard('{Escape}{Escape}{Escape}{Escape}');
		await expect.element(page.getByRole('dialog')).toBeVisible();
		await userEvent.keyboard('{NumpadAdd}');
		expect(onSave).not.toHaveBeenCalled();
		await userEvent.keyboard('{Enter}');
		expect(onOk).toHaveBeenCalledOnce();
	});

	it('resyncs when the native dialog closes anyway', async () => {
		const { onSave } = await open();
		document.querySelector('dialog')!.close();
		await expect.element(page.getByRole('contentinfo')).toHaveTextContent('Guardar');
		await userEvent.keyboard('{NumpadAdd}');
		expect(onSave).toHaveBeenCalledOnce();
	});
});
