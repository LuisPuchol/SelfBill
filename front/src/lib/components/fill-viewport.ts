/**
 * Caps the element's height to what the window has left below its top, less the bottom padding of
 * the page's `main` (the room kept for the key bar): what does not fit scrolls inside it, and the
 * page never scrolls, so what sits above stays in sight. Never below `min` px: on a window too
 * short for even that, the page scrolls instead.
 */
export function fillViewport(node: HTMLElement, min = 160): { destroy(): void } {
	const fit = () => {
		const main = node.closest('main');
		const reserve = main ? parseFloat(getComputedStyle(main).paddingBottom) : 0;
		const top = node.getBoundingClientRect().top + window.scrollY;
		node.style.maxHeight = `${Math.max(min, window.innerHeight - top - reserve)}px`;
	};
	// Whatever grows or shrinks above it moves its top; the window changes what is left.
	const observer = new ResizeObserver(fit);
	observer.observe(document.body);
	window.addEventListener('resize', fit);
	fit();
	return {
		destroy() {
			observer.disconnect();
			window.removeEventListener('resize', fit);
		}
	};
}
