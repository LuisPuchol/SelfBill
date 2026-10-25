package com.luispuchol.selfbill.selfbill_api.billing.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLine;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceLine;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceSeries;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.catalog.article.Article;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.settings.profile.IssuerData;
import com.luispuchol.selfbill.selfbill_api.settings.profile.UserProfileLogo;

class PrintedDocumentsTest {

    private static final LocalDateTime MAY_2 = LocalDateTime.of(2026, 5, 2, 8, 0);
    private static final LocalDateTime MAY_4 = LocalDateTime.of(2026, 5, 4, 8, 0);

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static Client client() {
        return Client.builder().code(7).businessName("OTHER NAME").nif("Z999").address("OTHER STREET")
                .postalCode("07001").city("PALMA").province("ILLES BALEARS").build();
    }

    private static Invoice.InvoiceBuilder invoice() {
        return Invoice.builder().series(InvoiceSeries.GENERAL).code(3852).status(InvoiceStatus.ISSUED)
                .date(LocalDateTime.of(2026, 5, 31, 10, 0)).client(client())
                .subtotal(bd("100.00")).vatPercentage(bd("4.00")).vatAmount(bd("4.00"))
                .surchargePercentage(bd("0.00")).surchargeAmount(bd("0.00")).total(bd("104.00"));
    }

    private static InvoiceLine invoiceLine(int noteCode, LocalDateTime noteDate, String name, int articleCode) {
        return InvoiceLine.builder().deliveryNoteCode(noteCode).deliveryNoteDate(noteDate).articleName(name)
                .articleCode(articleCode).traceabilityCode1("02260424").quantity(bd("5.3")).price(bd("1.9"))
                .total(bd("10.07")).build();
    }

    private static DeliveryNote.DeliveryNoteBuilder note() {
        return DeliveryNote.builder().code(31397).date(MAY_2).total(bd("1234.56")).format(DeliveryNoteFormat.PLAIN)
                .vatPercentage(bd("4.00")).surchargePercentage(bd("0.50")).client(client());
    }

    private static DeliveryNoteLine noteLine(String code1, String code2, String code3) {
        return DeliveryNoteLine.builder()
                .article(Article.builder().code(316).name("PIMIENTO VERDE KG").build())
                .traceabilityCode1(code1).traceabilityCode2(code2).traceabilityCode3(code3)
                .quantity(bd("5.3")).price(bd("1.9")).total(bd("10.07")).build();
    }

    private static Invoice withCopies(Invoice.InvoiceBuilder builder) {
        return builder.issuerFiscalName("ADELINA DELGADO TORRES").issuerNif("X1422821H")
                .issuerAddress("Mercado Pedro Garau Puestos 41,42 - 07007 Palma de Mallorca")
                .clientCode(41).clientBusinessName("SON TERMENS GRAN CHEF SL")
                .clientAddress("PREDIO SON TERMENS S/N").clientPostalCode("07193").clientCity("BUNYOLA")
                .clientProvince("ILLES BALEARS").clientNif("B07688310").build();
    }

    @Test
    void invoiceTitleAndDate() {
        PrintedDocument general = PrintedDocuments.invoice(invoice().build(), null);
        PrintedDocument withoutVat = PrintedDocuments
                .invoice(invoice().series(InvoiceSeries.WITHOUT_VAT).code(12).build(), null);

        assertThat(general.title()).isEqualTo("Factura 3852");
        assertThat(general.date()).isEqualTo("31/05/2026");
        assertThat(withoutVat.title()).isEqualTo("Factura S-12");
    }

