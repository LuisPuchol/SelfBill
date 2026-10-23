<script lang="ts" generics="T">
	import { fillViewport } from './fill-viewport';
	import type { Column } from './table';

	// The look of `DataTable` without its keys or its selection: for a panel beside a `DataTable`,
	// which owns the arrows of the screen.
	let {
		columns,
		rows,
		emptyText = 'No hay nada que mostrar.',
		scroll = false
	}: {
		columns: Column<T>[];
		rows: T[];
		emptyText?: string;
		/** Fill the window below the table's top and scroll inside, the titles staying on top */
		scroll?: boolean;
	} = $props();

	// Under the scroll box's top edge the row border would scroll away, so a shadow draws the line.
	const stuck = 'sticky top-0 z-10 bg-surface shadow-[0_1px_0_var(--color-line)]';
</script>

{#snippet table()}
	<table class="w-full border-collapse bg-surface text-ink">
		<thead>
			<tr class="border-b border-line text-muted">
				{#each columns as column (column.label)}
					<th
						scope="col"
						class="px-3 py-2 font-medium {column.align === 'right'
							? 'text-right'
							: 'text-left'} {scroll ? stuck : ''}"
					>
						{column.label}
					</th>
				{/each}
			</tr>
		</thead>
		<tbody>
			{#each rows as row, i (i)}
				<tr class="border-b border-line">
					{#each columns as column (column.label)}
						<td class="px-3 py-2 {column.align === 'right' ? 'tabular text-right' : 'text-left'}">
							{column.value(row)}
						</td>
					{/each}
				</tr>
			{/each}
		</tbody>
	</table>
{/snippet}

{#if rows.length === 0}
	<p class="p-6 text-muted">{emptyText}</p>
{:else if scroll}
	<div class="overflow-y-auto" use:fillViewport>
		{@render table()}
	</div>
{:else}
	{@render table()}
{/if}
