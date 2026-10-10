package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.dto.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;
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
}
