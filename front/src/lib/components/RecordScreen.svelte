<script lang="ts" generics="V extends { code: number | null }">
	import { onMount, tick, type Snippet } from 'svelte';
	import type { KeyAction } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { RecordForm } from '../records/record-form.svelte';
	import ActionButton from './ActionButton.svelte';
	import DeleteDialog from './DeleteDialog.svelte';
	import Dialog from './Dialog.svelte';
	import Form from './Form.svelte';
	import ScreenHeader from './ScreenHeader.svelte';

	let { form, title, children }: { form: RecordForm<V>; title: string; children: Snippet } =
		$props();

	// The fields register before this runs: the cursor starts in the first one.
	onMount(() => form.nav?.focusFirstField());

	/**
	 * "Volver a la ficha", also with Esc. The question may have opened over the unsaved-changes one
	 * (Esc, then "+"), whose buttons are gone: after the dialog's own focus return, back to the form.
	 */
	function backToForm(): void {
		form.cancelConfirm();
		void tick().then(() => form.restoreFocus());
	}

	// The keys and the title-line buttons share these actions.
	const save: KeyAction = {
		key: 'NumpadAdd',
		label: 'Guardar',
		run: async () => {
			await form.save();
		}
	};
	const remove: KeyAction = {
		key: 'F8',
		label: 'Borrar',
		when: () => form.id !== null,
		run: () => form.askDelete()
	};
	const back: KeyAction = { key: 'Escape', label: 'Volver', run: () => form.exit() };

	useKeys(() => [save, remove, back]);
</script>

<ScreenHeader {title}>
	<ActionButton action={save} variant="primary" />
	<ActionButton action={remove} variant="danger" />
	<ActionButton action={back} />
</ScreenHeader>
<Form bind:nav={form.nav} onLast={() => form.nav?.focusFirstField()}>
	{@render children()}
</Form>

<DeleteDialog flow={form.deletion} />

<Dialog
	bind:open={
		() => form.confirmMessage !== null,
		(open) => {
			if (!open) form.cancelConfirm();
		}
	}
	title={form.confirmMessage ?? ''}
	actions={[
		{ key: 'Enter', label: 'Volver a la ficha', run: backToForm },
		{ key: 'Escape', label: 'Volver a la ficha', hidden: true, run: backToForm },
		{ key: 'NumpadAdd', label: 'Guardar igualmente', run: () => form.confirmSave() }
	]}
/>
