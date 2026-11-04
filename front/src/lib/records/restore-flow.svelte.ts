import { showError } from '../components/errors.svelte';
import type { DeleteTarget } from './delete-flow.svelte';

export type RestoreTarget = DeleteTarget;

/** What an entity knows about bringing back its deleted records; its list of deleted ones uses it. */
export interface RestoreOptions {
	/** `feminine`: "la factura", "Factura 3 restaurada" */
	entity: { one: string; Label: string; feminine?: boolean };
	restore(id: number): Promise<void>;
}

export interface RestoreFlowOptions extends RestoreOptions {
	toast(message: string): void;
	onRestored(target: RestoreTarget): void | Promise<void>;
	/** After a failed restore (gone, or its code taken by a live one), once the error is shown */
	onFailed?(target: RestoreTarget): void | Promise<void>;
}

/**
 * The restore question, the delete one's twin: asks, restores and tells. `ConfirmDialog` renders it
 * with its own words.
 */
export class RestoreFlow {
	dialog: { title: string; name: string } | null = $state(null);

	readonly #options: RestoreFlowOptions;
	#target: RestoreTarget | null = null;
	#busy = false;

	constructor(options: RestoreFlowOptions) {
		this.#options = options;
	}

	/** Nothing depends on a deleted record: no count to wait for, the question comes at once. */
	ask(target: RestoreTarget): void {
		if (this.#busy || this.dialog) return;
		const { entity } = this.#options;
		this.#target = target;
		this.dialog = {
			title: `¿Restaurar ${entity.feminine ? 'la' : 'el'} ${entity.one} ${target.code}?`,
			name: target.name
		};
	}

	async confirm(): Promise<void> {
		const target = this.#target;
		if (!target || !this.dialog || this.#busy) return;
		this.#busy = true;
		try {
			await this.#options.restore(target.id);
		} catch (err) {
			this.dialog = null;
			showError(err);
			await this.#options.onFailed?.(target);
			return;
		} finally {
			this.#busy = false;
		}
		this.dialog = null;
		const { entity } = this.#options;
		this.#options.toast(
			`${entity.Label} ${target.code} ${entity.feminine ? 'restaurada' : 'restaurado'}`
		);
		await this.#options.onRestored(target);
	}

	cancel(): void {
		this.dialog = null;
	}
}
