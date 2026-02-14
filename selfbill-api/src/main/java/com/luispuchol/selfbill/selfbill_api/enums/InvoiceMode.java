package com.luispuchol.selfbill.selfbill_api.enums;

public enum InvoiceMode {
    PER_DELIVERY_NOTE("Facturar por albarán"),
    GROUPED("Facturación agrupada");

    private final String displayName;

    InvoiceMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}