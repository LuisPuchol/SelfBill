import { describe, expect, it } from 'vitest';
import { page } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import SetupNotice from './SetupNotice.svelte';

describe('SetupNotice', () => {
	it('shows what is missing', async () => {
		await render(SetupNotice, { status: { tax: null, anyTax: false, profile: null } });
		await expect
			.element(page.getByText('Falta configurar el IVA y los datos del titular.'))
			.toBeVisible();
	});

	it('renders nothing when everything is configured', async () => {
		const { container } = await render(SetupNotice, {
			status: { tax: { id: 1 }, anyTax: true, profile: { id: 1 } }
		});
		expect(container.querySelector('p')).toBeNull();
	});
});
