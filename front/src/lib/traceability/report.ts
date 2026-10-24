import { formatDate } from '../format/date';
import { formatDecimal } from '../format/number';

export interface TraceabilityReportInput {
	code: string;
	articleName: string;
	from: string;
	to: string;
	rows: {
		date: string;
		clientCode: number;
		clientName: string;
		deliveryNoteCode: number;
		quantity: number;
	}[];
}

const ESCAPES: Record<string, string> = {
	'&': '&amp;',
	'<': '&lt;',
	'>': '&gt;',
	'"': '&quot;',
	"'": '&#39;'
};
const escapeHtml = (text: string) => text.replace(/[&<>"']/g, (c) => ESCAPES[c]);

/** The printable listing of a lot's deliveries: a complete HTML document, for `printDocument`. */
export function traceabilityReport({ code, articleName, from, to, rows }: TraceabilityReportInput) {
	// Summed in hundredths, as floats drift
	const total = rows.reduce((acc, r) => acc + Math.round(r.quantity * 100), 0) / 100;
	const heading = escapeHtml(`Trazabilidad ${code} ${articleName}`);
	const body = rows
		.map(
			(r) => `<tr>
<td>${formatDate(r.date)}</td>
<td>${r.clientCode} ${escapeHtml(r.clientName)}</td>
<td class="num">${r.deliveryNoteCode}</td>
<td class="num">${formatDecimal(r.quantity, 2)}</td>
</tr>`
		)
		.join('\n');
	return `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Trazabilidad ${escapeHtml(code)}</title>
<style>
@page { size: A4; margin: 15mm; }
body { font-family: system-ui, sans-serif; font-size: 11pt; color: #000; }
h1 { font-size: 16pt; margin: 0 0 4pt; }
p { margin: 0 0 4pt; }
table { width: 100%; border-collapse: collapse; margin-top: 10pt; }
th, td { padding: 3pt 6pt; border-bottom: 1px solid #999; text-align: left; }
th.num, td.num { text-align: right; }
</style>
</head>
<body>
<h1>${heading}</h1>
<p>Total cantidad ${formatDecimal(total, 2)}</p>
<p>Del ${formatDate(from)} al ${formatDate(to)}</p>
<table>
<thead>
<tr><th>Fecha</th><th>Cliente</th><th class="num">Nº albarán</th><th class="num">Cantidad</th></tr>
</thead>
<tbody>
${body}
</tbody>
</table>
</body>
</html>
`;
}
