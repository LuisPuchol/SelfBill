<script lang="ts" generics="V extends object">
	import { onMount, type Snippet } from 'svelte';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { FormState } from '../records/form-state.svelte';
	import ActionButton from './ActionButton.svelte';
	import Form from './Form.svelte';
	import ScreenHeader from './ScreenHeader.svelte';

	let {
		form,
		title,
		save,
		exit,
		children
	}: {
		form: FormState<V>;
		title: string;
		/** e.g. `() => form.save(persist)`; the page brings the API */
		save: () => Promise<boolean>;
		exit: () => void;
		children: Snippet;
	} = $props();

	// The fields register before this runs: the cursor starts in the first one.
	onMount(() => form.nav?.focusFirstField());

	// The keys and the title-line buttons share these actions.
	const saveAction: KeyAction = {
		key: 'NumpadAdd',
		label: 'Guardar',
		run: async () => {
			await save();
		}
	};
	const backAction: KeyAction = { key: 'Escape', label: 'Volver', run: () => exit() };

	useKeys(() => [saveAction, backAction]);
</script>

<ScreenHeader {title}>
	<ActionButton action={saveAction} variant="primary" />
	<ActionButton action={backAction} />
</ScreenHeader>
<Form bind:nav={form.nav} onLast={() => form.nav?.focusFirstField()}>
	{@render children()}
</Form>
