import { describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import { ApiError } from '#lib/api/errors.js';
import { errorState } from '#lib/components/errors.svelte.js';
import ErrorDialog from '#lib/components/ErrorDialog.svelte';
import LogoField from '#lib/settings/LogoField.svelte';
import KeyboardRoot from '#lib/keyboard/KeyboardRoot.svelte';

const png = () => new File([new Uint8Array([1, 2, 3])], 'logo.png', { type: 'image/png' });

function props(overrides: Record<string, unknown> = {}) {
	return {
		exists: true,
		hasLogo: false,
		src: '/api/user-profile/logo',
		upload: vi.fn().mockResolvedValue(undefined),
		toast: vi.fn(),
		...overrides
	};
}

const fileInput = () => document.querySelector('input[type=file]') as HTMLInputElement;

describe('LogoField', () => {
	it('shows the logo or "Sin logo"', async () => {
		const { unmount } = await render(LogoField, props());
		await expect.element(page.getByText('Sin logo')).toBeVisible();
		await expect.element(page.getByRole('img', { name: 'Logo' })).not.toBeInTheDocument();
		await unmount();

		await render(LogoField, props({ hasLogo: true }));
		await expect
			.element(page.getByRole('img', { name: 'Logo' }))
			.toHaveAttribute('src', '/api/user-profile/logo?v=0');
		await expect.element(page.getByText('Sin logo')).not.toBeInTheDocument();
	});

	it('uploads the chosen file and says so', async () => {
		const p = props();
		await render(LogoField, p);
		const file = png();
		await userEvent.upload(fileInput(), file);
		await vi.waitFor(() => expect(p.toast).toHaveBeenCalledWith('Logo guardado'));
		expect(p.upload).toHaveBeenCalledWith(file);
		const src = document.querySelector('img')!.getAttribute('src')!;
		expect(src.endsWith('?v=1')).toBe(true);
		expect(fileInput().value).toBe('');
	});

	it('does not send a file over 2 MB', async () => {
		const p = props();
		await render(LogoField, p);
		const big = new File([new Uint8Array(2_097_153)], 'big.png', { type: 'image/png' });
		await userEvent.upload(fileInput(), big);
		await vi.waitFor(() =>
			expect(errorState.view).toMatchObject({ message: 'El logotipo no puede pasar de 2 MB.' })
		);
		expect(p.upload).not.toHaveBeenCalled();
		expect(fileInput().value).toBe('');
		expect(p.toast).not.toHaveBeenCalled();
	});

	it('reports a wrong format', async () => {
		const p = props({
			upload: vi
				.fn()
				.mockRejectedValue(new ApiError(400, 'USER_PROFILE_INVALID_LOGO_FORMAT', 'x', {}))
		});
		await render(LogoField, p);
		await render(KeyboardRoot);
		await render(ErrorDialog);
		await userEvent.upload(fileInput(), png());
		await expect
			.element(page.getByText('El logotipo tiene que ser una imagen JPG o PNG.'))
			.toBeVisible();
		expect(p.toast).not.toHaveBeenCalled();
		expect(document.querySelector('img')).toBeNull();
		expect(fileInput().value).toBe('');
	});

	it('without a profile there is no button', async () => {
		await render(LogoField, props({ exists: false }));
		await expect
			.element(page.getByText('Guarda primero los datos del titular para poder subir el logo.'))
			.toBeVisible();
		await expect
			.element(page.getByRole('button', { name: 'Cambiar logo…' }))
			.not.toBeInTheDocument();
		expect(fileInput()).toBeNull();
	});
});
