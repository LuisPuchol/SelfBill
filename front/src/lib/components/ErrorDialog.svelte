<script lang="ts">
	import { errorMessages } from '../api/messages';
	import Dialog from './Dialog.svelte';
	import { dismissError, errorState } from './errors.svelte';

	const view = $derived(errorState.view);
	const shown = $derived(view !== null && view.kind !== 'connection');
</script>

<Dialog
	bind:open={() => shown, (v) => !v && dismissError()}
	title={view?.kind === 'unexpected' ? 'Error inesperado' : 'No se ha podido hacer'}
	actions={[
		{ key: 'Enter', label: 'Entendido', run: dismissError },
		{ key: 'Escape', label: 'Ocultar', hidden: true, run: dismissError }
	]}
>
	{#if view && view.kind !== 'connection'}
		<p>
			{view.lead ? `${view.lead} ` : ''}{view.kind === 'business'
				? view.message
				: errorMessages.INTERNAL_ERROR}
		</p>
		{#if view.also}
			<p class="mt-2">{view.also}</p>
		{/if}
	{/if}
	{#if view?.kind === 'unexpected'}
		<details class="mt-4 text-muted">
			<summary class="cursor-pointer">Detalles técnicos</summary>
			<p>Estado: {view.status}</p>
			<p>Código: {view.code}</p>
			<p class="break-all">Detalle: {view.detail}</p>
		</details>
	{/if}
</Dialog>
