<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '../api/client';
	import ChoiceField from '../components/ChoiceField.svelte';
	import DigitsField from '../components/DigitsField.svelte';
	import NumberField from '../components/NumberField.svelte';
	import RecordScreen from '../components/RecordScreen.svelte';
	import TextField from '../components/TextField.svelte';
	import { toast } from '../components/toast.svelte';
	import UnsavedGuard from '../components/UnsavedGuard.svelte';
	import { recallList, rememberList } from '../records/list-memory';
	import { RecordForm } from '../records/record-form.svelte';
	import { missingBillingData, missingBillingMessage } from './billing';
	import { blankClient, clientPayload, EMAIL_PATTERN, type ClientValues } from './client-form';
	import { clientPendingMessage, regimeChangeMessage } from './pending';

	let { id = null, initial }: { id?: number | null; initial: ClientValues } = $props();

	type Body = ReturnType<typeof clientPayload>;

	async function blank(): Promise<ClientValues> {
		const next = await request(api.GET('/api/clients/next-code'));
		return blankClient(next.code ?? null);
	}

	async function goToList(savedId?: number): Promise<void> {
		const memory = recallList('clients');
		const href = memory?.href ?? resolve('/clients');
		rememberList('clients', { href, openedId: savedId ?? memory?.openedId });
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- the remembered URL came from the list page
		await goto(href);
	}

	/** What still waits on the client: unbilled delivery notes and draft invoices. */
	async function pending(clientId: number): Promise<[number, number]> {
		const [notes, drafts] = await Promise.all([
			request(
				api.GET('/api/delivery-notes', {
					params: { query: { clientId, invoiced: false, size: 1 } }
				})
			),
			request(
				api.GET('/api/invoices', { params: { query: { clientId, status: 'DRAFT', size: 1 } } })
			)
		]);
		return [notes.page?.totalElements ?? 0, drafts.page?.totalElements ?? 0];
	}

	// What a page does: the options are read once, when the record opens.
	// svelte-ignore state_referenced_locally
	const form = new RecordForm<ClientValues>({
		entity: { one: 'cliente', Label: 'Cliente', duplicateCode: 'CLIENT_DUPLICATE_CODE' },
		id,
		initial,
		toPayload: (v) => clientPayload(v),
		describe: (v) => v.name,
		create: async (payload) => {
			const r = await request(api.POST('/api/clients', { body: payload as Body }));
			return { id: r.id!, code: r.code! };
		},
		update: async (clientId, payload) => {
			const r = await request(
				api.PUT('/api/clients/{id}', { params: { path: { id: clientId } }, body: payload as Body })
			);
			return { id: r.id!, code: r.code! };
		},
		remove: async (clientId) => {
			await request(api.DELETE('/api/clients/{id}', { params: { path: { id: clientId } } }));
		},
		blank,
		pendingForDelete: async (clientId) => {
			const [notes, drafts] = await pending(clientId);
			return clientPendingMessage(notes, drafts);
		},
		confirmBeforeSave: async (values, original) => {
			if (id === null) return null;
			if (values.vatType === original.vatType && values.surchargeType === original.surchargeType) {
				return null;
			}
			const [notes, drafts] = await pending(id);
			return regimeChangeMessage(notes, drafts);
		},
		goToList,
		toast
	});

	const missing = $derived(missingBillingMessage(missingBillingData(form.values)));
	const fieldset = 'mb-4';
	const legend = 'mb-2 text-lg font-semibold';
</script>

<RecordScreen {form} title={form.id === null ? 'Cliente nuevo' : `Cliente ${form.values.code}`}>
	{#if missing}
		<p class="mb-4 rounded border-2 border-draft bg-surface p-3 text-draft">{missing}</p>
	{/if}
	<fieldset class={fieldset}>
		<legend class={legend}>Identificación</legend>
		<NumberField
			label="Código"
			name="code"
			decimals={0}
			required
			bind:value={form.values.code}
			error={form.errorFor('code')}
		/>
		<TextField
			label="Nombre comercial"
			name="name"
			required
			maxlength={255}
			bind:value={form.values.name}
			error={form.errorFor('name')}
		/>
		<TextField
			label="Razón social"
			name="businessName"
			required
			maxlength={255}
			bind:value={form.values.businessName}
			error={form.errorFor('businessName')}
		/>
		<TextField
			label="NIF"
			name="nif"
			maxlength={20}
			bind:value={form.values.nif}
			error={form.errorFor('nif')}
		/>
	</fieldset>
	<fieldset class={fieldset}>
		<legend class={legend}>Dirección</legend>
		<TextField
			label="Dirección"
			name="address"
			maxlength={500}
			bind:value={form.values.address}
			error={form.errorFor('address')}
		/>
		<DigitsField
			label="Código postal"
			name="postalCode"
			length={5}
			bind:value={form.values.postalCode}
			error={form.errorFor('postalCode')}
		/>
		<TextField
			label="Población"
			name="city"
			maxlength={100}
			bind:value={form.values.city}
			error={form.errorFor('city')}
		/>
		<TextField
			label="Provincia"
			name="province"
			maxlength={100}
			bind:value={form.values.province}
			error={form.errorFor('province')}
		/>
	</fieldset>
	<fieldset class={fieldset}>
		<legend class={legend}>Contacto</legend>
		<TextField
			label="Teléfono"
			name="phone"
			maxlength={20}
			bind:value={form.values.phone}
			error={form.errorFor('phone')}
		/>
		<TextField
			label="Teléfono 2"
			name="phone2"
			maxlength={20}
			bind:value={form.values.phone2}
			error={form.errorFor('phone2')}
		/>
		<TextField
			label="Email"
			name="email"
			type="email"
			maxlength={50}
			validate={(value) => (EMAIL_PATTERN.test(value.trim()) ? undefined : 'Revisa el email')}
			bind:value={form.values.email}
			error={form.errorFor('email')}
		/>
	</fieldset>
	<fieldset class={fieldset}>
		<legend class={legend}>Facturación</legend>
		<ChoiceField
			label="IVA"
			name="vatType"
			required
			options={[
				{ value: 'WITH_VAT', label: 'Con IVA' },
				{ value: 'WITHOUT_VAT', label: 'Sin IVA' }
			]}
			bind:value={form.values.vatType}
			error={form.errorFor('vatType')}
		/>
		<ChoiceField
			label="Recargo"
			name="surchargeType"
			required
			options={[
				{ value: 'WITHOUT_SURCHARGE', label: 'Sin recargo' },
				{ value: 'WITH_SURCHARGE', label: 'Con recargo' }
			]}
			bind:value={form.values.surchargeType}
			error={form.errorFor('surchargeType')}
		/>
		<ChoiceField
			label="Formato de albarán"
			name="defaultDeliveryNoteFormat"
			required
			options={[
				{ value: 'PLAIN', label: 'Simple' },
				{ value: 'VALUED', label: 'Valorado' }
			]}
			bind:value={form.values.defaultDeliveryNoteFormat}
			error={form.errorFor('defaultDeliveryNoteFormat')}
		/>
	</fieldset>
</RecordScreen>
<UnsavedGuard
	isDirty={() => form.isDirty}
	save={() => form.save()}
	restoreFocus={() => form.restoreFocus()}
/>