    @Test
    void invoiceGroupsLinesByDeliveryNote() {
        Invoice invoice = invoice().lines(List.of(
                invoiceLine(31397, MAY_2, "PIMIENTO VERDE KG", 316),
                invoiceLine(31397, MAY_2, "TOMATE KG", 317),
                invoiceLine(31420, MAY_4, "CEBOLLA KG", 318))).build();

        PrintedDocument document = PrintedDocuments.invoice(invoice, null);

        assertThat(document.sections()).extracting(PrintedDocument.Section::heading)
                .containsExactly("Albarán 31397 de 02/05/2026", "Albarán 31420 de 04/05/2026");
        assertThat(document.sections()).extracting(s -> s.lines().size()).containsExactly(2, 1);
        assertThat(document.sections().get(0).lines().get(0)).isEqualTo(
                new PrintedDocument.Line("316", "PIMIENTO VERDE KG", "02260424", "5,30", "1,90", "10,07"));
    }

    @Test
    void invoiceTotals() {
        Invoice invoice = invoice().subtotal(bd("1234.56")).vatPercentage(bd("4.00")).vatAmount(bd("49.38"))
                .surchargePercentage(bd("0.50")).surchargeAmount(bd("6.17")).total(bd("1290.11")).build();
        Invoice noSurcharge = invoice().series(InvoiceSeries.WITHOUT_VAT).vatPercentage(bd("0.00"))
                .vatAmount(bd("0.00")).build();

        PrintedDocument document = PrintedDocuments.invoice(invoice, null);
        PrintedDocument plain = PrintedDocuments.invoice(noSurcharge, null);

        assertThat(document.totals()).isEqualTo(
                new PrintedDocument.Totals("1.234,56", "4,00", "49,38", "0,50", "6,17", "1.290,11 €"));
        assertThat(document.plainTotal()).isNull();
        assertThat(document.nonFiscal()).isFalse();
        assertThat(plain.totals().surchargePercentage()).isNull();
        assertThat(plain.totals().surchargeAmount()).isNull();
        assertThat(plain.totals().vatPercentage()).isEqualTo("0,00");
        assertThat(plain.totals().vatAmount()).isEqualTo("0,00");
    }

    @Test
    void invoiceUsesItsCopies() {
        PrintedDocument document = PrintedDocuments.invoice(withCopies(invoice()), null);
        Invoice old = invoice().build();

        assertThat(document.issuer()).isEqualTo(new PrintedDocument.Issuer("ADELINA DELGADO TORRES", "X1422821H",
                "Mercado Pedro Garau Puestos 41,42 - 07007 Palma de Mallorca"));
        assertThat(document.client()).containsExactly("Cliente: 41 SON TERMENS GRAN CHEF SL",
                "PREDIO SON TERMENS S/N", "07193 BUNYOLA (ILLES BALEARS)", "CIF/NIF: B07688310");
        assertThat(PrintedDocuments.invoice(old, null).issuer()).isNull();
    }

    @Test
    void clientLinesLeaveOutWhatIsMissing() {
        Invoice.InvoiceBuilder base = invoice().clientCode(41).clientBusinessName("X");

        assertThat(PrintedDocuments.invoice(base.clientPostalCode("07193").clientCity("BUNYOLA").build(), null)
                .client()).containsExactly("Cliente: 41 X", "07193 BUNYOLA");
        assertThat(PrintedDocuments.invoice(base.clientProvince("ILLES BALEARS").clientPostalCode(null).build(), null)
                .client()).containsExactly("Cliente: 41 X", "BUNYOLA (ILLES BALEARS)");
        assertThat(PrintedDocuments.invoice(
                base.clientProvince(null).clientCity(null).clientPostalCode(null).build(), null).client())
                .containsExactly("Cliente: 41 X");
        assertThat(PrintedDocuments.invoice(base.clientNif("B1").clientAddress("CALLE 1").build(), null).client())
                .containsExactly("Cliente: 41 X", "CALLE 1", "CIF/NIF: B1");
        assertThat(PrintedDocuments.invoice(
                base.clientProvince("ILLES BALEARS").clientNif(null).clientAddress(null).build(), null).client())
                .containsExactly("Cliente: 41 X", "(ILLES BALEARS)");
    }

