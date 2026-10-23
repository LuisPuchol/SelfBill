package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Lines of one delivery note of the invoice")
@Builder
public record InvoiceSectionResponse(
        Integer deliveryNoteId,
        Integer deliveryNoteCode,
        LocalDateTime deliveryNoteDate,

        @Schema(description = "Sum of the line totals of the section")
        BigDecimal subtotal,

        List<InvoiceLineResponse> lines) {
}
