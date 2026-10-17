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

export function recallList(section: string): ListMemory | undefined {
	const memory = memories.get(section);
	return memory && { ...memory };
}
