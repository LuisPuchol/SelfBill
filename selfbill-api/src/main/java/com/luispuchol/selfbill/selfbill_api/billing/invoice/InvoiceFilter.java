package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record InvoiceFilter(
        InvoiceSeries series,
        Integer codeFrom,
        Integer codeTo,
        Integer clientId,
        Integer deliveryNoteId,
        InvoiceStatus status,
        LocalDateTime dateFrom,
        LocalDateTime dateTo) {
}
