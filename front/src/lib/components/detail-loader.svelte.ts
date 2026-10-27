/**
 * The detail of the marked row, loaded as the mark moves: answers cached by key, and only the
 * latest request shown or reported.
 */
export class DetailLoader<K extends string | number, T> {
	#load: (key: K) => Promise<T>;
	#onError: (err: unknown) => void;
	// Only read inside `select`, which is no reactive reader: a plain Map, not a SvelteMap.
	// eslint-disable-next-line svelte/prefer-svelte-reactivity -- never read reactively
	#cache = new Map<K, T>();
	#shown = $state.raw<{ key: K; value: T } | null>(null);
	/** Counts requests: only the latest one is shown. */
	#seq = 0;
	/** Raised by `clear`: an answer that started before is for another list. */
	#generation = 0;

	constructor(load: (key: K) => Promise<T>, onError: (err: unknown) => void) {
		this.#load = load;
		this.#onError = onError;
	}

	/** Points at a key, or none: its cached detail at once, else asks for it. Call it untracked. */
	select(key: K | undefined): void {
		const seq = ++this.#seq;
		const generation = this.#generation;
		if (key === undefined) {
			this.#shown = null;
			return;
		}
		if (this.#cache.has(key)) {
			this.#shown = { key, value: this.#cache.get(key) as T };
			return;
		}
		this.#load(key).then(
			(value) => {
				if (generation === this.#generation) this.#cache.set(key, value);
				if (seq === this.#seq && generation === this.#generation) this.#shown = { key, value };
			},
			(err: unknown) => {
				if (seq === this.#seq && generation === this.#generation) this.#onError(err);
			}
		);
	}

	/** The detail on screen if it is `key`'s, else undefined (loading, or another key). */
	get(key: K | undefined): T | undefined {
		const shown = this.#shown;
		return key !== undefined && shown !== null && shown.key === key ? shown.value : undefined;
	}

	/**
	 * Forgets every answer: one to a request made before is neither cached nor shown; what was on
	 * screen goes too.
	 */
	clear(): void {
		this.#generation++;
		this.#cache.clear();
		this.#shown = null;
	}
}
