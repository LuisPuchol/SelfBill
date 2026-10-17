/** Spanish texts for the API's per-field errors; `known` is keyed by path, indices emptied (`lines[].quantity`). */
export function fieldErrorMessages(
	apiErrors: Record<string, string>,
	known: Record<string, string> = {}
): Record<string, string> {
	return Object.fromEntries(
		Object.keys(apiErrors).map((path) => [
			path,
			known[path] ?? known[path.replace(/\[\d+\]/g, '[]')] ?? 'Revisa este dato'
		])
	);
}
