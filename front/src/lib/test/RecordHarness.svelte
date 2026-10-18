<script lang="ts">
	import KeyboardRoot from '../keyboard/KeyboardRoot.svelte';
	import KeyBar from '../components/KeyBar.svelte';
	import RecordScreen from '../components/RecordScreen.svelte';
	import NumberField from '../components/NumberField.svelte';
	import TextField from '../components/TextField.svelte';
	import { RecordForm, type Saved } from '../records/record-form.svelte';

	interface ArticleValues {
		code: number | null;
		name: string;
	}

	let {
		id = null,
		initial = { code: 101, name: '' },
		create,
		update,
		remove,
		blank,
		pendingForDelete,
		confirmBeforeSave,
		fieldMessages,
		firstField,
		readOnly,
		canDelete,
		goToList,
		toast
	}: {
		id?: number | null;
		initial?: ArticleValues;
		create: (payload: unknown) => Promise<Saved>;
		update: (id: number, payload: unknown) => Promise<Saved>;
		remove: (id: number) => Promise<void>;
		blank: () => Promise<ArticleValues>;
		pendingForDelete: (id: number) => Promise<string | null>;
		confirmBeforeSave?: (values: ArticleValues, original: ArticleValues) => Promise<string | null>;
		fieldMessages?: Record<string, string>;
		firstField?: string;
		readOnly?: boolean;
		canDelete?: () => boolean;
		goToList: (savedId?: number) => void;
		toast: (message: string) => void;
	} = $props();

	// What a page does: the options are read once, when the record opens.
	// svelte-ignore state_referenced_locally
	const form = new RecordForm<ArticleValues>({
		entity: { one: 'artículo', Label: 'Artículo', duplicateCode: 'ARTICLE_DUPLICATE_CODE' },
		id,
		initial,
		toPayload: (v) => ({ code: v.code, name: v.name.trim() }),
		describe: (v) => v.name,
		create,
		update,
		remove,
		blank,
		pendingForDelete,
		confirmBeforeSave,
		fieldMessages,
		firstField,
		goToList,
		toast
	});
</script>

<KeyboardRoot />
<RecordScreen
	{form}
	{readOnly}
	{canDelete}
	title={form.id === null ? 'Artículo nuevo' : `Artículo ${form.values.code}`}
>
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
<output data-testid="dirty">{String(form.isDirty)}</output>
<!-- What UnsavedGuard does when its question closes without leaving -->
<button type="button" onclick={() => form.restoreFocus()}>Restaurar el foco</button>
<KeyBar />
