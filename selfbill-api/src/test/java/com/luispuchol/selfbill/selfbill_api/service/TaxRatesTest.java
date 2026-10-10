package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceSeries;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Tax rates")
class TaxRatesTest extends DomainIntegrationTest {

    @Test
    @DisplayName("A delivery note freezes the rates its client actually pays")
    void deliveryNoteFreezesAppliedRates() {
        Integer withoutVatWithSurcharge = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE);

        DeliveryNoteResponse withVat = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse withSurcharge = createDeliveryNote(withoutVatWithSurcharge, "10", "2");

        assertThat(withVat.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(withVat.getSurchargePercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.getVatPercentage()).isEqualByComparingTo("0");
        assertThat(withSurcharge.getSurchargePercentage()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("Moving a delivery note to another client applies that client's regime")
    void movingDeliveryNoteAppliesNewClientRegime() {
        Integer withoutVat = createClient(2, VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");

        DeliveryNoteResponse moved = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), withoutVat, "10", "2"));

        assertThat(moved.getVatPercentage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a delivery note keeps the rates it was created with")
    void editingKeepsFrozenRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        DeliveryNoteResponse edited = deliveryNoteService.updateDeliveryNote(deliveryNote.getId(),
                deliveryNoteRequest(deliveryNote.getCode(), clientId, "5", "2"));

        assertThat(edited.getVatPercentage()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("An invoice uses the rates frozen in its delivery notes, not the current ones")
    void invoiceUsesFrozenRates() {
        Integer withSurcharge = createClient(2, VatType.WITH_VAT, SurchargeType.WITH_SURCHARGE);
        DeliveryNoteResponse deliveryNote = createDeliveryNote(withSurcharge, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        InvoiceResponse invoice = createInvoiceFor(withSurcharge, deliveryNote.getId());

        assertThat(invoice.getVatPercentage()).isEqualByComparingTo("4");
        assertThat(invoice.getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoice.getSurchargePercentage()).isEqualByComparingTo("0.5");
        assertThat(invoice.getSurchargeAmount()).isEqualByComparingTo("0.10");
        assertThat(invoice.getTotal()).isEqualByComparingTo("20.90");
    }

    @Test
    @DisplayName("Delivery notes with different rates cannot share an invoice")
    void mixedRatesAreRejected() {
        DeliveryNoteResponse before = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));
        DeliveryNoteResponse after = createDeliveryNote(clientId, "10", "2");

        assertFails(ErrorCode.INVOICE_MIXED_TAX_RATES, () -> createInvoice(before.getId(), after.getId()));
    }

    @Test
    @DisplayName("Rates equal in value but written differently are not mixed")
    void equalRatesWithDifferentScaleAreNotMixed() {
        DeliveryNoteResponse first = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("4.00", "0.50"));
        DeliveryNoteResponse second = createDeliveryNote(clientId, "10", "2");

        assertThat(createInvoice(first.getId(), second.getId()).getVatAmount()).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("Issuing recalculates a draft whose stored amounts went stale")
    void issuingRecalculatesTheDraft() {
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").getId());
        jdbc.update("UPDATE invoices SET vat_amount = 0, total = 0 WHERE id = ?", draft.getId());

        InvoiceResponse issued = invoiceService.issueInvoice(draft.getId());

        assertThat(issued.getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(issued.getTotal()).isEqualByComparingTo("20.80");
    }

    @Test
    @DisplayName("Changing a client's regime reaches its pending delivery notes and drafts")
    void regimeChangeReachesPendingDocuments() {
        DeliveryNoteResponse inDraft = createDeliveryNote(clientId, "10", "2");
        DeliveryNoteResponse free = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse draft = createInvoice(inDraft.getId());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITH_SURCHARGE));

        DeliveryNoteResponse refreshed = deliveryNoteService.getDeliveryNoteById(free.getId());
        assertThat(refreshed.getVatPercentage()).isEqualByComparingTo("0");
        assertThat(refreshed.getSurchargePercentage()).isEqualByComparingTo("0.5");
        InvoiceResponse recalculated = invoiceService.getInvoiceById(draft.getId());
        assertThat(recalculated.getSeries()).isEqualTo(InvoiceSeries.WITHOUT_VAT);
        assertThat(recalculated.getVatAmount()).isEqualByComparingTo("0");
        assertThat(recalculated.getSurchargeAmount()).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("Changing a client's regime leaves its issued invoices and their delivery notes alone")
    void regimeChangeLeavesIssuedDocumentsAlone() {
        DeliveryNoteResponse issuedNote = createDeliveryNote(clientId, "10", "2");
        InvoiceResponse issued = invoiceService.issueInvoice(createInvoice(issuedNote.getId()).getId());
        InvoiceResponse draft = createInvoice(createDeliveryNote(clientId, "10", "2").getId());

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Client 1 SL",
                VatType.WITHOUT_VAT, SurchargeType.WITHOUT_SURCHARGE));

        assertThat(deliveryNoteService.getDeliveryNoteById(issuedNote.getId()).getVatPercentage())
                .isEqualByComparingTo("4");
        assertThat(invoiceService.getInvoiceById(issued.getId()).getVatAmount()).isEqualByComparingTo("0.80");
        assertThat(invoiceService.getInvoiceById(draft.getId()).getVatAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Editing a client without changing its regime does not refresh rates")
    void nonRegimeEditKeepsRates() {
        DeliveryNoteResponse deliveryNote = createDeliveryNote(clientId, "10", "2");
        taxConfigService.updateTaxConfig(taxes("10", "1.4"));

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Renamed SL"));

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.getId()).getVatPercentage())
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

        assertThat(deliveryNoteService.getDeliveryNoteById(deliveryNote.getId()).getVatPercentage())
                .isEqualByComparingTo("10");
    }
}
