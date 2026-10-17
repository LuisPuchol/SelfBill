<script lang="ts" generics="T">
	import { tick } from 'svelte';
	import { on } from 'svelte/events';
	import { createSubscriber } from 'svelte/reactivity';
	import { isEditable } from '../keyboard/keys';
	import { useKeys } from '../keyboard/keymap.svelte';
	import type { Column } from './table';

	let {
		columns,
		rows,
		onOpen,
		page,
		onPage,
		selected = $bindable(0),
		emptyText = 'No hay nada que mostrar.'
	}: {
		columns: Column<T>[];
		rows: T[];
		onOpen: (row: T) => void;
		page?: { number: number; totalPages: number };
		onPage?: (n: number) => void;
		selected?: number;
		emptyText?: string;
	} = $props();

	let body: HTMLElement | undefined = $state();

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
	const hasNext = () => !!onPage && !!page && page.number + 1 < page.totalPages && notTyping();
	const hasPrevious = () => !!onPage && !!page && page.number > 0 && notTyping();

	useKeys(() => [
		{ key: 'ArrowUp', label: 'Anterior', hidden: true, when: canMove, run: () => move(-1) },
		{ key: 'ArrowDown', label: 'Siguiente', hidden: true, when: canMove, run: () => move(1) },
		{
			key: 'Enter',
			label: 'Abrir',
			when: canMove,
			run: () => {
				const row = rows[selected];
				if (row !== undefined) onOpen(row);
			}
		},
		{
			key: 'PageDown',
			label: 'Página siguiente',
			when: hasNext,
			run: () => onPage?.(page!.number + 1)
		},
		{
			key: 'PageUp',
			label: 'Página anterior',
			when: hasPrevious,
			run: () => onPage?.(page!.number - 1)
		}
	]);
</script>

{#if rows.length === 0}
	<p class="p-6 text-muted">{emptyText}</p>
{:else}
	<table class="w-full border-collapse bg-surface text-ink">
		<thead>
			<tr class="border-b border-line text-muted">
				{#each columns as column (column.label)}
					<th
						scope="col"
						class="px-3 py-2 font-medium {column.align === 'right' ? 'text-right' : 'text-left'}"
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
					onclick={() => (selected = i)}
					ondblclick={() => {
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
{/if}
{#if page && page.totalPages > 1}
	<p class="px-3 py-2 text-muted">Página {page.number + 1} de {page.totalPages}</p>
{/if}
