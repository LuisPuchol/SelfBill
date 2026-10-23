import { tick } from 'svelte';
import { classifyError, type ErrorView } from '../api/classify';

/** What the screen says around an error, in the same dialog. */
export interface ErrorContext {
	/** Before the error's own text, e.g. what did work */
	lead?: string;
	/** After it: something else that went wrong in the same go */
	also?: string;
}

export const errorState = $state<{
	view: (ErrorView & ErrorContext) | null;
	connectionLost: boolean;
}>({
	view: null,
	connectionLost: false
});

/** What waits for the error dialog to close: see `whenNoError`. */
let waiting: (() => void)[] = [];

/**
 * A connection error raises the full-screen flag; anything else opens the dialog (a newer error
 * replaces an older one), with `context` around the error's Spanish text.
 */
export function showError(err: unknown, context: ErrorContext = {}): void {
	const view = classifyError(err);
	if (view.kind === 'connection') {
		// The connection screen takes over: drop any open dialog so it can't resurface afterwards.
		errorState.view = null;
		errorState.connectionLost = true;
		closed();
	} else errorState.view = { ...view, ...context };
}

/** Opens the dialog with a text of the screen's own, whatever the error behind it. */
export function showMessage(message: string): void {
	errorState.view = { kind: 'business', message };
}

export function dismissError(): void {
	errorState.view = null;
	closed();
}

/**
 * Runs `then` once no error dialog is open: straight away when none is. For moving the cursor,
 * which cannot get behind an open dialog.
 */
export function whenNoError(then: () => void): void {
	if (errorState.view === null) then();
	else waiting.push(then);
}

function closed(): void {
	const run = waiting;
	waiting = [];
	// Once the dialog has really closed and given back the focus it took.
	if (run.length > 0) void tick().then(() => run.forEach((then) => then()));
}
