import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import ProfileRecordHarness from '#lib/test/ProfileRecordHarness.svelte';

const stored = {
	id: 1,
	fullName: 'María López',
	fiscalName: 'Frutas López S.L.',
	nif: 'B87654321',
	address: 'Mercavalencia, nave 3',
	email: 'facturas@frutaslopez.es',
	hasMailPassword: true
};

function props(overrides: Record<string, unknown> = {}) {
	return {
		initial: null,
		save: vi.fn().mockResolvedValue(undefined),
		goBack: vi.fn(),
		toast: vi.fn(),
		...overrides
	};
}

async function fillRequired() {
	await page.getByLabelText('Nombre completo').fill('María López');
	await page.getByLabelText('Nombre fiscal').fill('Frutas López S.L.');
	await page.getByLabelText('NIF').fill('B87654321');
	await page.getByLabelText('Dirección').fill('Mercavalencia, nave 3');
	await page.getByLabelText('Email').fill('facturas@frutaslopez.es');
}

describe('ProfileRecord', () => {
	it('has the two blocks with their fields in Enter order', async () => {
		await render(ProfileRecordHarness, props());
		await expect.element(page.getByRole('group', { name: 'Datos fiscales' })).toBeVisible();
		await expect.element(page.getByRole('group', { name: 'Correo' })).toBeVisible();
		await expect
			.element(page.getByText('Déjalos vacíos con Gmail, Outlook, Yahoo o iCloud.'))
			.toBeVisible();
		const ids = Array.from(document.querySelectorAll('form input, input')).map((i) => i.id);
		expect(ids).toEqual([
			'fullName',
			'fiscalName',
			'nif',
			'address',
			'phone',
			'email',
			'mailPassword',
			'smtpHost',
			'smtpPort'
		]);
	});

	it('caps the lengths as the API does', async () => {
		await render(ProfileRecordHarness, props());
		const caps = { fullName: 255, nif: 20, address: 500, phone: 20, email: 50, mailPassword: 255 };
		for (const [id, max] of Object.entries(caps)) {
			await expect
				.element(page.elementLocator(document.getElementById(id)!))
				.toHaveAttribute('maxlength', String(max));
		}
	});

	it('keeps the browser from autofilling the mail password', async () => {
		await render(ProfileRecordHarness, props());
		await expect
			.element(page.getByLabelText('Contraseña de aplicación'))
			.toHaveAttribute('autocomplete', 'new-password');
		await expect.element(page.getByLabelText('Email')).toHaveAttribute('autocomplete', 'off');
	});

	it('does not save without the required data', async () => {
		const p = props();
		await render(ProfileRecordHarness, p);
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Este dato es obligatorio').first()).toBeVisible();
		expect(p.save).not.toHaveBeenCalled();
	});

	it('refuses a wrong email and a port out of range', async () => {
		const p = props();
		await render(ProfileRecordHarness, p);
		await fillRequired();
		await page.getByLabelText('Email').fill('pepe@');
		await page.getByLabelText('Puerto').fill('70000');
		await userEvent.keyboard('{NumpadAdd}');
		await expect.element(page.getByText('Revisa el email')).toBeVisible();
		await expect.element(page.getByText('Tiene que estar entre 1 y 65535')).toBeVisible();
		expect(p.save).not.toHaveBeenCalled();
	});

	it('without a profile creates one, with optional empties as null', async () => {
		const p = props();
		await render(ProfileRecordHarness, p);
		await fillRequired();
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(p.goBack).toHaveBeenCalled());
		expect(p.save).toHaveBeenCalledWith(
			{
				fullName: 'María López',
				fiscalName: 'Frutas López S.L.',
				nif: 'B87654321',
				address: 'Mercavalencia, nave 3',
				email: 'facturas@frutaslopez.es',
				phone: null,
				smtpHost: null,
				smtpPort: null,
				mailPassword: null
			},
			false
		);
		expect(p.toast).toHaveBeenCalledWith('Datos del titular guardados');
	});

	it('with a profile updates it, and the stored password is kept when left empty', async () => {
		const p = props({ initial: stored });
		await render(ProfileRecordHarness, p);
		await expect.element(page.getByLabelText('Contraseña de aplicación')).toHaveValue('');
		await expect.element(page.getByText('Guardada. Déjalo vacío para conservarla.')).toBeVisible();
		await page.getByLabelText('Teléfono').fill('961234567');
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(p.goBack).toHaveBeenCalled());
		expect(p.save).toHaveBeenCalledWith(
			expect.objectContaining({ phone: '961234567', mailPassword: null }),
			true
		);
	});

	it('sends the password when one is typed', async () => {
		const p = props({ initial: stored });
		await render(ProfileRecordHarness, p);
		const password = page.getByLabelText('Contraseña de aplicación');
		await expect.element(password).toHaveAttribute('type', 'password');
		await password.fill('abcd efgh');
		await page.getByLabelText('Servidor').fill('smtp.ejemplo.es');
		await page.getByLabelText('Puerto').fill('587');
		await userEvent.keyboard('{NumpadAdd}');
		await vi.waitFor(() => expect(p.goBack).toHaveBeenCalled());
		expect(p.save).toHaveBeenCalledWith(
			expect.objectContaining({
				mailPassword: 'abcd efgh',
				smtpHost: 'smtp.ejemplo.es',
				smtpPort: 587
			}),
			true
		);
	});

	it('shows no note about a password when none is stored', async () => {
		await render(ProfileRecordHarness, props({ initial: { ...stored, hasMailPassword: false } }));
		await expect
			.element(page.getByText('Guardada. Déjalo vacío para conservarla.'))
			.not.toBeInTheDocument();
	});
});
