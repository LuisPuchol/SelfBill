import { describe, expect, it } from 'vitest';
import { page } from 'vitest/browser';
import { render } from 'vitest-browser-svelte';
import Toaster from './Toaster.svelte';
import { toast } from './toast.svelte';

describe('Toaster', () => {
	it('shows a message and hides it', async () => {
		render(Toaster);
		toast('Albarán 121 guardado', 200);
		await expect.element(page.getByRole('status')).toHaveTextContent('Albarán 121 guardado');
		await expect.element(page.getByText('Albarán 121 guardado')).not.toBeInTheDocument();
	});
});
