package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "One future invoice: the pending delivery notes of a client, in one month and with the same rates")
@Builder
public record PendingInvoiceResponse(
        Integer clientId,
        Integer clientCode,

        @Schema(description = "Trade name of the client")
        String clientName,

        @Schema(description = "Calendar month of the delivery notes", example = "2026-10")
        String month,

        BigDecimal vatPercentage,
        BigDecimal surchargePercentage,

        @Schema(description = "What the invoice would have, as issuing it computes")
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal surchargeAmount,
        BigDecimal total,

        @Schema(description = "The client was deleted, so the invoice cannot be issued")
        boolean clientDeleted,

        @Schema(description = "Fiscal data of the client that issuing requires and is empty: "
                + "`nif`, `address`, `postalCode`, `city`")
        List<String> missingFiscalData,

        @Schema(description = "Oldest first")
        List<PendingDeliveryNoteResponse> deliveryNotes) {
}
