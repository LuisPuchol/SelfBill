import { classifyError, type ErrorView } from '../api/classify';

export const errorState = $state<{ view: ErrorView | null; connectionLost: boolean }>({
	view: null,
	connectionLost: false
});

/** A connection error raises the full-screen flag; anything else opens the dialog (a newer error replaces an older one). */
export function showError(err: unknown): void {
	const view = classifyError(err);
	if (view.kind === 'connection') {
		// The connection screen takes over: drop any open dialog so it can't resurface afterwards.
		errorState.view = null;
		errorState.connectionLost = true;
	} else errorState.view = view;
}

export function dismissError(): void {
	errorState.view = null;
}
