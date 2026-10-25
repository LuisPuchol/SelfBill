package com.luispuchol.selfbill.selfbill_api.billing.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Issuer;
import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Line;
import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Section;
import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Totals;

class PdfRendererTest {

    static final Issuer ISSUER = new Issuer("ADELINA DELGADO TORRES", "X1422821H",
            "Mercado Pedro Garau Puestos 41,42 - 07007 Palma de Mallorca");
    static final List<String> CLIENT = List.of("Cliente: 41 SON TERMENS GRAN CHEF SL", "PREDIO SON TERMENS S/N",
            "07193 BUNYOLA (ILLES BALEARS)", "CIF/NIF: B07688310");
    static final String LEGEND = "Documento sin valor fiscal. No válido como factura.";

    private static final Pattern HEADING = Pattern.compile("Albarán \\d+ de \\d{2}/\\d{2}/\\d{4}");
    private static final float POINTS_PER_MM = 72 / 25.4f;

    private final PdfRenderer renderer = new PdfRenderer();

    @BeforeAll
    static void quietFontWarning() {
        PdfText.quietFontWarning();
    }

    private static Line line() {
        return new Line("316", "PIMIENTO VERDE KG", "02260424", "5,30", "1,90", "10,07");
    }

    private static Totals totals() {
        return new Totals("1.234,56", "4,00", "49,38", "0,50", "6,17", "1.290,11 €");
    }

    private static PrintedDocument invoice(List<Section> sections, Totals totals) {
        return new PrintedDocument("Factura 3852", "31/05/2026", ISSUER, null, CLIENT, sections, totals, null, false);
    }

    private static PrintedDocument invoice() {
        return invoice(List.of(new Section("Albarán 31397 de 02/05/2026", List.of(line()))), totals());
    }

    private static PrintedDocument note(Issuer issuer, String logo, List<String> client, List<Line> lines,
            Totals totals) {
        return new PrintedDocument("Albarán 31397", "02/05/2026", issuer, logo, client,
                List.of(new Section(null, lines)), totals, totals == null ? "1.234,56 €" : null, totals != null);
    }

    private static PrintedDocument plainNote(Issuer issuer, String logo) {
        return note(issuer, logo, CLIENT, List.of(line(), line()), null);
    }

    @Test
    void rendersTheInvoice() {
        String text = PdfText.text(renderer.render(invoice()));

        assertThat(text).contains("Factura 3852", "Fecha 31/05/2026", "ADELINA DELGADO TORRES · NIF X1422821H",
                "Mercado Pedro Garau Puestos 41,42 - 07007 Palma de Mallorca");
        assertThat(text).contains(CLIENT);
        assertThat(text).contains("Código", "Concepto", "Trazabilidad", "Cantidad", "Precio", "Importe");
        assertThat(text).contains("Albarán 31397 de 02/05/2026", "316", "PIMIENTO VERDE KG", "02260424", "5,30",
                "1,90", "10,07");
        assertThat(text).contains("Base imponible", "% IVA", "Cuota IVA", "% Recargo", "Cuota recargo", "Total",
                "1.234,56", "4,00", "49,38", "0,50", "6,17", "1.290,11 €");
        assertThat(text).contains("Página 1/1");
    }

    @Test
    void surchargeColumnsOnlyWithSurcharge() {
        Totals withoutSurcharge = new Totals("1.234,56", "4,00", "49,38", null, null, "1.283,94 €");

        String text = PdfText.text(renderer.render(invoice(invoice().sections(), withoutSurcharge)));

        assertThat(text).contains("Base imponible", "% IVA", "Cuota IVA", "1.283,94 €");
        assertThat(text).doesNotContainIgnoringCase("recargo");
    }

    @Test
    void plainNote() {
        String text = PdfText.text(renderer.render(plainNote(ISSUER, null)));

        assertThat(text).contains("Albarán 31397", "Fecha 02/05/2026", "PIMIENTO VERDE KG", "Total 1.234,56 €");
        assertThat(text).doesNotContain("Base imponible", LEGEND);
    }

    @Test
    void valuedNote() {
        String text = PdfText.text(renderer.render(note(ISSUER, null, CLIENT, List.of(line()), totals())));

        assertThat(text).contains("Albarán 31397", "Base imponible", "1.290,11 €", LEGEND);
        assertThat(text).doesNotContain("Factura");
    }

    @Test
    void spanishCharacters() {
        Line line = new Line("7", "PIÑA ÁCIDA AÇAÍ · 1/2", "", "1,00", "2,00", "2,00");
        List<String> client = List.of("Cliente: 8 CAFÈ L'ÀNEC", "Carrer de l'Àngel, 3");

        String text = PdfText.text(renderer.render(note(ISSUER, null, client, List.of(line), null)));

        assertThat(text).contains("PIÑA ÁCIDA AÇAÍ · 1/2", "Cliente: 8 CAFÈ L'ÀNEC", "Carrer de l'Àngel, 3", "€");
        // OpenHTMLtoPDF prints "#" for a character its font cannot draw
        assertThat(text).doesNotContain("#");
    }

