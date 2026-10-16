export interface Toast {
	id: number;
	message: string;
}

export const toasts = $state<{ list: Toast[] }>({ list: [] });

let nextId = 0;

export function toast(message: string, durationMs = 3000): void {
	const id = nextId++;
	toasts.list = [...toasts.list, { id, message }];
	setTimeout(() => {
		toasts.list = toasts.list.filter((t) => t.id !== id);
	}, durationMs);
}
