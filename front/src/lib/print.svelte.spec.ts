import { afterEach, describe, expect, it, vi } from 'vitest';
import { PRINT_TIMEOUT_MS, printPdf } from './print';

/** Replaces `print` on every iframe created; `seen` records how many iframes existed while printing. */
function stubPrint(impl: () => void = () => {}) {
	const seen: number[] = [];
	const print = vi.fn(() => {
		seen.push(document.querySelectorAll('iframe').length);
		impl();
	});
	const create = document.createElement.bind(document);
	vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
		const el = create(tag);
		if (tag === 'iframe') {
			el.addEventListener('load', () => {
				const win = (el as HTMLIFrameElement).contentWindow;
				if (win) win.print = print;
			});
		}
		return el;
	}) as typeof document.createElement);
	return { print, seen };
}

const frames = () => document.querySelectorAll('iframe');
const page = () => new Blob(['<p>hola</p>'], { type: 'text/html' });
/** What the browser fires once the print dialog closes. */
const endPrinting = () =>
	frames().forEach((frame) => frame.contentWindow?.dispatchEvent(new Event('afterprint')));

afterEach(() => {
	// No test leaves a frame open for the next one.
	endPrinting();
	vi.restoreAllMocks();
});

describe('printPdf', () => {
	it('prints through a hidden iframe, kept until printing ends', async () => {
		const revoke = vi.spyOn(URL, 'revokeObjectURL');
		const { print, seen } = stubPrint();
		await printPdf(page());
		expect(print).toHaveBeenCalledOnce();
		expect(seen).toEqual([1]);
		// The dialog may still be reading the PDF.
		expect(frames()).toHaveLength(1);
		expect(revoke).not.toHaveBeenCalled();

		endPrinting();
		expect(frames()).toHaveLength(0);
		expect(revoke).toHaveBeenCalledOnce();
	});

	it('keeps at most one frame across prints', async () => {
		const revoke = vi.spyOn(URL, 'revokeObjectURL');
		const { print, seen } = stubPrint();
		for (let i = 0; i < 3; i++) await printPdf(page());
		expect(print).toHaveBeenCalledTimes(3);
		// Each print released the frame before it.
		expect(seen).toEqual([1, 1, 1]);
		expect(frames()).toHaveLength(1);
		expect(revoke).toHaveBeenCalledTimes(2);
	});

	it('cleans up when print throws', async () => {
		const revoke = vi.spyOn(URL, 'revokeObjectURL');
		stubPrint(() => {
			throw new Error('boom');
		});
		await expect(printPdf(new Blob(['x'], { type: 'text/html' }))).rejects.toThrow('boom');
		expect(document.querySelectorAll('iframe')).toHaveLength(0);
		expect(revoke).toHaveBeenCalledOnce();
	});

	it('gives up and cleans up when the frame never loads', async () => {
		vi.useFakeTimers();
		try {
			const revoke = vi.spyOn(URL, 'revokeObjectURL');
			const frame = Object.assign(new EventTarget(), {
				setAttribute: () => {},
				style: {},
				remove: vi.fn(),
				tabIndex: 0,
				src: ''
			});
			const create = document.createElement.bind(document);
			vi.spyOn(document, 'createElement').mockImplementation(((tag: string) =>
				tag === 'iframe' ? frame : create(tag)) as typeof document.createElement);
			vi.spyOn(document.body, 'appendChild').mockImplementation((n) => n);
			const result = expect(printPdf(new Blob(['x']))).rejects.toThrow('too long');
			await vi.advanceTimersByTimeAsync(PRINT_TIMEOUT_MS);
			await result;
			expect(frame.remove).toHaveBeenCalledOnce();
			expect(revoke).toHaveBeenCalledOnce();
		} finally {
			vi.useRealTimers();
		}
	});
});
