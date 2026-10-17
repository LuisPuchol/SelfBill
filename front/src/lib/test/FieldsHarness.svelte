<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import DigitsField from '../components/DigitsField.svelte';
	import DateField from '../components/DateField.svelte';
	import ChoiceField from '../components/ChoiceField.svelte';

	import { todayIso } from '../format/date';

	type Format = 'PLAIN' | 'VALUED';

	let lot = $state('');
	let date = $state(todayIso());
	let format = $state<Format | null>(null);
	let last = $state('');
</script>

<KeyboardRoot />
<Form onLast={() => {}}>
	<DigitsField label="Lote" name="traceabilityCode1" length={8} bind:value={lot} required />
	<DateField label="Fecha" name="date" bind:value={date} />
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
	<TextField label="Fin" name="last" bind:value={last} />
</Form>
<output data-testid="lot">{lot}</output>
<output data-testid="date">{date}</output>
<button type="button" onclick={() => (date = '2026-01-01')}>Poner 01/01/2026</button>
<output data-testid="format">{String(format)}</output>
