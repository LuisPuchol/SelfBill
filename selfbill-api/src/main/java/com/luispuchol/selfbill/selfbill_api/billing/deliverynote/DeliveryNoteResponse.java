package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;

import lombok.Builder;

@Builder
public record DeliveryNoteResponse(
        Integer id,
        Integer code,

        Integer invoiceId,

        LocalDateTime date,
        BigDecimal total,

        DeliveryNoteFormat format,
        BigDecimal vatPercentage,
        BigDecimal surchargePercentage,

        Integer clientId,
        String clientName,

        List<DeliveryNoteLineResponse> lines,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
