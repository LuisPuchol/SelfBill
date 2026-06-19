package com.luispuchol.selfbill.selfbill_api.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Client
    CLIENT_NOT_FOUND("Client not found: {0}", HttpStatus.NOT_FOUND),
    CLIENT_DUPLICATE_CODE("Already exists a client with code: {0}", HttpStatus.CONFLICT),

    // Article
    ARTICLE_NOT_FOUND("Article not found: {0}", HttpStatus.NOT_FOUND),
    ARTICLE_DUPLICATE_CODE("Already exists an article with code: {0}", HttpStatus.CONFLICT),

    // DeliveryNote
    DELIVERY_NOTE_NOT_FOUND("Delivery note not found: {0}", HttpStatus.NOT_FOUND),
    DELIVERY_NOTE_DUPLICATE_CODE("Already exists a delivery note with code: {0}", HttpStatus.CONFLICT),

    // TaxConfig
    TAX_CONFIG_NOT_FOUND("Tax configuration not found", HttpStatus.NOT_FOUND),
    TAX_CONFIG_ALREADY_EXISTS("Tax configuration already exists", HttpStatus.CONFLICT),

    // Invoice
    INVOICE_NOT_FOUND("Invoice not found: {0}", HttpStatus.NOT_FOUND),
    INVOICE_DUPLICATE_CODE("Already exists an invoice with code: {0}", HttpStatus.CONFLICT),
    INVOICE_DUPLICATE_DELIVERY_NOTE("Delivery note {0} already has an associated invoice", HttpStatus.CONFLICT),
    INVOICE_MIXED_CLIENTS("All delivery notes must belong to the same client", HttpStatus.CONFLICT),

    // Generic
    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("Malformed request body", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("Resource not found", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String messageTemplate;
    private final HttpStatus httpStatus;

    ErrorCode(String messageTemplate, HttpStatus httpStatus) {
        this.messageTemplate = messageTemplate;
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String format(Object... params) {
        String result = messageTemplate;
        for (int i = 0; i < params.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(params[i]));
        }
        return result;
    }
}