    @Test
    void escapesMarkup() {
        List<String> client = List.of("Cliente: 9 <b>A&B</b>");

        String text = PdfText.text(renderer.render(note(ISSUER, null, client, List.of(line()), null)));

        assertThat(text).contains("Cliente: 9 <b>A&B</b>");
    }

    @Test
    void longDocumentRepeatsHeaderOnEveryPage() {
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            List<Line> lines = new ArrayList<>();
            for (int j = 0; j <= i % 9; j++) {
                lines.add(line());
            }
            sections.add(new Section("Albarán " + (31397 + i) + " de 02/05/2026", lines));
        }

        List<String> pages = PdfText.pages(renderer.render(invoice(sections, totals())));

        assertThat(pages).hasSizeGreaterThanOrEqualTo(3);
        for (int i = 0; i < pages.size(); i++) {
            String page = pages.get(i);
            boolean last = i == pages.size() - 1;
            assertThat(page).contains("Factura 3852", "Cliente: 41 SON TERMENS GRAN CHEF SL", "Código",
                    "Página " + (i + 1) + "/" + pages.size());
            assertThat(page.contains("Base imponible")).as("totals on page %d", i + 1).isEqualTo(last);
            assertThat(lastTableLine(page)).as("last table line of page %d", i + 1).doesNotMatch(HEADING);
        }
    }

    // The last line of the page above the totals box, if the page has it
    private static String lastTableLine(String page) {
        List<String> lines = page.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
        int end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Base imponible")) {
                end = i;
                break;
            }
        }
        return lines.get(end - 1);
    }

    // Where a page breaks depends on the row heights, so the first section grows a line at a time until
    // every row position has been the last of the first page: a heading or a two-line row lands there
    @Test
    void headingsAndRowsAreNeverSplitFromTheirLines() {
        Line twoLines = new Line("316", "PIMIENTO VERDE KG", "01266859 03264234 08261135", "5,30", "1,90", "10,07");
        for (int first = 1; first <= 40; first++) {
            List<Section> sections = new ArrayList<>();
            sections.add(new Section("Albarán 1 de 02/05/2026", Collections.nCopies(first, line())));
            for (int i = 2; i <= 8; i++) {
                sections.add(new Section("Albarán " + i + " de 02/05/2026", List.of(twoLines, line(), twoLines)));
            }

            List<String> pages = PdfText.pages(renderer.render(invoice(sections, totals())));

            for (int i = 0; i < pages.size(); i++) {
                String where = "page %d with a first section of %d lines".formatted(i + 1, first);
                List<String> lines = pages.get(i).lines().map(String::strip).filter(line -> !line.isEmpty())
                        .toList();
                int titles = indexOfColumnTitles(lines);
                assertThat(titles).as("column titles on " + where).isNotNegative();
                // Never the second line of a row's lots, nor the totals without a line above them
                assertThat(lines.get(titles + 1)).as("first line of " + where)
                        .matches(HEADING.pattern() + "|316 PIMIENTO VERDE KG .*");
                assertThat(lastTableLine(pages.get(i))).as("last line of " + where).doesNotMatch(HEADING);
            }
        }
    }

    private static int indexOfColumnTitles(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Código Concepto")) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void logo() {
        byte[] withLogo = renderer.render(plainNote(ISSUER, PdfText.pngDataUri(120, 40)));
        byte[] withoutLogo = renderer.render(plainNote(ISSUER, null));

        assertThat(PdfText.imageSizes(withLogo)).hasSize(1);
        assertThat(PdfText.text(withLogo)).containsOnlyOnce("ADELINA DELGADO TORRES");
        assertThat(PdfText.imageSizes(withoutLogo)).isEmpty();
        // Without a logo the fiscal name is printed large, above the issuer line
        assertThat(PdfText.text(withoutLogo).split("ADELINA DELGADO TORRES", -1)).hasSize(3);
    }

    @Test
    void oddLogosStayInTheirBox() {
        for (int[] size : new int[][] { { 2000, 100 }, { 100, 1000 } }) {
            byte[] pdf = renderer.render(plainNote(ISSUER, PdfText.pngDataUri(size[0], size[1])));

            List<String> pages = PdfText.pages(pdf);
            assertThat(pages).hasSize(1);
            assertThat(pages.getFirst()).contains(CLIENT).contains("Código", "PIMIENTO VERDE KG", "Total 1.234,56 €");
            float[] drawn = PdfText.imageSizes(pdf).getFirst();
            assertThat(drawn[0]).isLessThanOrEqualTo(80.5f * POINTS_PER_MM);
            assertThat(drawn[1]).isLessThanOrEqualTo(30.5f * POINTS_PER_MM);
            float ratio = (float) size[0] / size[1];
            assertThat(drawn[0] / drawn[1]).as("not deformed").isCloseTo(ratio, within(ratio * 0.05f));
        }
    }

    @Test
    void noIssuerBlock() {
        String text = PdfText.text(renderer.render(plainNote(null, null)));

        assertThat(text).contains("Albarán 31397", "Cliente: 41 SON TERMENS GRAN CHEF SL", "Total 1.234,56 €");
        assertThat(text).doesNotContain("· NIF", "ADELINA");
    }
}
