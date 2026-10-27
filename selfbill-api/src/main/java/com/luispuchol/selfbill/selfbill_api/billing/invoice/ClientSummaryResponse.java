package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What was invoiced to a client in a period: the sum of its issued invoices")
public record ClientSummaryResponse(
        Integer clientId,
        Integer clientCode,

        @Schema(description = "Trade name of the client")
        String clientName,

        Integer invoiceCount,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal surchargeAmount,
        BigDecimal total) {
}
