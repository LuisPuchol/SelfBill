package com.luispuchol.selfbill.selfbill_api.billing.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Line;
import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Section;
import com.luispuchol.selfbill.selfbill_api.billing.document.PrintedDocument.Totals;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.TaxAmounts;

/**
 * Writes the sample documents to {@code target/pdf-samples}, each page also as a PNG, to look at the
 * layout with the eyes: the tests only check that they render.
 */
class PdfSamplesTest {

    private static final Path SAMPLES = Path.of("target", "pdf-samples");
    private static final String[][] ARTICLES = {
            { "316", "PIMIENTO VERDE KG" }, { "101", "TOMATE PERA KG" }, { "205", "LECHUGA ICEBERG UD" },
            { "412", "PLÁTANO CANARIAS KG" }, { "118", "NARANJA ZUMO KG" }, { "530", "PIÑA TROPICAL UD" },
            { "222", "CALABACÍN KG" }, { "223", "BERENJENA KG" }, { "140", "CEBOLLA DULCE KG" },
            { "150", "PATATA NUEVA KG" }, { "160", "AJO MORADO MALLA 250 G" }, { "119", "LIMÓN KG" },
            { "610", "FRESÓN BANDEJA 500 G" }, { "620", "AGUACATE HASS KG" }, { "701", "CHAMPIÑÓN BANDEJA" },
            { "330", "PIMIENTO ROJO CALIFORNIA KG" }, { "1204", "MANZANA GOLDEN DE GIRONA KG" } };

    private final PdfRenderer renderer = new PdfRenderer();
    private final Random random = new Random(3852);

    @BeforeAll
    static void quietFontWarning() {
        PdfText.quietFontWarning();
    }

    @Test
    void writesTheSamples() throws IOException {
        Files.createDirectories(SAMPLES);
        String logo = logo();

        write("factura", invoice("Factura 3852", logo, 4, 3, 7, true));
        write("factura-larga", invoice("Factura 3853", logo, 18, 3, 9, false));
        List<Line> plainLines = lines(8);
        write("albaran-simple", new PrintedDocument("Albarán 31397", "02/05/2026", PdfRendererTest.ISSUER, null,
                PdfRendererTest.CLIENT, List.of(new Section(null, plainLines)), null,
                PrintFormat.amountWithCurrency(sum(plainLines)), false));
        List<Line> valuedLines = lines(6);
        write("albaran-valorado", new PrintedDocument("Albarán 31398", "03/05/2026", PdfRendererTest.ISSUER, logo,
                PdfRendererTest.CLIENT, List.of(new Section(null, valuedLines)),
                totals(sum(valuedLines), false), null, true));
    }

    private PrintedDocument invoice(String title, String logo, int notes, int minLines, int maxLines,
            boolean surcharge) {
        List<Section> sections = new ArrayList<>();
        BigDecimal base = BigDecimal.ZERO;
        for (int i = 0; i < notes; i++) {
            List<Line> lines = lines(minLines + random.nextInt(maxLines - minLines + 1));
            base = base.add(sum(lines));
            sections.add(new Section("Albarán %d de %02d/05/2026".formatted(31397 + i, 2 + i), lines));
        }
        return new PrintedDocument(title, "31/05/2026", PdfRendererTest.ISSUER, logo, PdfRendererTest.CLIENT,
                sections, totals(base, surcharge), null, false);
    }

    private List<Line> lines(int count) {
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String[] article = ARTICLES[random.nextInt(ARTICLES.length)];
            BigDecimal quantity = BigDecimal.valueOf(100 + random.nextInt(4000), 2);
            BigDecimal price = BigDecimal.valueOf(40 + random.nextInt(560), 2);
            if (i == 2) {
                // A return
                quantity = quantity.negate();
            }
            BigDecimal amount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
            lines.add(new Line(article[0], article[1], lots(), PrintFormat.number(quantity),
                    PrintFormat.number(price), PrintFormat.number(amount)));
        }
        return lines;
    }

    private String lots() {
        List<String> lots = new ArrayList<>();
        for (int i = 0, count = random.nextInt(4); i < count; i++) {
            lots.add("0%d26%04d".formatted(1 + random.nextInt(9), random.nextInt(10000)));
        }
        return String.join(" ", lots);
    }

    private static BigDecimal sum(List<Line> lines) {
        return lines.stream()
                .map(line -> new BigDecimal(line.amount().replace(".", "").replace(",", ".")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Totals totals(BigDecimal base, boolean surcharge) {
        BigDecimal vat = new BigDecimal("4.00");
        BigDecimal surchargePercentage = surcharge ? new BigDecimal("0.50") : BigDecimal.ZERO;
        TaxAmounts amounts = TaxAmounts.of(base, vat, surchargePercentage);
        return new Totals(PrintFormat.number(base), PrintFormat.number(vat), PrintFormat.number(amounts.vatAmount()),
                surcharge ? PrintFormat.number(surchargePercentage) : null,
                surcharge ? PrintFormat.number(amounts.surchargeAmount()) : null,
                PrintFormat.amountWithCurrency(amounts.total()));
    }

    private void write(String name, PrintedDocument document) throws IOException {
        byte[] pdf = renderer.render(document);
        Files.write(SAMPLES.resolve(name + ".pdf"), pdf);
        try (PDDocument loaded = Loader.loadPDF(pdf)) {
            assertThat(loaded.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            PDFRenderer pages = new PDFRenderer(loaded);
            for (int page = 0; page < loaded.getNumberOfPages(); page++) {
                ImageIO.write(pages.renderImageWithDPI(page, 110), "png",
                        SAMPLES.resolve(name + "-" + (page + 1) + ".png").toFile());
            }
        }
    }

    // A wide logo with a lettering, as a shop's usually is
    private static String logo() throws IOException {
        BufferedImage image = new BufferedImage(900, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x2E7D32));
        g.fillOval(20, 40, 220, 220);
        g.setColor(new Color(0xF9A825));
        g.fillOval(150, 30, 90, 90);
        g.setColor(new Color(0x1B5E20));
        g.setStroke(new BasicStroke(10));
        g.drawLine(260, 230, 880, 230);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
        g.drawString("FRUTAS", 270, 140);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 64));
        g.drawString("ADELINA", 275, 210);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
