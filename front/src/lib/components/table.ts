export interface Column<T> {
	label: string;
	value: (row: T) => string;
	align?: 'left' | 'right';
}
