<script lang="ts">
	import { tick } from 'svelte';
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import Dialog from '../components/Dialog.svelte';
	import Form from '../components/Form.svelte';
	import TextField from '../components/TextField.svelte';
	import { createUnsavedGuard } from '../components/unsaved';
	import type { FormNav } from '../keyboard/form-nav';
	import { useKeys } from '../keyboard/keymap.svelte';

	let { onLeave }: { onLeave: (url: URL) => void } = $props();

	let first = $state('');
	let second = $state('');
	let nav = $state<FormNav>();
	let asking = $state(false);

	// What UnsavedGuard does, without SvelteKit's navigation.
	const guard = createUnsavedGuard({
		isDirty: () => true,
		save: async () => (await nav?.validateAll()) ?? false,
		goto: async (url) => onLeave(url),
		onAsk: (value) => (asking = value),
		onClosed: () => void tick().then(() => nav?.restoreFocus())
	});

	useKeys([
		{
			key: 'Escape',
			label: 'Salir',
			run: () =>
				guard.check({ to: { url: new URL('http://localhost/') }, willUnload: false, cancel() {} })
		}
	]);
</script>

<KeyboardRoot />
<Form bind:nav>
	<TextField label="Primero" name="first" required bind:value={first} />
	<TextField label="Segundo" name="second" bind:value={second} />
</Form>
<Dialog
	bind:open={asking}
	title="Hay cambios sin guardar"
	actions={[
		{ key: 'Enter', label: 'Seguir editando', run: () => guard.keepEditing() },
		{ key: 'NumpadAdd', label: 'Guardar y salir', run: () => guard.saveAndLeave() }
	]}
/>
<KeyBar />
