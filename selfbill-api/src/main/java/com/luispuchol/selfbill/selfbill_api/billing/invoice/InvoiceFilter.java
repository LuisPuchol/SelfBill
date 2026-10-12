package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record InvoiceFilter(
        InvoiceSeries series,

        @Schema(description = "Lowest number within the series, inclusive")
        Integer codeFrom,

        @Schema(description = "Highest number within the series, inclusive")
        Integer codeTo,

        Integer clientId,

        @Schema(description = "Only the invoice that contains this delivery note")
        Integer deliveryNoteId,

        InvoiceStatus status,

        @Schema(description = "Earliest issue date, inclusive. Drafts have no date", example = "2026-10-01")
        LocalDate dateFrom,

        @Schema(description = "Latest issue date, inclusive. Drafts have no date", example = "2026-10-31")
        LocalDate dateTo) {
}
