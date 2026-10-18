/** An optional text as the API wants it: trimmed, and null when nothing is left. */
export function trimOrNull(value: string | null | undefined): string | null {
	const trimmed = value?.trim() ?? '';
	return trimmed === '' ? null : trimmed;
}
