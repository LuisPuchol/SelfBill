<script lang="ts">
	import { formatMoney } from '../format/money';
	import { formatPercent } from '../format/number';
	import type { LineValues } from './form';
	import { totals, type Rates } from './totals';

	let {
		lines,
		rates,
		notice
	}: {
		lines: readonly LineValues[];
		/** Null while they are not known (no client yet, or no VAT for the date): only the base shows */
		rates: Rates | null;
		/** Said in place of the taxes, e.g. that no VAT covers the date */
		notice?: string;
	} = $props();

	// A line counts once it has both quantity and price.
	const priced = $derived(
		lines.flatMap((line) =>
			line.quantity !== null && line.price !== null
				? [{ quantity: line.quantity, price: line.price }]
				: []
		)
	);
	const sums = $derived(totals(priced, rates ?? { vat: 0, surcharge: 0 }));
	const money = (cents: number) => formatMoney(cents / 100);
	const row = 'flex items-baseline justify-between gap-8';
</script>

<!-- In the header, beside Cliente and Fecha: it stays where it is however many lines the note has -->
<section
	aria-label="Totales"
	class="min-w-72 rounded border-2 border-line bg-surface px-4 py-2 text-lg"
>
	<dl class="flex flex-col gap-0.5">
		<div class={row}>
			<dt class="text-muted">Base</dt>
			<dd class="tabular">{money(sums.baseCents)}</dd>
		</div>
		{#if rates}
			<div class={row}>
				<dt class="text-muted">IVA ({formatPercent(rates.vat)})</dt>
				<dd class="tabular">{money(sums.vatCents)}</dd>
			</div>
			<div class={row}>
				<dt class="text-muted">Recargo ({formatPercent(rates.surcharge)})</dt>
				<dd class="tabular">{money(sums.surchargeCents)}</dd>
			</div>
			<div class="{row} mt-1 border-t border-line pt-1">
				<dt class="font-semibold">Total</dt>
				<dd class="tabular font-semibold">{money(sums.totalCents)}</dd>
			</div>
		{/if}
	</dl>
	{#if !rates && notice}
		<p class="mt-1 max-w-72 text-base text-danger">{notice}</p>
	{/if}
</section>
