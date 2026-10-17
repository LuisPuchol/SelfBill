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
			console.error(event.reason);
			showError(event.reason);
		};
		// No preventDefault: the browser still logs it to the console.
		const onError = (event: ErrorEvent) => {
			if (event.error instanceof Error) showError(event.error);
		};
		window.addEventListener('unhandledrejection', onRejection);
		window.addEventListener('error', onError);
		return () => {
			window.removeEventListener('unhandledrejection', onRejection);
			window.removeEventListener('error', onError);
		};
	});
</script>

<svelte:head>
	<title>SelfBill</title>
	<link rel="icon" href={favicon} />
</svelte:head>

<KeyboardRoot />
<div inert={errorState.connectionLost}>
	<TopBar />
</div>
<!-- bottom padding keeps content clear of the fixed key bar -->
<main class="p-6 pb-16" inert={errorState.connectionLost}>
	{@render children()}
</main>
<Toaster />
<ErrorDialog />
{#if errorState.connectionLost}
	<ConnectionScreen onRestored={invalidateAll} />
{/if}
<KeyBar />
