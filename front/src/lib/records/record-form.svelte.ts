import { tick } from 'svelte';
import { ApiError, type ErrorCode } from '../api/errors';
import { fieldErrorMessages } from '../api/field-errors';
import { showError } from '../components/errors.svelte';
import { DeleteFlow } from './delete-flow.svelte';
import type { FormNav } from '../keyboard/form-nav';

export interface Saved {
	id: number;
	code: number;
}

export interface RecordFormOptions<V extends { code: number | null }> {
	/** e.g. `{ one: 'cliente', Label: 'Cliente', duplicateCode: 'CLIENT_DUPLICATE_CODE' }` */
	entity: { one: string; Label: string; duplicateCode: ErrorCode };
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
	goToList(savedId?: number): void | Promise<void>;
	toast(message: string): void;
}

/** An error the API put on a field, and the field's value when it arrived. */
interface FieldError {
	message: string;
	value: string | undefined;
}

/** The value at a field path such as `name` or `lines[2].quantity`. */
function valueAt(values: unknown, path: string): unknown {
	return path
		.split(/[.[\]]/)
		.filter((key) => key !== '')
		.reduce<unknown>((value, key) => (value as Record<string, unknown> | undefined)?.[key], values);
}

function same(a: unknown, b: unknown): boolean {
	return JSON.stringify(a) === JSON.stringify(b);
}

/**
 * State and actions of a record form: values and their original copy, the API's field errors,
 * the lock while saving, saving with validation, chained new records and deleting with a warning.
 * `RecordScreen` renders it; the API and the navigation come in through the options.
 */
export class RecordForm<V extends { code: number | null }> {
	values: V;
	/** Set by RecordScreen through `bind:nav` */
	nav: FormNav | undefined = $state.raw();
	readonly id: number | null;
	readonly deletion: DeleteFlow;
	confirmMessage: string | null = $state(null);

	readonly #options: RecordFormOptions<V>;
	#original: V;
	#fieldErrors: Record<string, FieldError> = $state.raw({});
	#saving = false;
	/** The last record this form created, in a chain of new ones: the list selects it on the way back. */
	#createdId: number | undefined;

	constructor(options: RecordFormOptions<V>) {
		this.#options = options;
		this.id = options.id;
		this.#original = $state.raw($state.snapshot(options.initial) as V);
		// A copy of its own: the fields write here, never into the caller's object.
		this.values = $state($state.snapshot(options.initial) as V);
		this.deletion = new DeleteFlow({
			entity: options.entity,
			remove: options.remove,
			pendingForDelete: options.pendingForDelete,
			toast: options.toast,
			onDeleted: async () => {
				// Nothing left to save: the unsaved-changes question must not stop the way out.
				this.#original = this.#snapshot();
				await this.#options.goToList();
			}
		});
	}

	get isDirty(): boolean {
		return !same(this.values, this.#original);
	}

	/** The API's error for a field, while the field keeps the value it had when the error came. */
	errorFor(name: string): string | undefined {
		const error = this.#fieldErrors[name];
		if (!error) return undefined;
		return JSON.stringify(valueAt(this.values, name)) === error.value ? error.message : undefined;
	}

	/** False when nothing was saved: invalid, waiting for a confirmation, refused or already saving. */
	async save(): Promise<boolean> {
		if (this.#saving) return false;
		this.#saving = true;
		try {
			if (!(await this.nav?.validateAll())) return false;
			const confirm = this.#options.confirmBeforeSave;
			if (confirm) {
				const message = await confirm(this.#snapshot(), this.#original);
				if (message !== null) {
					this.confirmMessage = message;
					return false;
				}
			}
			return await this.#persist();
		} catch (err) {
			// Counting what a change affects goes to the API too.
			showError(err);
			return false;
		} finally {
			this.#saving = false;
		}
	}

	/** "Guardar igualmente": saves without asking again. */
	async confirmSave(): Promise<void> {
		if (this.#saving) return;
		this.confirmMessage = null;
		this.#saving = true;
		try {
			await this.#persist();
		} catch (err) {
			showError(err);
		} finally {
			this.#saving = false;
		}
	}

	cancelConfirm(): void {
		this.confirmMessage = null;
	}

	/** Asks to delete the saved record; nothing for a new one. */
	async askDelete(): Promise<void> {
		const id = this.id;
		if (id === null) return;
		// Like the code: the saved record's, not what the form holds now.
		await this.deletion.ask({ id, code: this.#code, name: this.#options.describe(this.#original) });
	}

	exit(): void {
		void this.#options.goToList(this.#createdId);
	}

	/** Back to the first field the API refused; with none, to where the form had the cursor. */
	restoreFocus(): void {
		const refused = Object.keys(this.#fieldErrors).filter((name) => this.errorFor(name));
		if (refused.length > 0) this.nav?.focusFirst(refused);
		else this.nav?.restoreFocus();
	}

	/** The saved record's code, also when the field has been changed since. */
	get #code(): number | null {
		return this.#original.code;
	}

	#snapshot(): V {
		return $state.snapshot(this.values) as V;
	}

	async #persist(): Promise<boolean> {
		const { id } = this;
		const { entity } = this.#options;
		let saved: Saved;
		try {
			const payload = this.#options.toPayload(this.#snapshot());
			saved =
				id === null ? await this.#options.create(payload) : await this.#options.update(id, payload);
		} catch (err) {
			this.#refused(err);
			return false;
		}
		this.#fieldErrors = {};
		this.#options.toast(`${entity.Label} ${saved.code} guardado`);
		// Saved: nothing left to save, also while the next new record is on its way.
		this.#original = this.#snapshot();
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
		this.values = next;
		this.#original = $state.snapshot(next) as V;
		await tick();
		this.nav?.focus('code');
		return true;
	}

	#refused(err: unknown): void {
		const { entity, fieldMessages } = this.#options;
		if (err instanceof ApiError && err.code === entity.duplicateCode) {
			this.#setFieldErrors({ code: `Ya existe un ${entity.one} con ese código` });
			this.nav?.focus('code');
		} else if (
			err instanceof ApiError &&
			err.code === 'VALIDATION_ERROR' &&
			Object.keys(err.fieldErrors).length > 0
		) {
			// The API takes only positive codes; empty is already caught by validateAll.
			const messages = fieldErrorMessages(err.fieldErrors, {
				code: 'El código tiene que ser mayor que cero',
				...fieldMessages
			});
			this.#setFieldErrors(messages);
			this.nav?.focusFirst(Object.keys(messages));
		} else {
			showError(err);
		}
	}

	#setFieldErrors(messages: Record<string, string>): void {
		this.#fieldErrors = Object.fromEntries(
			Object.entries(messages).map(([name, message]) => [
				name,
				{ message, value: JSON.stringify(valueAt(this.values, name)) }
			])
		);
	}
}
