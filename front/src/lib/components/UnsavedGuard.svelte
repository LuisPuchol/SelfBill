<script lang="ts">
	import { beforeNavigate, goto } from '$app/navigation';
	import Dialog from './Dialog.svelte';
	import { createUnsavedGuard } from './unsaved';

	let { isDirty, save }: { isDirty: () => boolean; save: () => Promise<boolean> } = $props();

	let asking = $state(false);

	const guard = createUnsavedGuard({
		isDirty: () => isDirty(),
		save: () => save(),
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- the URL comes from SvelteKit's own navigation, already resolved
		goto: (url) => goto(url),
		onAsk: (value) => (asking = value)
	});

	beforeNavigate((nav) => guard.check(nav));
</script>

<Dialog
	bind:open={asking}
	title="Hay cambios sin guardar"
	actions={[
		{ key: 'Enter', label: 'Seguir editando', run: () => guard.keepEditing() },
		{ key: 'Escape', label: 'Seguir editando', hidden: true, run: () => guard.keepEditing() },
		{ key: 'NumpadAdd', label: 'Guardar y salir', run: () => guard.saveAndLeave() },
		{ key: 'F8', label: 'Salir sin guardar', run: () => guard.discard() }
	]}
/>
