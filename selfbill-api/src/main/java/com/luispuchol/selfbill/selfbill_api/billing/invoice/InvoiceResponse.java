package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record InvoiceResponse(
        Integer id,
        InvoiceSeries series,

        @Schema(description = "Number within the series, or null while `DRAFT`")
        Integer code,

        @Schema(description = "Printed number: series prefix and code, or null while `DRAFT`", example = "S-12")
        String number,

        @Schema(description = "Issue date, or null while `DRAFT`")
        LocalDateTime date,

        InvoiceStatus status,

        Integer clientId,

        @Schema(description = "Trade name of the client")
        String clientName,

        @Schema(description = "One per delivery note. Not included in lists")
        List<InvoiceSectionResponse> sections,

        @Schema(description = "Sum of the delivery note totals, without taxes")
        BigDecimal subtotal,
        BigDecimal vatPercentage,
        BigDecimal vatAmount,
        BigDecimal surchargePercentage,
        BigDecimal surchargeAmount,

        @Schema(description = "Subtotal plus VAT and surcharge")
        BigDecimal total,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
