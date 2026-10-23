package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Create and issue an invoice in one step")
class InvoiceIssueNowTest extends DomainIntegrationTest {

    @Test
    @DisplayName("Issues an invoice with its number")
    void issuesAnInvoiceWithItsNumber() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "1", "1");

        InvoiceResponse invoice = invoiceService.createAndIssue(invoiceRequest(note.id()));

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.number()).isEqualTo("1");
        assertThat(deliveryNoteService.getDeliveryNoteById(note.id()).invoiceStatus())
                .isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    @DisplayName("Issues the same amounts and lines as creating a draft and then issuing it")
    void issuesWhatTheTwoStepsIssue() {
        Integer client = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        DeliveryNoteResponse twoStepNote = createDeliveryNote(client, "10", "2");
        DeliveryNoteResponse oneStepNote = createDeliveryNote(client, "10", "2");

        InvoiceResponse twoSteps = invoiceService.issueInvoice(createInvoiceFor(client, twoStepNote.id()).id());
        InvoiceResponse oneStep = invoiceService.createAndIssue(invoiceRequestFor(client, oneStepNote.id()));

        assertThat(twoSteps.total()).isEqualByComparingTo("20.90");
        assertSameContent(oneStep, twoSteps);
        assertSameContent(invoiceService.getInvoiceById(oneStep.id()), invoiceService.getInvoiceById(twoSteps.id()));
    }

    @Test
    @DisplayName("A client without fiscal data creates nothing")
    void missingClientDataCreatesNothing() {
        Integer client = clientService.createClient(ClientRequest.builder()
                .code(2)
                .businessName("Client 2 SL")
                .name("Client 2")
                .city("Valencia")
                .postalCode("46001")
                .address("Calle Mayor 2")
                .vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN)
                .build()).id();
        DeliveryNoteResponse note = createDeliveryNote(client, "1", "1");

        assertFails(ErrorCode.CLIENT_FISCAL_DATA_MISSING,
                () -> invoiceService.createAndIssue(invoiceRequestFor(client, note.id())));

        assertNothingCreated(note);
    }

    @Test
    @DisplayName("Without the user profile it creates nothing")
    void missingProfileCreatesNothing() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "1", "1");
        jdbc.update("DELETE FROM user_profile");

        assertFails(ErrorCode.USER_PROFILE_NOT_FOUND, () -> invoiceService.createAndIssue(invoiceRequest(note.id())));

        assertNothingCreated(note);
    }

    @Test
    @DisplayName("An already invoiced note creates nothing")
    void alreadyInvoicedNoteCreatesNothing() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "1", "1");
        createInvoice(note.id());

        assertFails(ErrorCode.INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED,
                () -> invoiceService.createAndIssue(invoiceRequest(note.id())));

        assertThat(count("invoices")).isEqualTo(1);
    }

    private InvoiceRequest invoiceRequestFor(Integer client, Integer... noteIds) {
        return InvoiceRequest.builder().clientId(client).deliveryNoteIds(List.of(noteIds)).build();
    }

    private static void assertSameContent(InvoiceResponse actual, InvoiceResponse expected) {
        assertThat(actual.subtotal()).isEqualByComparingTo(expected.subtotal());
        assertThat(actual.vatPercentage()).isEqualByComparingTo(expected.vatPercentage());
        assertThat(actual.vatAmount()).isEqualByComparingTo(expected.vatAmount());
        assertThat(actual.surchargePercentage()).isEqualByComparingTo(expected.surchargePercentage());
        assertThat(actual.surchargeAmount()).isEqualByComparingTo(expected.surchargeAmount());
        assertThat(actual.total()).isEqualByComparingTo(expected.total());
        assertThat(actual.sections()).singleElement().satisfies(section -> assertThat(section.lines())
                .singleElement()
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected.sections().getFirst().lines().getFirst()));
    }

    private void assertNothingCreated(DeliveryNoteResponse note) {
        assertThat(count("invoices")).isZero();
        assertThat(deliveryNoteService.getDeliveryNoteById(note.id()).invoiceStatus()).isNull();
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
