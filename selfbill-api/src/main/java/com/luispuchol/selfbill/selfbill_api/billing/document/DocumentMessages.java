package com.luispuchol.selfbill.selfbill_api.billing.document;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentMessages {

    private final MessageSource messageSource;

    public String filename(DocumentType type, String number) {
        return message(type, "pdf.filename", number);
    }

    public String emailSubject(DocumentType type, String number) {
        return message(type, "email.subject", number);
    }

    public String emailBody(DocumentType type, String recipientName, String number) {
        return message(type, "email.body", recipientName, number);
    }

    private String message(DocumentType type, String key, Object... args) {
        return messageSource.getMessage(type.messageKey(key), args, LocaleContextHolder.getLocale());
    }
}
