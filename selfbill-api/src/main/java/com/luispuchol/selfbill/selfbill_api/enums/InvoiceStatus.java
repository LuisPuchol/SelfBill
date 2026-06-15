package com.luispuchol.selfbill.selfbill_api.enums;

public enum InvoiceStatus {
    ACTIVE("Activa"),
    RECTIFIED("Rectificada");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
