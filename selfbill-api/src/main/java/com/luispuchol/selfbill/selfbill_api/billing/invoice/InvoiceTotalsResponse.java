package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The sum of all the invoices a filter matches, however many a list would page")
public record InvoiceTotalsResponse(
        Integer count,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal surchargeAmount,
        BigDecimal total) {
}
