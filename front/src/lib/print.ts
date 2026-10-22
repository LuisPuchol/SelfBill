/** A PDF viewer inside an iframe may never fire load or error: give up after this long. */
export const PRINT_TIMEOUT_MS = 15_000;

/**
 * Opens the browser's print dialog for a PDF: loads it in a hidden iframe and calls its `print()`.
 * The iframe and the object URL are released afterwards, also when printing fails.
 */
export function printPdf(pdf: Blob): Promise<void> {
	const url = URL.createObjectURL(pdf);
	const frame = document.createElement('iframe');
	frame.setAttribute('aria-hidden', 'true');
	frame.tabIndex = -1;
	frame.style.cssText = 'position:fixed;width:0;height:0;border:0;visibility:hidden';
	let timer: ReturnType<typeof setTimeout> | undefined;
	return new Promise<void>((resolve, reject) => {
		timer = setTimeout(() => reject(new Error('The PDF took too long to load')), PRINT_TIMEOUT_MS);
		frame.addEventListener('load', () => {
			try {
				const win = frame.contentWindow;
				if (!win) throw new Error('The print frame has no window');
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
	}).finally(() => {
		clearTimeout(timer);
		frame.remove();
		URL.revokeObjectURL(url);
	});
}
