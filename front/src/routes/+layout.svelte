<script lang="ts">
	import './layout.css';
	import favicon from '#lib/assets/favicon.svg';
	import KeyboardRoot from '#lib/keyboard/KeyboardRoot.svelte';
	import KeyBar from '#lib/components/KeyBar.svelte';
	import TopBar from '#lib/components/TopBar.svelte';
	import Toaster from '#lib/components/Toaster.svelte';
	import ErrorDialog from '#lib/components/ErrorDialog.svelte';
	import ConnectionScreen from '#lib/components/ConnectionScreen.svelte';
	import { errorState, showError } from '#lib/components/errors.svelte.js';
	import { invalidateAll } from '$app/navigation';
	import { onMount } from 'svelte';
	import type { LayoutProps } from './$types';

	let { children }: LayoutProps = $props();

	// Any rejection nobody handled (a failed action, a lookup...) ends up on screen.
	onMount(() => {
		const onRejection = (event: PromiseRejectionEvent) => {
			event.preventDefault();
			showError(event.reason);
		};
		window.addEventListener('unhandledrejection', onRejection);
		return () => window.removeEventListener('unhandledrejection', onRejection);
	});
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
<Toaster />
<ErrorDialog />
{#if errorState.connectionLost}
	<ConnectionScreen onRestored={invalidateAll} />
{/if}
<KeyBar />
