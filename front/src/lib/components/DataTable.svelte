<script lang="ts" generics="T">
	import { tick } from 'svelte';
	import { on } from 'svelte/events';
	import { createSubscriber } from 'svelte/reactivity';
	import { isEditable } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import { fillViewport } from './fill-viewport';
	import type { Column } from './table';

	let {
		columns,
		rows,
		onOpen,
		selected = $bindable(0),
		emptyText = 'No hay nada que mostrar.',
		openLabel = 'Abrir',
		scroll = false
	}: {
		columns: Column<T>[];
		rows: T[];
		/** Absent: the table only selects, with no Enter action and no double click */
		onOpen?: (row: T) => void;
		selected?: number;
		emptyText?: string;
		/** The label of the Enter action */
		openLabel?: string;
		/** Fill the window below the table's top and scroll inside, the titles staying on top */
		scroll?: boolean;
	} = $props();

	let body: HTMLElement | undefined = $state();
	/** The row under the first press of the latest click, null for a press elsewhere. */
	let pressedRow: Element | null = null;

	// The browser counts a press as the second of a double click by time and place alone: after a
	// dialog's button closes the dialog, it lands on the row behind. Only the row that got the first
	// press takes it. Capture: no handler can stop the press before it is seen here.
	$effect(() =>
		on(
			document,
			'mousedown',
			(e) => {
				if (e.detail < 2) pressedRow = e.target instanceof Element ? e.target.closest('tr') : null;
			},
			{ capture: true }
		)
	);

	// The selection goes back to the first row whenever the rows change.
	$effect(() => {
		void rows;
		selected = 0;
	});

	function move(delta: number) {
		if (rows.length === 0) return;
		selected = Math.min(rows.length - 1, Math.max(0, selected + delta));
		void tick().then(() =>
			body?.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest' })
		);
	}

	// `when` runs inside a $derived in the key bar: re-evaluate it as the focus moves.
	const subscribe = createSubscriber((update) => {
		const off = [on(document, 'focusin', update), on(document, 'focusout', update)];
		return () => off.forEach((remove) => remove());
	});
	// Enter belongs to a focused button or link, and arrows to a text field.
	const notTyping = () => {
		subscribe();
		const active = document.activeElement;
		return !isEditable(active) && active?.tagName !== 'BUTTON' && active?.tagName !== 'A';
	};
	const canMove = () => rows.length > 0 && notTyping();

	useKeys(() => [
		{ key: 'ArrowUp', label: 'Anterior', hidden: true, when: canMove, run: () => move(-1) },
		{ key: 'ArrowDown', label: 'Siguiente', hidden: true, when: canMove, run: () => move(1) },
		...(onOpen
			? [
					{
						key: 'Enter',
						label: openLabel,
						when: canMove,
						run: () => {
							const row = rows[selected];
							if (row !== undefined) onOpen(row);
						}
					}
				]
			: [])
	]);

	// Under the scroll box's top edge the row border would scroll away, so a shadow draws the line.
	const stuck = 'sticky top-0 z-10 bg-surface shadow-[0_1px_0_var(--color-line)]';
</script>

{#if rows.length === 0}
	<p class="p-6 text-muted">{emptyText}</p>
{:else}
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
			<tbody bind:this={body}>
				{#each rows as row, i (i)}
					<tr
						aria-selected={i === selected}
						class="cursor-default border-b border-line {i === selected ? 'bg-field-active' : ''}"
						onclick={(e) => {
							if (e.detail < 2 || e.currentTarget === pressedRow) selected = i;
						}}
						ondblclick={(e) => {
							if (!onOpen || e.currentTarget !== pressedRow) return;
							selected = i;
							onOpen(row);
						}}
					>
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
	{#if scroll}
		<div class="overflow-y-auto" use:fillViewport>
			{@render table()}
		</div>
	{:else}
		{@render table()}
	{/if}
{/if}
