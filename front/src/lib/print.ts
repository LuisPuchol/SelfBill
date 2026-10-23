/** A PDF viewer inside an iframe may never fire load or error: give up after this long. */
export const PRINT_TIMEOUT_MS = 15_000;

/** Releases the frame of the last print, which its print dialog may still be reading. */
let releaseLast: (() => void) | undefined;

/**
 * Opens the browser's print dialog for a PDF: loads it in a hidden iframe and calls its `print()`.
 * A PDF viewer's `print()` may return before the dialog has read the PDF, so the iframe and the object
 * URL live on until printing ends (`afterprint`) or the next print replaces them. When loading or
 * printing fails, they are released at once.
 */
export function printPdf(pdf: Blob): Promise<void> {
	releaseLast?.();
	const url = URL.createObjectURL(pdf);
	const frame = document.createElement('iframe');
	frame.setAttribute('aria-hidden', 'true');
	frame.tabIndex = -1;
	frame.style.cssText = 'position:fixed;width:0;height:0;border:0;visibility:hidden';
	let released = false;
	const release = () => {
		if (released) return;
		released = true;
		frame.remove();
		URL.revokeObjectURL(url);
		if (releaseLast === release) releaseLast = undefined;
	};
	releaseLast = release;
	let timer: ReturnType<typeof setTimeout> | undefined;
	return new Promise<void>((resolve, reject) => {
		timer = setTimeout(() => reject(new Error('The PDF took too long to load')), PRINT_TIMEOUT_MS);
		frame.addEventListener('load', () => {
			try {
				const win = frame.contentWindow;
				if (!win) throw new Error('The print frame has no window');
				win.addEventListener('afterprint', release, { once: true });
				win.focus();
				win.print();
				resolve();
			} catch (err) {
				reject(err);
			}
		});
		frame.addEventListener('error', () => reject(new Error('The PDF could not be loaded')));
		frame.src = url;
		document.body.appendChild(frame);
	}).then(
		() => clearTimeout(timer),
		(err: unknown) => {
			clearTimeout(timer);
			release();
			throw err;
		}
	);
}
