package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record DeliveryNoteResponse(
        Integer id,
        Integer code,

        @Schema(description = "Invoice that contains the delivery note, or null while it is pending")
        Integer invoiceId,

        @Schema(description = "Status of the invoice that contains the delivery note, or null while it is pending")
        InvoiceStatus invoiceStatus,

        @Schema(description = "Number of the invoice, as printed (\"1\", \"S-1\"). Null while pending or in a draft",
                example = "S-1")
        String invoiceNumber,

        LocalDateTime date,

        @Schema(description = "Sum of the line totals, without taxes")
        BigDecimal total,

        DeliveryNoteFormat format,

        @Schema(description = "VAT rate applied, already resolved with the client's regime: 0 when it has no VAT")
        BigDecimal vatPercentage,

        @Schema(description = "Surcharge rate applied, already resolved with the client's regime: 0 when it has none")
        BigDecimal surchargePercentage,

        Integer clientId,

        @Schema(description = "Trade name of the client")
        String clientName,

        List<DeliveryNoteLineResponse> lines,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
