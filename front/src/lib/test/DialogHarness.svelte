<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Dialog from '../components/Dialog.svelte';
	import { useKeys } from '../keyboard/keymap.svelte';

	let {
		onSave,
		onOk,
		withEscape = true
	}: { onSave: () => void; onOk: () => void; withEscape?: boolean } = $props();

	let open = $state(false);

	useKeys([{ key: 'NumpadAdd', label: 'Guardar', run: () => onSave() }]);
</script>

<KeyboardRoot />
<input aria-label="Nombre" />
<button onclick={() => (open = true)}>Abrir</button>
<Dialog
	bind:open
	title="Pregunta"
	actions={[
		{
			key: 'Enter',
			label: 'Vale',
			run: () => {
				onOk();
				open = false;
			}
		},
		...(withEscape
			? [
					{
						key: 'Escape',
						label: 'Cerrar',
						hidden: true,
						run: () => {
							open = false;
						}
					}
				]
			: [])
	]}
/>
<KeyBar />
