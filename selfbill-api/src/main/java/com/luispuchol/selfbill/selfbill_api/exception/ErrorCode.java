package com.luispuchol.selfbill.selfbill_api.exception;

public enum ErrorCode {

    // Client
    CLIENT_NOT_FOUND("Client not found: {0}"),
    CLIENT_DUPLICATE_CODE("Already exists a client with code: {0}"),

    // Article
    ARTICLE_NOT_FOUND("Article not found: {0}"),
    ARTICLE_DUPLICATE_CODE("Already exists an article with code: {0}"),

    // DeliveryNote
    DELIVERY_NOTE_NOT_FOUND("Delivery note not found: {0}"),
    DELIVERY_NOTE_DUPLICATE_CODE("Already exists a delivery note with code: {0}"),

    // Generic
    VALIDATION_ERROR("Validation error"),
    MALFORMED_REQUEST("Malformed request body"),
    RESOURCE_NOT_FOUND("Resource not found"),
    INTERNAL_ERROR("Internal server error");

    private final String messageTemplate;

    ErrorCode(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String format(Object... params) {
        String result = messageTemplate;
        for (int i = 0; i < params.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(params[i]));
        }
        return result;
    }
}
