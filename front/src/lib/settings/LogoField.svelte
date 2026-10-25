<script lang="ts">
	import { errorMessages } from '../api/messages';
	import { showError, showMessage } from '../components/errors.svelte';

	/** The API takes up to 2 MB; saying so here spares sending a file it will refuse. */
	const MAX_BYTES = 2 * 1024 * 1024;

	let {
		exists,
		hasLogo,
		src,
		upload,
		toast
	}: {
		/** The logo hangs from the profile: without one there is nothing to upload to */
		exists: boolean;
		hasLogo: boolean;
		/** Where the logo is served from */
		src: string;
		upload: (file: File) => Promise<void>;
		toast: (message: string) => void;
	} = $props();

	// The block lives as long as the screen: what the page read is where it starts.
	// svelte-ignore state_referenced_locally
	let has = $state(hasLogo);
	// Changes after each upload, so the browser asks for the new image.
	let version = $state(0);
	let input: HTMLInputElement | undefined = $state();

	async function choose() {
		const file = input?.files?.[0];
		if (!file) return;
		try {
			if (file.size > MAX_BYTES) {
				showMessage(errorMessages.USER_PROFILE_LOGO_TOO_LARGE);
				return;
			}
			try {
				await upload(file);
			} catch (err) {
				showError(err);
				return;
			}
			has = true;
			version++;
			toast('Logo guardado');
		} finally {
			// So that choosing the same file again fires a change.
			if (input) input.value = '';
		}
	}
</script>

<!-- Not a form field: it saves on its own and takes no part in Enter's path. -->
{#if !exists}
	<p class="text-muted">Guarda primero los datos del titular para poder subir el logo.</p>
{:else}
	<div class="flex items-center gap-4">
		{#if has}
			<img
				alt="Logo"
				src="{src}?v={version}"
				class="max-h-24 max-w-64 rounded border-2 border-line bg-surface p-1"
			/>
		{:else}
			<p class="text-muted">Sin logo</p>
		{/if}
		<button
			type="button"
			onclick={() => input?.click()}
			class="h-12 rounded border-2 border-line bg-surface px-4 text-lg text-ink hover:bg-canvas focus:border-focus"
		>
			Cambiar logo…
		</button>
		<input
			bind:this={input}
			type="file"
			accept="image/jpeg,image/png"
			onchange={choose}
			tabindex="-1"
			aria-hidden="true"
			class="sr-only"
		/>
	</div>
{/if}
