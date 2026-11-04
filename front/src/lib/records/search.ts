/** The list's single search box: digits alone look up an exact code, anything else filters by text. */
export function isCodeSearch(text: string): boolean {
	return /^\d+$/.test(text.trim());
}

/** The largest code the API binds (a Java `Integer`). */
const MAX_CODE = 2147483647;

/**
 * Query parameters for a list search; `textParam` is the entity's text filter (`name` or `q`).
 * A code above what the API binds cannot exist: it asks for an empty range instead of a 400.
 */
export function listQuery(
	text: string,
	textParam: 'name' | 'q'
): { codeFrom?: number; codeTo?: number; name?: string; q?: string } {
	const trimmed = text.trim();
	if (trimmed === '') return {};
	if (isCodeSearch(trimmed)) {
		const code = Number(trimmed);
		if (code > MAX_CODE) return { codeFrom: MAX_CODE, codeTo: 0 };
		return { codeFrom: code, codeTo: code };
	}
	return { [textParam]: trimmed };
}
