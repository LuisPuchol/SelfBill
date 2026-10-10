package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record InvoiceLineResponse(
        Integer id,

        Integer articleId,
        String articleName,
        Integer articleCode,

        Integer trazabilityCode1,
        Integer trazabilityCode2,
        Integer trazabilityCode3,

        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
