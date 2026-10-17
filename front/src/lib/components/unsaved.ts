export interface PendingNavigation {
	to: { url: URL } | null;
	willUnload: boolean;
	cancel(): void;
}

export function createUnsavedGuard(options: {
	isDirty: () => boolean;
	save: () => Promise<boolean>;
	goto: (url: URL) => Promise<void>;
	onAsk: (asking: boolean) => void;
}): {
	check(nav: PendingNavigation): void;
	keepEditing(): void;
	discard(): Promise<void>;
	saveAndLeave(): Promise<void>;
} {
	let pending: URL | null = null;
	let leaving = false;
	let saving = false;

	async function leave(): Promise<void> {
		const url = pending;
		pending = null;
		options.onAsk(false);
		if (!url) return;
		// Our own goto passes through check() again: let it through.
		leaving = true;
		try {
			await options.goto(url);
		} finally {
			leaving = false;
		}
	}

	return {
		check(nav) {
			if (leaving || !options.isDirty()) return;
			// Closing the window or reloading: cancel() makes the browser show its own warning.
			nav.cancel();
			if (nav.willUnload || !nav.to) return;
			pending = nav.to.url;
			options.onAsk(true);
		},
		keepEditing() {
			pending = null;
			options.onAsk(false);
		},
		discard: leave,
		async saveAndLeave() {
			if (saving) return;
			saving = true;
			try {
				if (!(await options.save())) return;
			} finally {
				saving = false;
			}
			await leave();
		}
	};
}
