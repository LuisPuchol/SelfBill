/** Where a section's list was left: its URL (search and page) and the last record opened from it. */
export interface ListMemory {
	href: string;
	openedId?: number;
}

// In memory: it only has to last while the app is open.
const memories = new Map<string, ListMemory>();

export function rememberList(section: string, memory: ListMemory): void {
	memories.set(section, { ...memory });
}

/** The list's URL now, after a search or a page turn; the last record opened stays. */
export function rememberListUrl(section: string, href: string): void {
	memories.set(section, { ...memories.get(section), href });
}

/** A record was deleted: if it was the last one opened, keep the list's URL and forget the record. */
export function forgetOpened(section: string, id: number): void {
	const memory = memories.get(section);
	if (memory?.openedId === id) memories.set(section, { href: memory.href });
}

export function recallList(section: string): ListMemory | undefined {
	const memory = memories.get(section);
	return memory && { ...memory };
}
