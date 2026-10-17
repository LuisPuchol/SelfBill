export interface ArticleValues {
	code: number | null;
	name: string;
}

export function articlePayload(v: ArticleValues): { code: number | null; name: string } {
	return { code: v.code, name: v.name.trim() };
}
