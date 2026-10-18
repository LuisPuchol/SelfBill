<script lang="ts">
	import type { Snippet } from 'svelte';
	import { ApiError } from '../api/errors';
	import type { components } from '../api/schema';
	import DateField from '../components/DateField.svelte';
	import EditScreen from '../components/EditScreen.svelte';
	import NumberField from '../components/NumberField.svelte';
	import { formatDate } from '../format/date';
	import { FormState } from '../records/form-state.svelte';

	type TaxConfigResponse = components['schemas']['TaxConfigResponse'];
	interface Values {
		validFrom: string;
		vatPercentage: number | null;
		surchargePercentage: number | null;
	}
	type Body = { validFrom: string; vatPercentage: number; surchargePercentage: number };

	let {
		mode,
		initial,
		create,
		update,
		goToList,
		toast,
		guard
	}: {
		mode: 'new' | 'edit';
		initial: Values;
		create: (body: Body) => Promise<TaxConfigResponse>;
		update: (body: Body) => Promise<TaxConfigResponse>;
		goToList: (savedId?: number) => void | Promise<void>;
		toast: (message: string) => void;
		/** The page's unsaved-changes guard, which needs the form's state */
		guard?: Snippet<[FormState<Values>, () => Promise<boolean>]>;
	} = $props();

	const RANGE = 'Tiene que estar entre 0 y 100';
	const inRange = (v: number) => (v > 100 ? RANGE : undefined);

	// The form opens on the values it is given; the page loads again to change them.
	// svelte-ignore state_referenced_locally
	const form = new FormState<Values>(initial);
	// svelte-ignore state_referenced_locally
	const validFrom = initial.validFrom;

	async function persist(): Promise<boolean> {
		const v = form.snapshot();
		const body: Body = {
			validFrom: v.validFrom,
			vatPercentage: v.vatPercentage!,
			surchargePercentage: v.surchargePercentage!
		};
		try {
			const saved = await (mode === 'new' ? create(body) : update(body));
			form.markSaved();
			toast(`Tipo desde el ${formatDate(body.validFrom)} guardado`);
			await goToList(saved.id);
			return true;
		} catch (err) {
			if (err instanceof ApiError && err.code === 'TAX_CONFIG_ALREADY_EXISTS') {
				form.setFieldErrors({
					validFrom: 'Ya hay un tipo que empieza ese día: corrígelo desde la lista.'
				});
				return false;
			}
			if (form.showFieldErrors(err, { vatPercentage: RANGE, surchargePercentage: RANGE })) {
				return false;
			}
			throw err;
		}
	}

	const save = () => form.save(persist);
</script>

<EditScreen
	{form}
	title={mode === 'new' ? 'Tipo nuevo' : `Tipo desde el ${formatDate(validFrom)}`}
	{save}
	exit={() => void goToList()}
>
	{#if mode === 'new'}
		<DateField
			label="Desde"
			name="validFrom"
			required
			bind:value={form.values.validFrom}
			error={form.errorFor('validFrom')}
		/>
	{/if}
	<NumberField
		label="IVA %"
		name="vatPercentage"
		required
		validate={inRange}
		bind:value={form.values.vatPercentage}
		error={form.errorFor('vatPercentage')}
	/>
	<NumberField
		label="Recargo %"
		name="surchargePercentage"
		required
		validate={inRange}
		bind:value={form.values.surchargePercentage}
		error={form.errorFor('surchargePercentage')}
	/>
	{#if mode === 'edit'}
		<p class="text-muted">
			Los albaranes y facturas ya hechos conservan el tipo con el que se valoraron.
		</p>
	{/if}
</EditScreen>
{@render guard?.(form, save)}
