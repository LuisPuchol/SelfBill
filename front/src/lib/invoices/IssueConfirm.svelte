<script lang="ts" module>
	/** The question before issuing: the key that asked confirms it, as with deleting. */
	export interface IssueQuestion {
		key: 'NumpadAdd' | 'F3';
		title: string;
		lines: string[];
		confirm: () => void;
	}
</script>

<script lang="ts">
	import Dialog from '../components/Dialog.svelte';

	let { question, onCancel }: { question: IssueQuestion | null; onCancel: () => void } = $props();
</script>

<Dialog
	bind:open={
		() => question !== null,
		(open) => {
			if (!open) onCancel();
		}
	}
	title={question?.title ?? ''}
	actions={[
		{ key: 'Enter', label: 'No emitir', run: onCancel },
		{ key: 'Escape', label: 'No emitir', hidden: true, run: onCancel },
		{ key: question?.key ?? 'NumpadAdd', label: 'Emitir', run: () => question?.confirm() }
	]}
>
	{#each question?.lines ?? [] as line, i (i)}
		<p class={i > 0 ? 'mt-2' : ''}>{line}</p>
	{/each}
</Dialog>
