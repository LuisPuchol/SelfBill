<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { useKeys } from '#lib/keyboard/keymap.svelte.js';

	useKeys([{ key: 'Escape', label: 'Volver', run: () => goto(resolve('/invoices')) }]);
</script>

<h1 class="text-3xl font-semibold">Libro de facturas</h1>
<p class="mt-4 text-muted">Esta sección todavía no está hecha.</p>
