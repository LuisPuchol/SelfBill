package com.luispuchol.selfbill.selfbill_api.enums;

public enum SurchargeType {
    WITH_SURCHARGE("Con recargo de equivalencia"),
    WITHOUT_SURCHARGE("Sin recargo de equivalencia");

    private final String displayName;

    SurchargeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}