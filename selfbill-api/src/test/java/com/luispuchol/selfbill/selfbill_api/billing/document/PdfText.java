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
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
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
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes(width, height));
    }

    static byte[] pngBytes(int width, int height) {
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
        return out.toByteArray();
    }

    /** The drawn width and height, in points, of each image on the first page. */
    static List<float[]> imageSizes(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            ImageSizes sizes = new ImageSizes();
            sizes.processPage(document.getPage(0));
            return sizes.sizes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // PDFBox's PrintImageLocations example: the current matrix at each "Do" of an image is its drawn size
    private static final class ImageSizes extends PDFStreamEngine {

        private final List<float[]> sizes = new ArrayList<>();

        ImageSizes() {
            addOperator(new Concatenate(this));
            addOperator(new DrawObject(this));
            addOperator(new SetGraphicsStateParameters(this));
            addOperator(new Save(this));
            addOperator(new Restore(this));
            addOperator(new SetMatrix(this));
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            if (!OperatorName.DRAW_OBJECT.equals(operator.getName())) {
                super.processOperator(operator, operands);
                return;
            }
            PDXObject xobject = getResources().getXObject((COSName) operands.getFirst());
            if (xobject instanceof PDImageXObject) {
                Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
                sizes.add(new float[] { ctm.getScalingFactorX(), ctm.getScalingFactorY() });
            } else if (xobject instanceof PDFormXObject form) {
                showForm(form);
            }
        }
    }
}
