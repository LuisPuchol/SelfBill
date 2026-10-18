<script lang="ts" generics="V extends { code: number | null }">
	import { onMount, tick, type Snippet } from 'svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { RecordForm } from '../records/record-form.svelte';
	import Dialog from './Dialog.svelte';
	import Form from './Form.svelte';

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

	useKeys(() => [
		{
			key: 'NumpadAdd',
			label: 'Guardar',
			run: async () => {
				await form.save();
			}
		},
		{ key: 'F8', label: 'Borrar', when: () => form.id !== null, run: () => form.askDelete() },
		{ key: 'Escape', label: 'Salir', run: () => form.exit() }
	]);
</script>

<h1 class="mb-4 text-2xl font-semibold">{title}</h1>
<Form bind:nav={form.nav} onLast={() => form.nav?.focusFirstField()}>
	{@render children()}
</Form>

<Dialog
	bind:open={
		() => form.deleteDialog !== null,
		(open) => {
			if (!open) form.cancelDelete();
		}
	}
	title={form.deleteDialog?.title ?? ''}
	actions={[
		{ key: 'Enter', label: 'No borrar', run: () => form.cancelDelete() },
		{ key: 'Escape', label: 'No borrar', hidden: true, run: () => form.cancelDelete() },
		{ key: 'F8', label: 'Borrar', run: () => form.confirmDelete() }
	]}
>
	{#if form.deleteDialog?.name}
		<p class="font-medium">{form.deleteDialog.name}</p>
	{/if}
	{#if form.deleteDialog?.body}
		<p class="mt-2">{form.deleteDialog.body}</p>
	{/if}
</Dialog>

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
