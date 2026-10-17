<script lang="ts">
	import type { Snippet } from 'svelte';
	import { FormNav, setFormNav } from '../keyboard/form-nav';
	import { useKeys } from '../keyboard/keymap.svelte';

	let {
		// eslint-disable-next-line no-useless-assignment -- set below, read by the page through bind:nav
		nav = $bindable(),
		onLast,
		children
	}: {
		/** For the page, via `bind:nav`: `if (await nav.validateAll()) save()` before saving */
		nav?: FormNav;
		onLast?: () => void;
		children: Snippet;
	} = $props();

	const formNav = new FormNav({ onLast: () => onLast?.() });
	setFormNav(formNav);
	// eslint-disable-next-line no-useless-assignment -- read by the page through bind:nav
	nav = formNav;

	useKeys([
		{
			key: 'Enter',
			label: 'Siguiente',
			when: () => formNav.hasFocus(),
			run: () => formNav.next()
		},
		{
			key: 'Enter',
			shift: true,
			label: 'Anterior',
			hidden: true,
			when: () => formNav.hasFocus(),
			run: () => formNav.prev()
		}
	]);
</script>

<form novalidate onsubmit={(event) => event.preventDefault()}>
	{@render children()}
</form>
