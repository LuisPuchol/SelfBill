<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { api, request } from '../api/client';
	import NumberField from '../components/NumberField.svelte';
	import RecordScreen from '../components/RecordScreen.svelte';
	import TextField from '../components/TextField.svelte';
	import { toast } from '../components/toast.svelte';
	import UnsavedGuard from '../components/UnsavedGuard.svelte';
	import { recallList, rememberList } from '../records/list-memory';
	import { RecordForm } from '../records/record-form.svelte';
	import { articlePayload, type ArticleValues } from './article-form';
	import { articlePendingMessage } from './pending';

	let { id = null, initial }: { id?: number | null; initial: ArticleValues } = $props();

	type Body = { code: number; name: string };

	async function blank(): Promise<ArticleValues> {
		const next = await request(api.GET('/api/articles/next-code'));
		return { code: next.code ?? null, name: '' };
	}

	async function goToList(savedId?: number): Promise<void> {
		const memory = recallList('articles');
		const href = memory?.href ?? resolve('/articles');
		rememberList('articles', { href, openedId: savedId ?? memory?.openedId });
		// eslint-disable-next-line svelte/no-navigation-without-resolve -- the remembered URL came from the list page
		await goto(href);
	}

	// What a page does: the options are read once, when the record opens.
	// svelte-ignore state_referenced_locally
	const form = new RecordForm<ArticleValues>({
		entity: { one: 'artículo', Label: 'Artículo', duplicateCode: 'ARTICLE_DUPLICATE_CODE' },
		id,
		initial,
		toPayload: (v) => articlePayload(v),
		describe: (v) => v.name,
		create: async (payload) => {
			const r = await request(api.POST('/api/articles', { body: payload as Body }));
			return { id: r.id!, code: r.code! };
		},
		update: async (articleId, payload) => {
			const r = await request(
				api.PUT('/api/articles/{id}', {
					params: { path: { id: articleId } },
					body: payload as Body
				})
			);
			return { id: r.id!, code: r.code! };
		},
		remove: async (articleId) => {
			await request(api.DELETE('/api/articles/{id}', { params: { path: { id: articleId } } }));
		},
		blank,
		pendingForDelete: async (articleId) => {
			const notes = await request(
				api.GET('/api/delivery-notes', {
					params: { query: { articleId, invoiced: false, size: 1 } }
				})
			);
			return articlePendingMessage(notes.page?.totalElements ?? 0);
		},
		goToList,
		toast
	});
</script>

<RecordScreen {form} title={form.id === null ? 'Artículo nuevo' : `Artículo ${form.values.code}`}>
	<NumberField
		label="Código"
		name="code"
		decimals={0}
		required
		bind:value={form.values.code}
		error={form.errorFor('code')}
	/>
	<TextField
		label="Nombre"
		name="name"
		required
		bind:value={form.values.name}
		error={form.errorFor('name')}
	/>
</RecordScreen>
<UnsavedGuard
	isDirty={() => form.isDirty}
	save={() => form.save()}
	restoreFocus={() => form.restoreFocus()}
/>
