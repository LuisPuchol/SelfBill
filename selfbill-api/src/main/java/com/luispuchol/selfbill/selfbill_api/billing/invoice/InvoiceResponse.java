package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record InvoiceResponse(
        Integer id,
        InvoiceSeries series,
        Integer code,
        String number,
        LocalDateTime date,
        InvoiceStatus status,

        Integer clientId,
        String clientName,

        List<InvoiceSectionResponse> sections,

        BigDecimal subtotal,
        BigDecimal vatPercentage,
        BigDecimal vatAmount,
        BigDecimal surchargePercentage,
        BigDecimal surchargeAmount,
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
