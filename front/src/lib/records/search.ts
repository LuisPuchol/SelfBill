/** The list's single search box: digits alone look up an exact code, anything else filters by text. */
export function isCodeSearch(text: string): boolean {
	return /^\d+$/.test(text.trim());
}

/** Query parameters for a list search; `textParam` is the entity's text filter (`name` or `q`). */
export function listQuery(
	text: string,
	textParam: 'name' | 'q'
): { codeFrom?: number; codeTo?: number; name?: string; q?: string } {
	const trimmed = text.trim();
	if (trimmed === '') return {};
	if (isCodeSearch(trimmed)) {
		const code = Number(trimmed);
		return { codeFrom: code, codeTo: code };
	}
	return { [textParam]: trimmed };
}