    @Test
    void plainDeliveryNote() {
        DeliveryNote note = note().lines(List.of(noteLine("02260424", null, null))).build();

        PrintedDocument document = PrintedDocuments.deliveryNote(note, null, null);

        assertThat(document.title()).isEqualTo("Albarán 31397");
        assertThat(document.date()).isEqualTo("02/05/2026");
        assertThat(document.sections()).hasSize(1);
        assertThat(document.sections().get(0).heading()).isNull();
        assertThat(document.totals()).isNull();
        assertThat(document.plainTotal()).isEqualTo("1.234,56 €");
        assertThat(document.nonFiscal()).isFalse();
    }

    @Test
    void valuedDeliveryNote() {
        DeliveryNote valued = note().format(DeliveryNoteFormat.VALUED).total(bd("100.00")).build();
        DeliveryNote small = note().format(DeliveryNoteFormat.VALUED).total(bd("13.00")).build();

        PrintedDocument document = PrintedDocuments.deliveryNote(valued, null, null);

        assertThat(document.totals()).isEqualTo(
                new PrintedDocument.Totals("100,00", "4,00", "4,00", "0,50", "0,50", "104,50 €"));
        assertThat(document.plainTotal()).isNull();
        assertThat(document.nonFiscal()).isTrue();
        assertThat(PrintedDocuments.deliveryNote(small, null, null).totals().surchargeAmount()).isEqualTo("0,07");
    }

    @Test
    void noteOutsideAnIssuedInvoiceUsesCurrentData() {
        IssuerData issuer = new IssuerData("CURRENT NAME", "N1", "CURRENT ADDRESS");
        DeliveryNote free = note().lines(List.of(noteLine("02260424", null, null))).build();
        DeliveryNote inDraft = note().invoice(invoice().status(InvoiceStatus.DRAFT).build())
                .lines(List.of(noteLine("02260424", null, null))).build();

        for (DeliveryNote note : List.of(free, inDraft)) {
            PrintedDocument document = PrintedDocuments.deliveryNote(note, issuer, null);

            assertThat(document.issuer())
                    .isEqualTo(new PrintedDocument.Issuer("CURRENT NAME", "N1", "CURRENT ADDRESS"));
            assertThat(document.client()).containsExactly("Cliente: 7 OTHER NAME", "OTHER STREET",
                    "07001 PALMA (ILLES BALEARS)", "CIF/NIF: Z999");
            assertThat(document.sections().get(0).lines().get(0).code()).isEqualTo("316");
            assertThat(document.sections().get(0).lines().get(0).concept()).isEqualTo("PIMIENTO VERDE KG");
        }
        assertThat(PrintedDocuments.deliveryNote(free, null, null).issuer()).isNull();
    }

    @Test
    void noteInIssuedInvoiceUsesTheInvoiceCopy() {
        Invoice invoice = withCopies(invoice().lines(List.of(
                invoiceLine(31397, MAY_2, "FROZEN NAME", 316),
                invoiceLine(31420, MAY_4, "ANOTHER NOTE", 318))));
        DeliveryNote note = note().invoice(invoice).lines(List.of(noteLine("02260424", null, null))).build();

        PrintedDocument document = PrintedDocuments.deliveryNote(note, new IssuerData("CURRENT", "N", "A"), null);

        assertThat(document.issuer().fiscalName()).isEqualTo("ADELINA DELGADO TORRES");
        assertThat(document.client().get(0)).isEqualTo("Cliente: 41 SON TERMENS GRAN CHEF SL");
        assertThat(document.sections()).hasSize(1);
        assertThat(document.sections().get(0).lines()).extracting(PrintedDocument.Line::concept)
                .containsExactly("FROZEN NAME");
    }

    @Test
    void traceabilityJoinsTheLotsItHas() {
        DeliveryNote note = note().lines(List.of(noteLine("02260424", null, "02020524"))).build();

        PrintedDocument document = PrintedDocuments.deliveryNote(note, null, null);

        assertThat(document.sections().get(0).lines().get(0).traceability()).isEqualTo("02260424 02020524");
    }

