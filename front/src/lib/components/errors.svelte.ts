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

/** What waits for the error dialog to close and the connection to come back: see `whenNoError`. */
let waiting: (() => void)[] = [];

/**
 * A connection error raises the full-screen flag; anything else opens the dialog (a newer error
 * replaces an older one), with `context` around the error's Spanish text.
 */
export function showError(err: unknown, context: ErrorContext = {}): void {
	const view = classifyError(err);
	if (view.kind === 'connection') {
		// The connection screen takes over: drop any open dialog so it can't resurface afterwards.
		// What waited for the dialog waits for the connection now.
		errorState.view = null;
		errorState.connectionLost = true;
	} else errorState.view = { ...view, ...context };
}

/** Opens the dialog with a text of the screen's own, whatever the error behind it. */
export function showMessage(message: string): void {
	errorState.view = { kind: 'business', message };
}

/** A print that failed: a lost connection raises its screen, anything else says `message`. */
export function showPrintError(err: unknown, message: string): void {
	if (classifyError(err).kind === 'connection') showError(err);
	else showMessage(message);
}

export function dismissError(): void {
	errorState.view = null;
	closed();
}

/** The API answers again: the connection screen goes, and what waited for it runs. */
export function connectionRestored(): void {
	errorState.connectionLost = false;
	closed();
}

/**
 * Runs `then` once no error dialog is open and the connection screen is gone: straight away when
 * neither is up. For moving the cursor, which can get neither behind an open dialog nor into the
 * page the connection screen makes inert. A connection lost while it waits keeps it waiting until
 * the connection is back.
 */
export function whenNoError(then: () => void): void {
	if (noError()) then();
	else waiting.push(then);
}

function noError(): boolean {
	return errorState.view === null && !errorState.connectionLost;
}

function closed(): void {
	if (!noError()) return;
	const run = waiting;
	waiting = [];
	// Once the dialog has really closed and given back the focus it took, and the page is no
	// longer inert.
	if (run.length > 0) void tick().then(() => run.forEach((then) => then()));
}
