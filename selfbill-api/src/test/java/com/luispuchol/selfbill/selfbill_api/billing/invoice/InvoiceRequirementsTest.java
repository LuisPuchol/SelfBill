package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Invoice requirements")
class InvoiceRequirementsTest extends DomainIntegrationTest {

    private static final LocalDateTime OCT_31 = LocalDateTime.of(2026, 10, 31, 23, 0);
    private static final LocalDateTime NOV_1 = LocalDateTime.of(2026, 11, 1, 0, 30);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "nif", "address", "postalCode", "city" })
    @DisplayName("An invoice cannot be issued while its client lacks fiscal data")
    void issuingRequiresClientFiscalData(String missing) {
        Integer client = clientService.createClient(clientWithout(missing)).id();
        InvoiceResponse draft = createInvoiceFor(client, createDeliveryNote(client, "1", "1").id());

        assertFails(ErrorCode.CLIENT_FISCAL_DATA_MISSING, () -> invoiceService.issueInvoice(draft.id()));
        assertThat(invoiceService.getInvoiceById(draft.id()).status()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("An invoice cannot be issued without the user profile, which holds the issuer's fiscal data")
    void issuingRequiresUserProfile() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "1", "1").id());
        jdbc.update("DELETE FROM user_profile");

        assertFails(ErrorCode.USER_PROFILE_NOT_FOUND, () -> invoiceService.issueInvoice(draft.id()));
    }

    @Test
    @DisplayName("A draft of a deleted client cannot be issued")
    void draftOfDeletedClientCannotBeIssued() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "1", "1").id());
        clientService.deleteClient(clientId);

        assertFails(ErrorCode.CLIENT_NOT_FOUND, () -> invoiceService.issueInvoice(draft.id()));
    }

    @Test
    @DisplayName("Delivery notes of different months cannot share an invoice")
    void deliveryNotesOfDifferentMonthsAreRejected() {
        taxConfigService.createTaxConfig(taxes(LocalDate.of(2025, 1, 1), "4", "0.5"));
        DeliveryNoteResponse october = createDeliveryNoteOn(OCT_31);
        DeliveryNoteResponse november = createDeliveryNoteOn(NOV_1);
        DeliveryNoteResponse octoberLastYear = createDeliveryNoteOn(OCT_31.minusYears(1));

        assertFails(ErrorCode.INVOICE_MIXED_MONTHS, () -> createInvoice(october.id(), november.id()));
        assertFails(ErrorCode.INVOICE_MIXED_MONTHS, () -> createInvoice(october.id(), octoberLastYear.id()));
        assertThat(createInvoice(october.id(), createDeliveryNoteOn(OCT_31.minusDays(30)).id()).sections())
                .hasSize(2);
    }

    @Test
    @DisplayName("A delivery note of a draft cannot move to another month than the rest of the draft")
    void deliveryNoteOfDraftCannotLeaveItsMonth() {
        DeliveryNoteResponse staying = createDeliveryNoteOn(OCT_31);
        DeliveryNoteResponse moving = createDeliveryNoteOn(OCT_31);
        createInvoice(staying.id(), moving.id());

        assertFails(ErrorCode.INVOICE_MIXED_MONTHS, () -> deliveryNoteService.updateDeliveryNote(moving.id(),
                deliveryNoteRequest(moving.code(), clientId, "1", "1", NOV_1)));
        assertThat(deliveryNoteService.getDeliveryNoteById(moving.id()).date()).isEqualTo(OCT_31);
    }

    private static ClientRequest clientWithout(String field) {
        ClientRequest.ClientRequestBuilder client = ClientRequest.builder()
                .code(2)
                .businessName("Client 2 SL")
                .name("Client 2")
                .nif("B00000002")
                .address("Calle Mayor 2")
                .city("Valencia")
                .postalCode("46001")
                .vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN);
        switch (field) {
            case "nif" -> client.nif(null);
            case "address" -> client.address("  ");
            case "postalCode" -> client.postalCode(null);
            case "city" -> client.city("");
            default -> throw new IllegalArgumentException(field);
        }
        return client.build();
    }
}
