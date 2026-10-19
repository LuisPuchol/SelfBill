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

	let {
		form,
		title,
		readOnly = false,
		canDelete = () => true,
		onLast = () => form.nav?.focusFirstField(),
		children
	}: {
		form: RecordForm<V>;
		title: string;
		/** No saving nor deleting: only looking, and the way back */
		readOnly?: boolean;
		/** Whether the saved record may be deleted now; an existing record only */
		canDelete?: () => boolean;
		/** Enter on the last field; back to the first one by default */
		onLast?: () => void;
		children: Snippet;
	} = $props();

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
		when: () => !readOnly,
		run: async () => {
			await form.save();
		}
	};
	const remove: KeyAction = {
		key: 'F8',
		label: 'Borrar',
		when: () => !readOnly && form.id !== null && canDelete(),
		run: () => form.askDelete()
	};
	const back: KeyAction = { key: 'Escape', label: 'Volver', run: () => form.exit() };

	useKeys(() => [save, remove, back]);
</script>

<ScreenHeader {title}>
	{#if !readOnly}
		<ActionButton action={save} variant="primary" />
		<ActionButton action={remove} variant="danger" />
	{/if}
	<ActionButton action={back} />
</ScreenHeader>
<Form bind:nav={form.nav} onLast={() => onLast()}>
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
