package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLine;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceLine;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.TaxAmounts;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.settings.profile.IssuerData;
import com.luispuchol.selfbill.selfbill_api.settings.profile.UserProfileLogo;

/**
 * Builds what a document prints from its entities. Pure: it reads no repository and keeps the
 * entities' line order (the database order comes from {@code @OrderBy} on the collections).
 */
public final class PrintedDocuments {

    private PrintedDocuments() {
    }

    public static PrintedDocument invoice(Invoice invoice, UserProfileLogo logo) {
        boolean surcharge = hasSurcharge(invoice.getSurchargePercentage());
        return new PrintedDocument(
                "Factura " + invoice.getNumber(),
                PrintFormat.date(invoice.getDate()),
                issuerCopy(invoice),
                dataUri(logo),
                clientCopy(invoice),
                invoiceSections(invoice.getLines()),
                new PrintedDocument.Totals(
                        PrintFormat.number(invoice.getSubtotal()),
                        PrintFormat.number(invoice.getVatPercentage()),
                        PrintFormat.number(invoice.getVatAmount()),
                        surcharge ? PrintFormat.number(invoice.getSurchargePercentage()) : null,
                        surcharge ? PrintFormat.number(invoice.getSurchargeAmount()) : null,
                        PrintFormat.amountWithCurrency(invoice.getTotal())),
                null,
                false);
    }

    public static PrintedDocument deliveryNote(DeliveryNote note, IssuerData currentIssuer, UserProfileLogo logo) {
        Invoice invoice = note.getInvoice();
        boolean inIssuedInvoice = invoice != null && invoice.getStatus() == InvoiceStatus.ISSUED;

        PrintedDocument.Issuer issuer;
        List<String> client;
        List<PrintedDocument.Line> lines;
        if (inIssuedInvoice) {
            issuer = issuerCopy(invoice);
            client = clientCopy(invoice);
            // Codes are frozen once the invoice is issued and unique inside it
            lines = invoice.getLines().stream()
                    .filter(line -> Objects.equals(line.getDeliveryNoteCode(), note.getCode()))
                    .map(PrintedDocuments::line)
                    .toList();
        } else {
            issuer = currentIssuer == null ? null : new PrintedDocument.Issuer(currentIssuer.fiscalName(),
                    currentIssuer.nif(), currentIssuer.address());
            client = clientLines(note.getClient());
            lines = note.getLines().stream().map(PrintedDocuments::line).toList();
        }

        boolean valued = note.getFormat() == DeliveryNoteFormat.VALUED;
        return new PrintedDocument(
                "Albarán " + note.getCode(),
                PrintFormat.date(note.getDate()),
                issuer,
                dataUri(logo),
                client,
                List.of(new PrintedDocument.Section(null, lines)),
                valued ? valuedTotals(note) : null,
                valued ? null : PrintFormat.amountWithCurrency(note.getTotal()),
                valued);
    }

    private static PrintedDocument.Totals valuedTotals(DeliveryNote note) {
        TaxAmounts amounts = TaxAmounts.of(note.getTotal(), note.getVatPercentage(), note.getSurchargePercentage());
        boolean surcharge = hasSurcharge(note.getSurchargePercentage());
        return new PrintedDocument.Totals(
                PrintFormat.number(note.getTotal()),
                PrintFormat.number(note.getVatPercentage()),
                PrintFormat.number(amounts.vatAmount()),
                surcharge ? PrintFormat.number(note.getSurchargePercentage()) : null,
                surcharge ? PrintFormat.number(amounts.surchargeAmount()) : null,
                PrintFormat.amountWithCurrency(amounts.total()));
    }

    private static boolean hasSurcharge(BigDecimal percentage) {
        return percentage != null && percentage.signum() != 0;
    }

    /** Consecutive invoice lines of the same delivery note code make one section. */
    private static List<PrintedDocument.Section> invoiceSections(List<InvoiceLine> invoiceLines) {
        List<PrintedDocument.Section> sections = new ArrayList<>();
        InvoiceLine first = null;
        List<PrintedDocument.Line> current = new ArrayList<>();
        for (InvoiceLine line : invoiceLines) {
            if (first != null && !Objects.equals(first.getDeliveryNoteCode(), line.getDeliveryNoteCode())) {
                sections.add(section(first, current));
                current = new ArrayList<>();
                first = null;
            }
            if (first == null) {
                first = line;
            }
            current.add(line(line));
        }
        if (first != null) {
            sections.add(section(first, current));
        }
        return sections;
    }

    private static PrintedDocument.Section section(InvoiceLine first, List<PrintedDocument.Line> lines) {
        return new PrintedDocument.Section("Albarán " + first.getDeliveryNoteCode() + " de "
                + PrintFormat.date(first.getDeliveryNoteDate()), lines);
    }

    private static PrintedDocument.Line line(InvoiceLine line) {
        return new PrintedDocument.Line(String.valueOf(line.getArticleCode()), line.getArticleName(),
                traceability(line.getTraceabilityCode1(), line.getTraceabilityCode2(), line.getTraceabilityCode3()),
                PrintFormat.number(line.getQuantity()), PrintFormat.number(line.getPrice()),
                PrintFormat.number(line.getTotal()));
    }

    private static PrintedDocument.Line line(DeliveryNoteLine line) {
        return new PrintedDocument.Line(String.valueOf(line.getArticle().getCode()), line.getArticle().getName(),
                traceability(line.getTraceabilityCode1(), line.getTraceabilityCode2(), line.getTraceabilityCode3()),
                PrintFormat.number(line.getQuantity()), PrintFormat.number(line.getPrice()),
                PrintFormat.number(line.getTotal()));
    }

    private static String traceability(String... lots) {
        return Stream.of(lots).filter(PrintedDocuments::hasText).collect(Collectors.joining(" "));
    }

    /** An old invoice issued without a profile has no issuer copy. */
    private static PrintedDocument.Issuer issuerCopy(Invoice invoice) {
        if (invoice.getIssuerFiscalName() == null) {
            return null;
        }
        return new PrintedDocument.Issuer(invoice.getIssuerFiscalName(), invoice.getIssuerNif(),
                invoice.getIssuerAddress());
    }

    private static List<String> clientCopy(Invoice invoice) {
        return clientLines(invoice.getClientCode(), invoice.getClientBusinessName(), invoice.getClientAddress(),
                invoice.getClientPostalCode(), invoice.getClientCity(), invoice.getClientProvince(),
                invoice.getClientNif());
    }

    private static List<String> clientLines(Client client) {
        return clientLines(client.getCode(), client.getBusinessName(), client.getAddress(),
                client.getPostalCode(), client.getCity(), client.getProvince(), client.getNif());
    }

    private static List<String> clientLines(Integer code, String name, String address, String postalCode,
            String city, String province, String nif) {
        List<String> lines = new ArrayList<>();
        lines.add("Cliente: " + joined(code == null ? null : String.valueOf(code), name));
        if (hasText(address)) {
            lines.add(address);
        }
        String town = joined(postalCode, city);
        if (hasText(province)) {
            town = joined(town, "(" + province + ")");
        }
        if (!town.isEmpty()) {
            lines.add(town);
        }
        if (hasText(nif)) {
            lines.add("CIF/NIF: " + nif);
        }
        return lines;
    }

    private static String joined(String... parts) {
        return Stream.of(parts).filter(PrintedDocuments::hasText).collect(Collectors.joining(" "));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String dataUri(UserProfileLogo logo) {
        if (logo == null) {
            return null;
        }
        return "data:" + logo.contentType() + ";base64," + Base64.getEncoder().encodeToString(logo.data());
    }
}
