/**
 * Selects the whole content when the field gets the cursor, also with the mouse:
 * the browser would otherwise undo the selection on the click's mouseup.
 * A click on a field that already has the cursor places it as usual.
 */
export function selectOnFocus(node: HTMLInputElement): { destroy(): void } {
	let entering = false;

	const onmousedown = () => {
		entering = document.activeElement !== node;
	};
	const onfocus = () => node.select();
	const onmouseup = (event: MouseEvent) => {
		if (!entering) return;
		entering = false;
		event.preventDefault();
	};
	const onblur = () => (entering = false);

	node.addEventListener('mousedown', onmousedown);
	node.addEventListener('focus', onfocus);
	node.addEventListener('mouseup', onmouseup);
	node.addEventListener('blur', onblur);
	return {
		destroy() {
			node.removeEventListener('mousedown', onmousedown);
			node.removeEventListener('focus', onfocus);
			node.removeEventListener('mouseup', onmouseup);
			node.removeEventListener('blur', onblur);
		}
	};
}
