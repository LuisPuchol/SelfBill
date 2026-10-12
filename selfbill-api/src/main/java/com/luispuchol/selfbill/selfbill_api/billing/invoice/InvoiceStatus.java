package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DRAFT has no number and its delivery notes can change. ISSUED is numbered, dated and frozen")
public enum InvoiceStatus {
    DRAFT,
    ISSUED
}
