package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record DeliveryNoteLineResponse(
        Integer id,
        Integer deliveryNoteId,

        Integer articleId,
        String articleName,

        Integer traceabilityCode1,
        Integer traceabilityCode2,
        Integer traceabilityCode3,

        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
