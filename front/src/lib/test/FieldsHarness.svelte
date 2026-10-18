<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import DigitsField from '../components/DigitsField.svelte';
	import DateField from '../components/DateField.svelte';
	import NumberField from '../components/NumberField.svelte';
	import ChoiceField from '../components/ChoiceField.svelte';

	import { todayIso } from '../format/date';
	import type { FormNav } from '../keyboard/form-nav';

	type Format = 'PLAIN' | 'VALUED';

	let { compact = false }: { compact?: boolean } = $props();

	let nav = $state<FormNav>();
	let lot = $state('');
	let date = $state(todayIso());
	let format = $state<Format | null>(null);
	let last = $state('');
	let whole = $state<number | null>(null);
	let percent = $state<number | null>(null);
</script>

<KeyboardRoot />
<Form bind:nav onLast={() => {}}>
	<DigitsField label="Lote" name="traceabilityCode1" length={8} bind:value={lot} required />
	<DateField label="Fecha" name="date" bind:value={date} required />
	<ChoiceField
		label="Formato"
		name="format"
		options={[
			{ value: 'PLAIN', label: 'Simple' },
			{ value: 'VALUED', label: 'Valorado' }
		]}
		bind:value={format}
		required
	/>
	<TextField label="Fin" name="last" bind:value={last} {compact} required={compact} />
	<NumberField label="Entero" name="whole" decimals={0} bind:value={whole} />
	<NumberField
		label="Porcentaje"
		name="percent"
		bind:value={percent}
		validate={(v) => (v > 100 ? 'Tiene que estar entre 0 y 100' : undefined)}
	/>
</Form>
<output data-testid="lot">{lot}</output>
<output data-testid="date">{date}</output>
<button type="button" onclick={() => (date = '2026-01-01')}>Poner 01/01/2026</button>
<output data-testid="format">{String(format)}</output>
<!-- What a screen does before saving -->
<button type="button" onclick={() => void nav?.validateAll()}>Validar todo</button>
