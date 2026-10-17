<script lang="ts">
	import { onMount } from 'svelte';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { errorState } from './errors.svelte';

	let {
		ping = defaultPing,
		intervalMs = 3000,
		onRestored,
		onBack
	}: {
		ping?: () => Promise<boolean>;
		intervalMs?: number;
		onRestored?: () => void;
		onBack?: () => void;
	} = $props();

	// Any HTTP answer means the API is up.
	async function defaultPing(): Promise<boolean> {
		try {
			// A timeout counts as not connected, so a hanging API can't leave a ping pending forever.
			await fetch('/api/articles/next-code', { signal: AbortSignal.timeout(5000) });
			return true;
		} catch {
			return false;
		}
	}

	let busy = false;
	let alive = true;

	async function retry() {
		if (busy) return;
		busy = true;
		try {
			if (alive && (await ping())) {
				alive = false;
				errorState.connectionLost = false;
				onRestored?.();
			}
		} finally {
			busy = false;
		}
	}

	useKeys(
		() => [
			{ key: 'Enter', label: 'Reintentar', run: retry },
			// The modal layer shadows the page's own keys, so going back is declared here.
			{ key: 'Escape', label: 'Volver', run: () => onBack?.(), when: () => !!onBack }
		],
		{ modal: true }
	);

	onMount(() => {
		const timer = setInterval(retry, intervalMs);
		return () => {
			alive = false;
			clearInterval(timer);
		};
	});
</script>

<div
	class="fixed inset-0 z-50 flex flex-col items-center justify-center gap-4 bg-canvas p-6 text-center"
	role="alert"
>
	<h1 class="text-2xl font-semibold text-ink">No se puede conectar con SelfBill</h1>
	<p class="text-muted">Comprueba que el programa sigue abierto. Reintentando…</p>
</div>
