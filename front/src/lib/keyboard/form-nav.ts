import { getContext, setContext } from 'svelte';
import { on } from 'svelte/events';
import { createSubscriber } from 'svelte/reactivity';

export interface NavField {
	/** API field path, e.g. `lines[2].quantity`; also the input's id and name */
	name: string;
	el: HTMLElement;
	/** Validates on leaving with Enter; false keeps the cursor in the field */
	commit?: () => boolean | Promise<boolean>;
}

const FORM_NAV = Symbol('form-nav');

/** Enter / Shift+Enter navigation over the registered fields, in page order. */
export class FormNav {
	#fields: NavField[] = [];
	#busy = false;
	#last: NavField | undefined;
	/** First field that failed the last validateAll(); the cursor could not go there if a dialog was open. */
	#lastFailed: NavField | undefined;
	readonly #onLast?: () => void;
	readonly #subscribe = createSubscriber((update) => {
		const off = [on(document, 'focusin', update), on(document, 'focusout', update)];
		return () => off.forEach((remove) => remove());
	});

	constructor(options: { onLast?: () => void } = {}) {
		this.#onLast = options.onLast;
	}

	/** True while waiting for a commit or a validateAll(): next() does nothing meanwhile. */
	get busy(): boolean {
		return this.#busy;
	}

	register(field: NavField): () => void {
		this.#fields.push(field);
		const off = on(field.el, 'focusin', () => {
			this.#last = field;
			if (this.#lastFailed !== field) this.#lastFailed = undefined;
		});
		return () => {
			off();
			this.#fields = this.#fields.filter((f) => f !== field);
			if (this.#last === field) this.#last = undefined;
			if (this.#lastFailed === field) this.#lastFailed = undefined;
		};
	}

	async next(): Promise<void> {
		if (this.#busy) return;
		const field = this.#current();
		if (!field) return;
		let ok = field.commit?.() ?? true;
		if (typeof ok !== 'boolean') {
			this.#busy = true;
			try {
				ok = await ok;
			} finally {
				this.#busy = false;
			}
			// The user may have moved elsewhere (or the field gone) meanwhile: leave the cursor be.
			if (this.#current() !== field) return;
		}
		if (!ok) return;
		const ordered = this.#ordered();
		const following = ordered[ordered.indexOf(field) + 1];
		if (following) following.el.focus();
		else this.#onLast?.();
	}

	/**
	 * Before saving: runs every field's commit in page order, so every error shows, and puts the
	 * cursor on the first field that fails. Fields reached with the mouse, or never visited, are
	 * only checked here. False, without checking again, while a commit is already running.
	 */
	async validateAll(): Promise<boolean> {
		if (this.#busy) return false;
		this.#busy = true;
		try {
			const ordered = this.#ordered();
			// Started in page order; the async ones (lookups) are awaited together.
			const results = await Promise.all(ordered.map(async (field) => field.commit?.() ?? true));
			const failed = ordered.find((_, i) => !results[i]);
			this.#lastFailed = failed;
			failed?.el.focus();
			return failed === undefined;
		} finally {
			this.#busy = false;
		}
	}

	prev(): void {
		const field = this.#current();
		if (!field) return;
		const ordered = this.#ordered();
		ordered[ordered.indexOf(field) - 1]?.el.focus();
	}

	/**
	 * The cursor is in a registered field. Reactive: the key bar evaluates `when`
	 * inside a $derived, and document.activeElement alone would never refresh it.
	 */
	hasFocus(): boolean {
		this.#subscribe();
		return this.#current() !== undefined;
	}

	focus(name: string): void {
		this.#fields.find((f) => f.name === name)?.el.focus();
	}

	/** Focuses whichever of `names` comes first in the page, e.g. the first field with an error. */
	focusFirst(names: readonly string[]): void {
		this.#ordered()
			.find((f) => names.includes(f.name))
			?.el.focus();
	}

	/** The first field in page order. */
	focusFirstField(): void {
		this.#ordered()[0]?.el.focus();
	}

	/** Back to the first field that failed validateAll(), else the last one with the cursor, else the first. */
	restoreFocus(): void {
		(this.#lastFailed ?? this.#last ?? this.#ordered()[0])?.el.focus();
	}

	#current(): NavField | undefined {
		const active = document.activeElement;
		if (!active) return undefined;
		return this.#fields.find((f) => f.el.contains(active));
	}

	#ordered(): NavField[] {
		return [...this.#fields].sort((a, b) => {
			if (a.el === b.el) return 0;
			return a.el.compareDocumentPosition(b.el) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1;
		});
	}
}

export function setFormNav(nav: FormNav): void {
	setContext(FORM_NAV, nav);
}

export function getFormNav(): FormNav | undefined {
	return getContext<FormNav | undefined>(FORM_NAV);
}
