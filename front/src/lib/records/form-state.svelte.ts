import { ApiError } from '../api/errors';
import { fieldErrorMessages } from '../api/field-errors';
import { showError } from '../components/errors.svelte';
import type { FormNav } from '../keyboard/form-nav';

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
 * State of a form: its values and the saved copy, the API's field errors, the lock while saving
 * and the way back to the right field. `EditScreen` renders it and the page brings the API;
 * `RecordForm` builds the catalog's records on it.
 */
export class FormState<V extends object> {
	values: V;
	/** Set by the screen through `bind:nav` */
	nav: FormNav | undefined = $state.raw();

	#saved: V;
	#fieldErrors: Record<string, FieldError> = $state.raw({});
	#saving = false;
	readonly #comparable: (values: V) => unknown;

	/** `comparable`: what the dirty check compares, e.g. without what is never sent; all by default. */
	constructor(initial: V, options: { comparable?(values: V): unknown } = {}) {
		this.#comparable = options.comparable ?? ((values) => values);
		this.#saved = $state.raw($state.snapshot(initial) as V);
		// A copy of its own: the fields write here, never into the caller's object.
		this.values = $state($state.snapshot(initial) as V);
	}

	get isDirty(): boolean {
		return !same(this.#comparable(this.values), this.#comparable(this.#saved));
	}

	/** The values as last saved (or as opened). */
	get saved(): V {
		return this.#saved;
	}

	/** The API's error for a field, while the field keeps the value it had when the error came. */
	errorFor(name: string): string | undefined {
		const error = this.#fieldErrors[name];
		if (!error) return undefined;
		return JSON.stringify(valueAt(this.values, name)) === error.value ? error.message : undefined;
	}

	/** Errors under the fields, by field path; the cursor goes to the first one in the page. */
	setFieldErrors(messages: Record<string, string>): void {
		this.#fieldErrors = Object.fromEntries(
			Object.entries(messages).map(([name, message]) => [
				name,
				{ message, value: JSON.stringify(valueAt(this.values, name)) }
			])
		);
		this.nav?.focusFirst(Object.keys(messages));
	}

	clearFieldErrors(): void {
		this.#fieldErrors = {};
	}

	/**
	 * A VALIDATION_ERROR with field errors goes under its fields, in Spanish: `messages` by field
	 * path, "Revisa este dato" for the rest. False for anything else, which is left to the caller.
	 * `fieldPath` turns the API's path into the form's when they differ.
	 */
	showFieldErrors(
		err: unknown,
		messages: Record<string, string> = {},
		fieldPath: (apiPath: string) => string = (apiPath) => apiPath
	): boolean {
		if (
			!(err instanceof ApiError) ||
			err.code !== 'VALIDATION_ERROR' ||
			Object.keys(err.fieldErrors).length === 0
		) {
			return false;
		}
		const shown = fieldErrorMessages(err.fieldErrors, messages);
		this.setFieldErrors(
			Object.fromEntries(Object.entries(shown).map(([path, message]) => [fieldPath(path), message]))
		);
		return true;
	}

	/** Nothing left to save: the current values become the saved ones. */
	markSaved(): void {
		this.#saved = this.snapshot();
	}

	/** Another record to edit: values and saved copy both become `values`. */
	reset(values: V): void {
		this.values = $state.snapshot(values) as V;
		this.markSaved();
	}

	snapshot(): V {
		return $state.snapshot(this.values) as V;
	}

	/** Back to the first field the API refused; with none, to where the form had the cursor. */
	restoreFocus(): void {
		const refused = Object.keys(this.#fieldErrors).filter((name) => this.errorFor(name));
		if (refused.length > 0) this.nav?.focusFirst(refused);
		else this.nav?.restoreFocus();
	}

	/**
	 * Checks every field, then runs `persist`, which sends the values and shows the API's answer.
	 * One save at a time; what it throws goes to the error dialog. False when nothing was saved:
	 * invalid, already saving, or `persist` said so.
	 */
	save(persist: () => Promise<boolean>): Promise<boolean> {
		return this.exclusively(async () => {
			if (!(await this.nav?.validateAll())) return false;
			return persist();
		});
	}

	/**
	 * Runs `step` unless a save is already running, holding the lock meanwhile. What it throws goes
	 * to the error dialog. False when it did not run.
	 */
	protected async exclusively(step: () => Promise<boolean>): Promise<boolean> {
		if (this.#saving) return false;
		this.#saving = true;
		try {
			return await step();
		} catch (err) {
			showError(err);
			return false;
		} finally {
			this.#saving = false;
		}
	}
}
