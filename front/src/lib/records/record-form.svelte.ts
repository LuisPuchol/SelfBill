import { tick } from 'svelte';
import { ApiError, type ErrorCode } from '../api/errors';
import { fieldErrorMessages } from '../api/field-errors';
import { showError } from '../components/errors.svelte';
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
	deleteDialog: { title: string; name: string; body: string | null } | null = $state(null);
	confirmMessage: string | null = $state(null);

	readonly #options: RecordFormOptions<V>;
	#original: V;
	#fieldErrors: Record<string, FieldError> = $state.raw({});
	#saving = false;
	#deleting = false;

	constructor(options: RecordFormOptions<V>) {
		this.#options = options;
		this.id = options.id;
		this.#original = $state.raw($state.snapshot(options.initial) as V);
		// A copy of its own: the fields write here, never into the caller's object.
		this.values = $state($state.snapshot(options.initial) as V);
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

	async askDelete(): Promise<void> {
		const id = this.id;
		if (id === null || this.#deleting || this.deleteDialog) return;
		this.#deleting = true;
		try {
			const body = await this.#options.pendingForDelete(id);
			this.deleteDialog = {
				title: `¿Borrar el ${this.#options.entity.one} ${this.#code}?`,
				// Like the code: the saved record's, not what the form holds now.
				name: this.#options.describe(this.#original),
				body
			};
		} catch (err) {
			showError(err);
		} finally {
			this.#deleting = false;
		}
	}

	async confirmDelete(): Promise<void> {
		const id = this.id;
		if (id === null || this.#deleting) return;
		this.#deleting = true;
		try {
			await this.#options.remove(id);
		} catch (err) {
			this.deleteDialog = null;
			showError(err);
			return;
		} finally {
			this.#deleting = false;
		}
		this.deleteDialog = null;
		this.#options.toast(`${this.#options.entity.Label} ${this.#code} borrado`);
		// Nothing left to save: the unsaved-changes question must not stop the way out.
		this.#original = this.#snapshot();
		await this.#options.goToList();
	}

	cancelDelete(): void {
		this.deleteDialog = null;
	}

	exit(): void {
		void this.#options.goToList();
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
		if (id !== null) {
			this.#original = this.#snapshot();
			await this.#options.goToList(id);
			return true;
		}
		let next: V;
		try {
			next = await this.#options.blank();
		} catch (err) {
			// Saved, but no next record to chain into: back to the list, where the new one is.
			this.#original = this.#snapshot();
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
			const messages = fieldErrorMessages(err.fieldErrors, fieldMessages);
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
