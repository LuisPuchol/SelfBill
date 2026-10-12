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

        String traceabilityCode1,
        String traceabilityCode2,
        String traceabilityCode3,

        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
