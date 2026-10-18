import { showError } from '../components/errors.svelte';

export interface DeleteTarget {
	id: number;
	code: number | null;
	name: string;
}

/** What an entity knows about deleting its records; shared by its record and its list. */
export interface DeleteOptions {
	entity: { one: string; Label: string };
	remove(id: number): Promise<void>;
	/** The body of the question, or null when nothing depends on the record */
	pendingForDelete(id: number): Promise<string | null>;
}

export interface DeleteFlowOptions extends DeleteOptions {
	toast(message: string): void;
	onDeleted(target: DeleteTarget): void | Promise<void>;
	/** After a failed remove, once the error is shown */
	onFailed?(target: DeleteTarget): void | Promise<void>;
}

/**
 * The delete question: counts what depends on the record, asks, removes and tells.
 * `DeleteDialog` renders it; the record screen and the list each own one.
 */
export class DeleteFlow {
	dialog: { title: string; name: string; body: string | null } | null = $state(null);

	readonly #options: DeleteFlowOptions;
	#target: DeleteTarget | null = null;
	#busy = false;

	constructor(options: DeleteFlowOptions) {
		this.#options = options;
	}

	async ask(target: DeleteTarget): Promise<void> {
		if (this.#busy || this.dialog) return;
		this.#busy = true;
		try {
			const body = await this.#options.pendingForDelete(target.id);
			this.#target = target;
			this.dialog = {
				title: `¿Borrar el ${this.#options.entity.one} ${target.code}?`,
				name: target.name,
				body
			};
		} catch (err) {
			showError(err);
		} finally {
			this.#busy = false;
		}
	}

	async confirm(): Promise<void> {
		const target = this.#target;
		if (!target || !this.dialog || this.#busy) return;
		this.#busy = true;
		try {
			await this.#options.remove(target.id);
		} catch (err) {
			this.dialog = null;
			showError(err);
			await this.#options.onFailed?.(target);
			return;
		} finally {
			this.#busy = false;
		}
		this.dialog = null;
		this.#options.toast(`${this.#options.entity.Label} ${target.code} borrado`);
		await this.#options.onDeleted(target);
	}

	cancel(): void {
		this.dialog = null;
	}
}
