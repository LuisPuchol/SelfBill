<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import ConnectionScreen from '#lib/components/ConnectionScreen.svelte';
	import { useKeys } from '#lib/keyboard/keymap.svelte.js';

	const back = () => goto(resolve('/'));

	useKeys([{ key: 'Escape', label: 'Volver', run: back, when: () => page.status !== 503 }]);
</script>

{#if page.status === 503}
	<ConnectionScreen onRestored={invalidateAll} onBack={back} />
{:else}
	<p class="text-xl">
		{page.status === 404
			? 'No se ha encontrado. Puede que se haya borrado.'
			: 'No se ha podido cargar esta pantalla.'}
	</p>
{/if}
