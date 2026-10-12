package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Copy of a delivery note line. Its article code and name are frozen once the invoice is issued")
@Builder
public record InvoiceLineResponse(
        Integer id,

        Integer articleId,
        String articleName,
        Integer articleCode,

        String traceabilityCode1,
        String traceabilityCode2,
        String traceabilityCode3,

        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
