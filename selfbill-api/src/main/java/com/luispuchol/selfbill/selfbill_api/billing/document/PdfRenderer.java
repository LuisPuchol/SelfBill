package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;

/** Lays out a printed document with the {@code pdf/document.html} template and turns it into PDF. */
@Component
public class PdfRenderer {

    private static final Locale SPANISH = Locale.of("es", "ES");

    static {
        // OpenHTMLtoPDF writes three INFO lines to the console on every render; its warnings are kept
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
    }

    private final TemplateEngine templateEngine = templateEngine();

    // Standalone, not Spring MVC's view engine: this one only knows the PDF templates
    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("pdf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    public byte[] render(PrintedDocument document) {
        Context context = new Context(SPANISH);
        context.setVariable("document", document);
        String html = templateEngine.process("document", context);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render the PDF of " + document.title(), e);
        }
        return out.toByteArray();
    }
}
