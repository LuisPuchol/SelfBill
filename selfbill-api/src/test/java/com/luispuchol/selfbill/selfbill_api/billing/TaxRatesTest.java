package com.luispuchol.selfbill.selfbill_api.billing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceSeries;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.catalog.client.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Tax rates")
class TaxRatesTest extends DomainIntegrationTest {

    @Test
    @DisplayName("A delivery note freezes the rates its client actually pays")
    void deliveryNoteFreezesAppliedRates() {
        Integer withoutVatWithSurcharge = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE);

        DeliveryNoteResponse withVat = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse withSurcharge = createDeliveryNote(withoutVatWithSurcharge, "10", "2");

        assertThat(withVat.vatPercentage()).isEqualByComparingTo("4");
        assertThat(withVat.surchargePercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.vatPercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.surchargePercentage()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("Moving a delivery note to another client applies that client's regime")
    void movingDeliveryNoteAppliesNewClientRegime() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        DeliveryNoteResponse moved = deliveryNoteService.updateDeliveryNote(deliveryNote.id(),
                deliveryNoteRequest(deliveryNote.code(), withoutVat, "10", "2"));

        assertThat(moved.vatPercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a delivery note keeps the rates it was created with")
    void editingKeepsFrozenRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse edited = deliveryNoteService.updateDeliveryNote(deliveryNote.id(),
                deliveryNoteRequest(deliveryNote.code(), clientId, "5", "2"));

        assertThat(edited.vatPercentage()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("An invoice uses the rates frozen in its delivery notes, not the current ones")
    void invoiceUsesFrozenRates() {
        Integer withSurcharge = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withSurcharge, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        InvoiceResponse invoice = createInvoiceFor(withSurcharge, deliveryNote.id());

        assertThat(invoice.vatPercentage()).isEqualByComparingTo("4");
        assertThat(invoice.vatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoice.surchargePercentage()).isEqualByComparingTo("0.5");
        assertThat(invoice.surchargeAmount()).isEqualByComparingTo("0.10");
        assertThat(invoice.total()).isEqualByComparingTo("20.90");
    }

    @Test
    @DisplayName("Delivery notes with different rates cannot share an invoice")
    void mixedRatesAreRejected() {
        DeliveryNoteResponse before = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));
        DeliveryNoteResponse after = createDeliveryNote(clientId, "10", "2");

        assertFails(ErrorCode.INVOICE_MIXED_TAX_RATES, () -> createInvoice(before.id(), after.id()));
    }

    @Test
    @DisplayName("Rates equal in value but written differently are not mixed")
    void equalRatesWithDifferentScaleAreNotMixed() {
        DeliveryNoteResponse first = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("4.00", "0.50"));
        DeliveryNoteResponse second = createDeliveryNote(clientId, "10", "2");

        assertThat(createInvoice(first.id(), second.id()).vatAmount()).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("Issuing recalculates a draft whose stored amounts went stale")
    void issuingRecalculatesTheDraft() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").id());
        jdbc.update("UPDATE invoices SET vat_amount = 0, total = 0 WHERE id = ?", draft.id());

        InvoiceResponse issued = invoiceService.issueInvoice(draft.id());

        assertThat(issued.vatAmount()).isEqualByComparingTo("0.80");
        assertThat(issued.total()).isEqualByComparingTo("20.80");
    }

