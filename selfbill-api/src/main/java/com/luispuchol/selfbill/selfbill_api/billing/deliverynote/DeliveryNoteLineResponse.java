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

        Integer trazabilityCode1,
        Integer trazabilityCode2,
        Integer trazabilityCode3,

        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
