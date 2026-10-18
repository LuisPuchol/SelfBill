<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { components } from '../api/schema';
	import { EMAIL_PATTERN } from '../catalog/client-form';
	import EditScreen from '../components/EditScreen.svelte';
	import NumberField from '../components/NumberField.svelte';
	import TextField from '../components/TextField.svelte';
	import { FormState } from '../records/form-state.svelte';
	import { trimOrNull } from '../records/payload';

	type UserProfileRequest = components['schemas']['UserProfileRequest'];
	type UserProfileResponse = components['schemas']['UserProfileResponse'];
	interface Values {
		fullName: string;
		fiscalName: string;
		nif: string;
		address: string;
		phone: string;
		email: string;
		mailPassword: string;
		smtpHost: string;
		smtpPort: number | null;
	}

	let {
		initial,
		save: persistProfile,
		goBack,
		toast,
		guard
	}: {
		/** Null while there is no profile yet */
		initial: UserProfileResponse | null;
		/** `exists` tells a change (PUT) from a first save (POST) */
		save: (body: UserProfileRequest, exists: boolean) => Promise<void>;
		goBack: () => void | Promise<void>;
		toast: (message: string) => void;
		/** The page's unsaved-changes guard, which needs the form's state */
		guard?: Snippet<[FormState<Values>, () => Promise<boolean>]>;
	} = $props();

	// The form opens on the values it is given; the page loads again to change them.
	// svelte-ignore state_referenced_locally
	const exists = initial !== null;
	// svelte-ignore state_referenced_locally
	const hasMailPassword = initial?.hasMailPassword === true;
	// The password never comes back from the API: the field always opens empty.
	// svelte-ignore state_referenced_locally
	const form = new FormState<Values>({
		fullName: initial?.fullName ?? '',
		fiscalName: initial?.fiscalName ?? '',
		nif: initial?.nif ?? '',
		address: initial?.address ?? '',
		phone: initial?.phone ?? '',
		email: initial?.email ?? '',
		mailPassword: '',
		smtpHost: initial?.smtpHost ?? '',
		smtpPort: initial?.smtpPort ?? null
	});

	const PORT = 'Tiene que estar entre 1 y 65535';
	const checkPort = (v: number) => (v < 1 || v > 65535 ? PORT : undefined);

	async function persist(): Promise<boolean> {
		const v = form.snapshot();
		const body: UserProfileRequest = {
			fullName: v.fullName.trim(),
			fiscalName: v.fiscalName.trim(),
			nif: v.nif.trim(),
			address: v.address.trim(),
			email: v.email.trim(),
			// The API takes null for "nothing"; the generated type only models absence.
			phone: trimOrNull(v.phone) as string,
			smtpHost: trimOrNull(v.smtpHost) as string,
			smtpPort: v.smtpPort as number,
			// Null keeps the stored password.
			mailPassword: (v.mailPassword.trim() === '' ? null : v.mailPassword) as string
		};
		try {
			await persistProfile(body, exists);
		} catch (err) {
			if (form.showFieldErrors(err, { email: 'Revisa el email', smtpPort: PORT })) return false;
			throw err;
		}
		form.markSaved();
		toast('Datos del titular guardados');
		await goBack();
		return true;
	}

	const save = () => form.save(persist);
	const fieldset = 'mb-4';
	const legend = 'mb-2 text-lg font-semibold';
	// Side by side within each block; Enter follows the page order.
	const fields = 'grid gap-x-4 md:grid-cols-2 xl:grid-cols-4';
</script>

<EditScreen {form} title="Datos del titular" {save} exit={() => void goBack()}>
	<fieldset class={fieldset}>
		<legend class={legend}>Datos fiscales</legend>
		<div class={fields}>
			<TextField
				label="Nombre completo"
				name="fullName"
				required
				maxlength={255}
				bind:value={form.values.fullName}
				error={form.errorFor('fullName')}
			/>
			<TextField
				label="Nombre fiscal"
				name="fiscalName"
				required
				maxlength={255}
				bind:value={form.values.fiscalName}
				error={form.errorFor('fiscalName')}
			/>
			<TextField
				label="NIF"
				name="nif"
				required
				maxlength={20}
				bind:value={form.values.nif}
				error={form.errorFor('nif')}
			/>
			<TextField
				label="Dirección"
				name="address"
				required
				maxlength={500}
				bind:value={form.values.address}
				error={form.errorFor('address')}
			/>
			<TextField
				label="Teléfono"
				name="phone"
				maxlength={20}
				bind:value={form.values.phone}
				error={form.errorFor('phone')}
			/>
		</div>
	</fieldset>
	<fieldset class={fieldset}>
		<legend class={legend}>Correo</legend>
		<div class={fields}>
			<TextField
				label="Email"
				name="email"
				type="email"
				required
				maxlength={50}
				validate={(value) => (EMAIL_PATTERN.test(value.trim()) ? undefined : 'Revisa el email')}
				bind:value={form.values.email}
				error={form.errorFor('email')}
			/>
			<TextField
				label="Contraseña de aplicación"
				name="mailPassword"
				type="password"
				autocomplete="new-password"
				maxlength={255}
				hint={hasMailPassword ? 'Guardada. Déjalo vacío para conservarla.' : undefined}
				bind:value={form.values.mailPassword}
				error={form.errorFor('mailPassword')}
			/>
			<TextField
				label="Servidor"
				name="smtpHost"
				maxlength={255}
				bind:value={form.values.smtpHost}
				error={form.errorFor('smtpHost')}
			/>
			<NumberField
				label="Puerto"
				name="smtpPort"
				decimals={0}
				validate={checkPort}
				bind:value={form.values.smtpPort}
				error={form.errorFor('smtpPort')}
			/>
		</div>
		<p class="text-muted">Déjalos vacíos con Gmail, Outlook, Yahoo o iCloud.</p>
	</fieldset>
</EditScreen>
{@render guard?.(form, save)}
