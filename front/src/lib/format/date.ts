const pad = (n: number, width = 2) => String(n).padStart(width, '0');

/** Reads the digits directly: `new Date(iso)` would parse date-only strings as UTC. */
export function formatDate(iso: string): string {
	const [year, month, day] = iso.slice(0, 10).split('-');
	return `${day}/${month}/${year}`;
}

/** Accepts ddmm (year of `today`), ddmmyy (20yy) or ddmmyyyy; returns ISO or null. */
export function parseDateDigits(text: string, today: string): string | null {
	const digits = text.replace(/\D/g, '');
	let year: number;
	if (digits.length === 4) year = Number(today.slice(0, 4));
	else if (digits.length === 6) year = 2000 + Number(digits.slice(4));
	else if (digits.length === 8) year = Number(digits.slice(4));
	else return null;

	const day = Number(digits.slice(0, 2));
	const month = Number(digits.slice(2, 4));
	const check = new Date(Date.UTC(year, month - 1, day));
	if (
		check.getUTCFullYear() !== year ||
		check.getUTCMonth() !== month - 1 ||
		check.getUTCDate() !== day
	) {
		return null;
	}
	return `${pad(year, 4)}-${pad(month)}-${pad(day)}`;
}

/** Local day; `toISOString()` would give the UTC day, still yesterday just after midnight. */
export function todayIso(now = new Date()): string {
	return `${pad(now.getFullYear(), 4)}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}
