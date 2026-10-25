package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/** Reads a rendered PDF back as text, the way the tests check what is printed. */
final class PdfText {

    private PdfText() {
    }

    /** What application.properties does for the app: the plain tests run without Spring's logging setup. */
    static void quietFontWarning() {
        ((Logger) LoggerFactory.getLogger("org.apache.fontbox.ttf.CmapSubtable")).setLevel(Level.ERROR);
    }

    static String text(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The text of each page, top to bottom, whatever order the PDF draws it in. */
    static List<String> pages(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(document));
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A plain PNG of the given size, as the data: URI the printed document carries. */
    static String pngDataUri(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, (x / 10 + y / 10) % 2 == 0 ? 0x2E7D32 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