    @Test
    @DisplayName("Changing a client's regime reaches its pending delivery notes and drafts")
    void regimeChangeReachesPendingDocuments() {
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse free = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse draft = createInvoice(inDraft.id());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE));

        DeliveryNoteResponse refreshed = deliveryNoteService.getDeliveryNoteById(free.id());
        assertThat(refreshed.vatPercentage()).isEqualByComparingTo("0");
        assertThat(refreshed.surchargePercentage()).isEqualByComparingTo("0.5");
        InvoiceResponse recalculated = invoiceService.getInvoiceById(draft.id());
        assertThat(recalculated.series()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(recalculated.vatAmount()).isEqualByComparingTo("0");
        assertThat(recalculated.surchargeAmount()).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("Changing a client's regime leaves its issued invoices and their delivery notes alone")
    void regimeChangeLeavesIssuedDocumentsAlone() {
        DeliveryNoteResponse issuedNote = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse issued = invoiceService.issueInvoice(createInvoice(issuedNote.id()).id());
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").id());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE));

        assertThat(deliveryNoteService.getDeliveryNoteById(issuedNote.id()).vatPercentage())
                .isEqualByComparingTo("4");
        assertThat(invoiceService.getInvoiceById(issued.id()).vatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoiceService.getInvoiceById(draft.id()).vatAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a client without changing its regime does not refresh rates")
    void nonRegimeEditKeepsRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Renamed SL"));

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.id()).vatPercentage())
                .isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("A regime change applies today's rate, even to older delivery notes (no rate history)")
    void regimeChangeUsesTodaysRate() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withoutVat, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(withoutVat, clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL",
                VatType.WITH_VAT, SurchargeType.WITHOUT_SURCHARGE));

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.id()).vatPercentage())
                .isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Deleting an issued invoice realigns its freed delivery notes with the client's regime")
    void deletingIssuedInvoiceRealignsFreedNotesWithTheClient() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse issued = issue(clientId, deliveryNote);
        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE));

        invoiceService.deleteInvoice(issued.id());

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.id()).vatPercentage())
                .isEqualByComparingTo("0");
        InvoiceResponse reinvoiced = issue(clientId, deliveryNote);
        assertThat(reinvoiced.series()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(reinvoiced.vatAmount()).isEqualByComparingTo("0");
        assertThat(reinvoiced.total()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Deleting an issued invoice realigns its freed delivery notes the other way too")
    void deletingIssuedInvoiceRealignsInTheOtherDirection() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withoutVat, "10", "2");
        InvoiceResponse issued = issue(withoutVat, deliveryNote);
        clientService.updateClient(withoutVat, clientRequest(2, DeliveryNoteFormat.PLAIN, "Client 2 SL",
                VatType.WITH_VAT, SurchargeType.WITHOUT_SURCHARGE));

        invoiceService.deleteInvoice(issued.id());

        InvoiceResponse reinvoiced = issue(withoutVat, deliveryNote);
        assertThat(reinvoiced.series()).isEqualTo(InvoiceSeries.GENERAL);
        assertThat(reinvoiced.vatPercentage()).isEqualByComparingTo("4");
        assertThat(reinvoiced.vatAmount()).isEqualByComparingTo("0.80");
    }

    @Test
    @DisplayName("Deleting an issued invoice keeps the frozen rates when the client's regime did not change")
    void deletingIssuedInvoiceKeepsRatesWhenRegimeUnchanged() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse issued = issue(clientId, deliveryNote);
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        invoiceService.deleteInvoice(issued.id());

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.id()).vatPercentage())
                .isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("Moving a delivery note to a client with the same regime keeps its frozen rates")
    void movingToClientWithSameRegimeKeepsFrozenRate() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));
        Integer sameRegime = createClient(2, VatType.WITH_VAT, SurchargeType.WITHOUT_SURCHARGE);

        DeliveryNoteResponse moved = deliveryNoteService.updateDeliveryNote(deliveryNote.id(),
                deliveryNoteRequest(deliveryNote.code(), sameRegime, "10", "2"));

        assertThat(moved.vatPercentage()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("A surcharge-only regime change keeps the frozen VAT rate")
    void surchargeOnlyRegimeChangeKeepsVatRate() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE));

        DeliveryNoteResponse refreshed = deliveryNoteService.getDeliveryNoteById(deliveryNote.id());
        assertThat(refreshed.vatPercentage()).isEqualByComparingTo("4");
        assertThat(refreshed.surchargePercentage()).isEqualByComparingTo("1.4");
    }

    @Test
    @DisplayName("A regime change that cannot reach its pending delivery notes is not saved")
    void failedRegimeRefreshRollsBackTheClientUpdate() {
        createDeliveryNote(clientId, "10", "2");
        jdbc.update("DELETE FROM tax_config");

        assertFails(ErrorCode.TAX_CONFIG_NOT_FOUND, () -> clientService.updateClient(clientId,
                clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL", VatType.WITHOUT_VAT)));
        assertThat(clientService.getClientById(clientId).vatType()).isEqualTo(VatType.WITH_VAT);
    }
}
