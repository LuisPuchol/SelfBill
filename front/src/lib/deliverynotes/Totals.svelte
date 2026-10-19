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
	const item = 'flex items-baseline gap-2';
</script>

<!-- Always in sight, just above the key bar, however long the note is -->
<section
	aria-label="Totales"
	class="sticky bottom-12 mt-4 flex flex-wrap items-baseline justify-end gap-x-8 gap-y-1 border-t border-line bg-canvas py-3 text-lg"
>
	<dl class="flex flex-wrap gap-x-8">
		<div class={item}>
			<dt class="text-muted">Base</dt>
			<dd class="tabular">{money(sums.baseCents)}</dd>
		</div>
		{#if rates}
			<div class={item}>
				<dt class="text-muted">IVA ({formatPercent(rates.vat)})</dt>
				<dd class="tabular">{money(sums.vatCents)}</dd>
			</div>
			<div class={item}>
				<dt class="text-muted">Recargo ({formatPercent(rates.surcharge)})</dt>
				<dd class="tabular">{money(sums.surchargeCents)}</dd>
			</div>
			<div class={item}>
				<dt class="font-semibold">Total</dt>
				<dd class="tabular font-semibold">{money(sums.totalCents)}</dd>
			</div>
		{/if}
	</dl>
	{#if !rates && notice}
		<p class="text-danger">{notice}</p>
	{/if}
</section>
