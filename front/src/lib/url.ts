/**
 * Returns pathname + search with `params` applied. null/undefined remove a
 * parameter. `page` is the API page number, which starts at 0.
 */
export function withParams(
	url: URL,
	params: Record<string, string | number | null | undefined>
): string {
	const search = new URLSearchParams(url.search);
	for (const [key, value] of Object.entries(params)) {
		if (value === null || value === undefined) search.delete(key);
		else search.set(key, String(value));
	}
	const query = search.toString();
	return url.pathname + (query ? `?${query}` : '');
}
