package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Numbering series, each with its own counter. WITHOUT_VAT is for clients without VAT and is printed with the S- prefix")
public enum InvoiceSeries {
    GENERAL(""),
    WITHOUT_VAT("S-");

    private final String prefix;

    InvoiceSeries(String prefix) {
        this.prefix = prefix;
    }

    public static InvoiceSeries forVatType(VatType vatType) {
        return vatType == VatType.WITHOUT_VAT ? WITHOUT_VAT : GENERAL;
    }

    public String format(Integer code) {
        return prefix + code;
    }
}
