package com.luispuchol.selfbill.selfbill_api.shared;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    CLIENT_NOT_FOUND("Client not found: {0}", HttpStatus.NOT_FOUND),
    CLIENT_DUPLICATE_CODE("Already exists a client with code: {0}", HttpStatus.CONFLICT),
    CLIENT_EMAIL_NOT_CONFIGURED("Client {0} has no email address configured", HttpStatus.BAD_REQUEST),
    CLIENT_FISCAL_DATA_MISSING("Client {0} is missing {1}, required to issue an invoice", HttpStatus.BAD_REQUEST),

    ARTICLE_NOT_FOUND("Article not found: {0}", HttpStatus.NOT_FOUND),
    ARTICLE_DUPLICATE_CODE("Already exists an article with code: {0}", HttpStatus.CONFLICT),

    DELIVERY_NOTE_NOT_FOUND("Delivery note not found: {0}", HttpStatus.NOT_FOUND),
    DELIVERY_NOTE_DUPLICATE_CODE("Already exists a delivery note with code: {0}", HttpStatus.CONFLICT),
    DELIVERY_NOTE_ALREADY_INVOICED("Delivery note {0} is already invoiced and cannot be modified or deleted",
            HttpStatus.CONFLICT),

    INVOICE_NOT_FOUND("Invoice not found: {0}", HttpStatus.NOT_FOUND),
    INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED("Delivery note {0} already has an associated invoice",
            HttpStatus.CONFLICT),
    INVOICE_MIXED_CLIENTS("All delivery notes must belong to the same client", HttpStatus.CONFLICT),
    INVOICE_MIXED_TAX_RATES("All delivery notes of an invoice must have the same VAT and surcharge rates",
            HttpStatus.CONFLICT),
    INVOICE_MIXED_MONTHS("All delivery notes of an invoice must be from the same month", HttpStatus.CONFLICT),
    INVOICE_ALREADY_ISSUED("Invoice {0} is already issued and cannot be modified", HttpStatus.CONFLICT),
    INVOICE_NOT_ISSUED("Invoice {0} is a draft and must be issued first", HttpStatus.CONFLICT),

    TAX_CONFIG_NOT_FOUND("No tax configuration in force on {0}", HttpStatus.NOT_FOUND),
    TAX_CONFIG_ALREADY_EXISTS("A tax configuration from {0} already exists", HttpStatus.CONFLICT),

    USER_PROFILE_NOT_FOUND("User profile not found", HttpStatus.NOT_FOUND),
    USER_PROFILE_ALREADY_EXISTS("A user profile already exists", HttpStatus.CONFLICT),
    USER_PROFILE_LOGO_NOT_FOUND("User profile logo not found", HttpStatus.NOT_FOUND),
    USER_PROFILE_INVALID_LOGO_FORMAT("Logo must be a JPEG or PNG image", HttpStatus.BAD_REQUEST),

    MAIL_NOT_CONFIGURED(
            "Mail account is not fully configured. Please set your email, SMTP host/port and app password in your profile",
            HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED("Failed to send email to: {0}", HttpStatus.INTERNAL_SERVER_ERROR),

    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST),
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