    @Test
    void amountsFormat() {
        DeliveryNoteLine line = noteLine("02260424", null, null);
        line.setQuantity(bd("-2"));
        line.setTotal(bd("1234567.8"));
        DeliveryNote note = note().lines(List.of(line)).build();

        PrintedDocument.Line printed = PrintedDocuments.deliveryNote(note, null, null).sections().get(0).lines().get(0);

        assertThat(printed.amount()).isEqualTo("1.234.567,80");
        assertThat(printed.quantity()).isEqualTo("-2,00");
    }

    @Test
    void logoAsDataUri() {
        byte[] bytes = {1, 2, 3, 4};

        assertThat(PrintedDocuments.invoice(invoice().build(), new UserProfileLogo(bytes, "image/png")).logo())
                .isEqualTo("data:image/png;base64," + Base64.getEncoder().encodeToString(bytes));
        assertThat(PrintedDocuments.invoice(invoice().build(), null).logo()).isNull();
    }

    @Test
    void controlCharactersPrintAsSpaces() {
        // \u000B is Word's soft line break; XML cannot carry it
        Invoice invoice = withCopies(invoice().lines(List.of(invoiceLine(31397, MAY_2, "SOFT\u000BBREAK", 316))));
        invoice.setClientBusinessName("CLIENT\u000BNAME");
        invoice.setIssuerFiscalName("ISSUER\u0000NAME");
        DeliveryNote note = note().client(Client.builder().code(7).businessName("NOTE\u000BCLIENT").build())
                .lines(List.of(noteLine("LOT\u001F1", null, null))).build();
        note.getLines().get(0).getArticle().setName("ARTICLE\u000BNAME");

        PrintedDocument printed = PrintedDocuments.invoice(invoice, null);
        PrintedDocument printedNote = PrintedDocuments.deliveryNote(note, new IssuerData("A\u000CB", "N", "ADDR"), null);

        assertThat(printed.client().get(0)).isEqualTo("Cliente: 41 CLIENT NAME");
        assertThat(printed.issuer().fiscalName()).isEqualTo("ISSUER NAME");
        assertThat(printed.sections().get(0).lines().get(0).concept()).isEqualTo("SOFT BREAK");
        assertThat(printedNote.client().get(0)).isEqualTo("Cliente: 7 NOTE CLIENT");
        assertThat(printedNote.issuer().fiscalName()).isEqualTo("A B");
        assertThat(printedNote.sections().get(0).lines().get(0).concept()).isEqualTo("ARTICLE NAME");
        assertThat(printedNote.sections().get(0).lines().get(0).traceability()).isEqualTo("LOT 1");
    }

    @Test
    void valuedNoteInAnIssuedInvoiceTakesLinesFromTheInvoiceAndTotalsFromTheNote() {
        Invoice invoice = withCopies(invoice().lines(List.of(
                invoiceLine(31397, MAY_2, "FROZEN NAME", 316),
                invoiceLine(31420, MAY_4, "ANOTHER NOTE", 318))));
        DeliveryNote note = note().format(DeliveryNoteFormat.VALUED).total(bd("100.00")).invoice(invoice)
                .lines(List.of(noteLine("02260424", null, null))).build();

        PrintedDocument document = PrintedDocuments.deliveryNote(note, null, null);

        assertThat(document.sections().get(0).lines()).extracting(PrintedDocument.Line::concept)
                .containsExactly("FROZEN NAME");
        assertThat(document.totals()).isEqualTo(
                new PrintedDocument.Totals("100,00", "4,00", "4,00", "0,50", "0,50", "104,50 €"));
        assertThat(document.nonFiscal()).isTrue();
        assertThat(document.issuer().fiscalName()).isEqualTo("ADELINA DELGADO TORRES");
    }
}
