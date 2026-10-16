<script lang="ts">
	import type { Snippet } from 'svelte';
	import { FormNav, setFormNav } from '../keyboard/form-nav';
	import { useKeys } from '../keyboard/keymap.svelte';

	let { onLast, children }: { onLast?: () => void; children: Snippet } = $props();

	const nav = new FormNav({ onLast: () => onLast?.() });
	setFormNav(nav);

	useKeys([
		{ key: 'Enter', label: 'Siguiente', when: () => nav.hasFocus(), run: () => nav.next() },
		{
			key: 'Enter',
			shift: true,
			label: 'Anterior',
			hidden: true,
			when: () => nav.hasFocus(),
			run: () => nav.prev()
		}
	]);
</script>

<form novalidate onsubmit={(event) => event.preventDefault()}>
	{@render children()}
</form>
