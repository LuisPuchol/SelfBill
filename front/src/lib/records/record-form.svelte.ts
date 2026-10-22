import { tick } from 'svelte';
import { ApiError, type ErrorCode } from '../api/errors';
import { showError } from '../components/errors.svelte';
import { DeleteFlow } from './delete-flow.svelte';
import { FormState } from './form-state.svelte';

export interface Saved {
	id: number;
	code: number;
}

export interface RecordFormOptions<V extends { code: number | null }> {
	/**
	 * e.g. `{ one: 'cliente', Label: 'Cliente', duplicateCode: 'CLIENT_DUPLICATE_CODE' }`. Without
	 * `duplicateCode` (no code field to put it under) a repeated code goes to the error dialog.
	 */
	entity: { one: string; Label: string; duplicateCode?: ErrorCode };
	/** Null for a new record */
	id: number | null;
	initial: V;
	toPayload(values: V): unknown;
	/** The record's name, shown in the delete question */
	describe(values: V): string;
	create(payload: unknown): Promise<Saved>;
	update(id: number, payload: unknown): Promise<Saved>;
	remove(id: number): Promise<void>;
	/** The next new record, with the next code */
	blank(): Promise<V>;
	/** The body of the delete question, or null when nothing depends on the record */
	pendingForDelete(id: number): Promise<string | null>;
	/** A question to answer before saving, or null to save straight away */
	confirmBeforeSave?(values: V, original: V): Promise<string | null>;
	/** Spanish texts for the API's field errors, by field path */
	fieldMessages?: Record<string, string>;
	/** The form's field path for one of the API's, when the payload names or numbers them differently */
	mapFieldPath?(path: string): string;
	/** What the unsaved-changes check compares; all the values by default */
	comparable?(values: V): unknown;
	/** The field the cursor goes to in the next new record of a chain; `code` by default */
	firstField?: string;
	/**
	 * Runs after a successful create or update, before the toast and the chain or return. A returned
	 * text is appended to the toast; a throw goes to the error dialog and the flow goes on.
	 */
	afterSave?(saved: Saved): Promise<string | void>;
	goToList(savedId?: number): void | Promise<void>;
	toast(message: string): void;
}

/**
 * A catalog record's form: on top of `FormState`, its code, saving with an optional confirmation,
 * chained new records and deleting with a warning. `RecordScreen` renders it; the API and the
 * navigation come in through the options.
 */
export class RecordForm<V extends { code: number | null }> extends FormState<V> {
	readonly id: number | null;
	readonly deletion: DeleteFlow;
	confirmMessage: string | null = $state(null);

	readonly #options: RecordFormOptions<V>;
	/** The last record this form created, in a chain of new ones: the list selects it on the way back. */
	#createdId: number | undefined;

	constructor(options: RecordFormOptions<V>) {
		super(options.initial, { comparable: options.comparable });
		this.#options = options;
		this.id = options.id;
		this.deletion = new DeleteFlow({
			entity: options.entity,
			remove: options.remove,
			pendingForDelete: options.pendingForDelete,
			toast: options.toast,
			onDeleted: async () => {
				// Nothing left to save: the unsaved-changes question must not stop the way out.
				this.markSaved();
				await this.#options.goToList();
			}
		});
	}

	/** False when nothing was saved: invalid, waiting for a confirmation, refused or already saving. */
	override save(): Promise<boolean> {
		// The confirmation counts what a change affects through the API: FormState shows its errors.
		return super.save(async () => {
			const confirm = this.#options.confirmBeforeSave;
			if (confirm) {
				const message = await confirm(this.snapshot(), this.saved);
				if (message !== null) {
					this.confirmMessage = message;
					return false;
				}
			}
			return this.#persist();
		});
	}

	/** "Guardar igualmente": saves without asking again. */
	async confirmSave(): Promise<void> {
		await this.exclusively(() => {
			this.confirmMessage = null;
			return this.#persist();
		});
	}

	cancelConfirm(): void {
		this.confirmMessage = null;
	}

	/** Asks to delete the saved record; nothing for a new one. */
	async askDelete(): Promise<void> {
		const id = this.id;
		if (id === null) return;
		// Like the code: the saved record's, not what the form holds now.
		await this.deletion.ask({ id, code: this.#code, name: this.#options.describe(this.saved) });
	}

	exit(): void {
		void this.#options.goToList(this.#createdId);
	}

	/** The saved record's code, also when the field has been changed since. */
	get #code(): number | null {
		return this.saved.code;
	}

	async #persist(): Promise<boolean> {
		const { id } = this;
		const { entity } = this.#options;
		let saved: Saved;
		try {
			const payload = this.#options.toPayload(this.snapshot());
			saved =
				id === null ? await this.#options.create(payload) : await this.#options.update(id, payload);
		} catch (err) {
			this.#refused(err);
			return false;
		}
		this.clearFieldErrors();
		// Saved: nothing left to save, also while afterSave runs and the next new record is on its way.
		this.markSaved();
		let extra: string | void = undefined;
		try {
			extra = await this.#options.afterSave?.(saved);
		} catch (err) {
			showError(err);
		}
		this.#options.toast(`${entity.Label} ${saved.code} guardado${extra ? ` · ${extra}` : ''}`);
		if (id !== null) {
			await this.#options.goToList(id);
			return true;
		}
		this.#createdId = saved.id;
		let next: V;
		try {
			next = await this.#options.blank();
		} catch (err) {
			// Saved, but no next record to chain into: back to the list, where the new one is.
			showError(err);
			await this.#options.goToList(saved.id);
			return true;
		}
		this.reset(next);
		await tick();
		this.nav?.focus(this.#options.firstField ?? 'code');
		return true;
	}

	#refused(err: unknown): void {
		const { entity, fieldMessages, mapFieldPath } = this.#options;
		if (err instanceof ApiError && err.code === entity.duplicateCode) {
			this.setFieldErrors({ code: `Ya existe un ${entity.one} con ese código` });
			return;
		}
		// The API takes only positive codes; empty is already caught by validateAll.
		const messages = { code: 'El código tiene que ser mayor que cero', ...fieldMessages };
		if (!this.showFieldErrors(err, messages, mapFieldPath)) showError(err);
	}
}
