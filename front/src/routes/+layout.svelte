<script lang="ts">
	import './layout.css';
	import favicon from '#lib/assets/favicon.svg';
	import KeyboardRoot from '#lib/keyboard/KeyboardRoot.svelte';
	import KeyBar from '#lib/components/KeyBar.svelte';
	import TopBar from '#lib/components/TopBar.svelte';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();
</script>

<svelte:head>
	<title>SelfBill</title>
	<link rel="icon" href={favicon} />
</svelte:head>

<KeyboardRoot />
<TopBar />
<!-- bottom padding keeps content clear of the fixed key bar -->
<main class="p-6 pb-16">
	{@render children()}
</main>
<KeyBar />
