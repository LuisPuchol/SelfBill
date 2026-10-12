package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import com.luispuchol.selfbill.selfbill_api.catalog.client.VatType;

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
