package com.luispuchol.selfbill.selfbill_api.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeliveryNoteTaxRegime {

    private final TaxConfigService taxConfigService;

    public void apply(DeliveryNote deliveryNote, Client client) {
        boolean vatChanged = deliveryNote.getVatType() != client.getVatType();
        boolean surchargeChanged = deliveryNote.getSurchargeType() != client.getSurchargeType();
        if (!vatChanged && !surchargeChanged) {
            return;
        }

        TaxConfig taxConfig = taxConfigService.getCurrent();
        if (vatChanged) {
            deliveryNote.setVatType(client.getVatType());
            deliveryNote.setVatPercentage(client.getVatType() == VatType.WITH_VAT
                    ? taxConfig.getVatPercentage() : BigDecimal.ZERO);
        }
        if (surchargeChanged) {
            deliveryNote.setSurchargeType(client.getSurchargeType());
            deliveryNote.setSurchargePercentage(client.getSurchargeType() == SurchargeType.WITH_SURCHARGE
                    ? taxConfig.getSurchargePercentage() : BigDecimal.ZERO);
        }
    }
}
