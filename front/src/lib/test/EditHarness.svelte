<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import EditScreen from '../components/EditScreen.svelte';
	import NumberField from '../components/NumberField.svelte';
	import TextField from '../components/TextField.svelte';
	import { FormState } from '../records/form-state.svelte';

	interface Values {
		name: string;
		amount: number | null;
	}

	let {
		persist,
		exit
	}: {
		/** What the page sends to the API; it gets the form to show the API's answer in it */
		persist: (form: FormState<Values>) => Promise<boolean>;
		exit: () => void;
	} = $props();

	const form = new FormState<Values>({ name: '', amount: null });
</script>

<KeyboardRoot />
<EditScreen {form} title="Ficha de prueba" save={() => form.save(() => persist(form))} {exit}>
	<TextField
		label="Nombre"
		name="name"
		required
		bind:value={form.values.name}
		error={form.errorFor('name')}
	/>
	<NumberField
		label="Importe"
		name="amount"
		bind:value={form.values.amount}
		validate={(v) => (v > 100 ? 'Tiene que estar entre 0 y 100' : undefined)}
		error={form.errorFor('amount')}
	/>
</EditScreen>
<output data-testid="dirty">{String(form.isDirty)}</output>
<KeyBar />
