package com.luispuchol.selfbill.selfbill_api.enums;

public enum VatType {
    WITH_VAT("Con IVA"),
    WITHOUT_VAT("Sin IVA");

    private final String displayName;

    VatType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}