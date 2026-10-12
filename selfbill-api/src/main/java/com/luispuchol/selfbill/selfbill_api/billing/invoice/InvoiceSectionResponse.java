package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record InvoiceSectionResponse(
        Integer deliveryNoteId,
        Integer deliveryNoteCode,
        LocalDateTime deliveryNoteDate,

        List<InvoiceLineResponse> lines) {
}
