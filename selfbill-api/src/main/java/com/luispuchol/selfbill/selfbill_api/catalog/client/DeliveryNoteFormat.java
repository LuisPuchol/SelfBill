package com.luispuchol.selfbill.selfbill_api.catalog.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Print format of a delivery note. PLAIN shows lines and totals without taxes. VALUED adds the tax calculation, and is still not an invoice")
public enum DeliveryNoteFormat {
    VALUED,
    PLAIN
}
