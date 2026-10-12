package com.luispuchol.selfbill.selfbill_api.billing.document;

public enum DocumentType {
    INVOICE("invoice"),
    DELIVERY_NOTE("deliveryNote");

    private final String messagePrefix;

    DocumentType(String messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    public String messageKey(String key) {
        return messagePrefix + "." + key;
    }
}
