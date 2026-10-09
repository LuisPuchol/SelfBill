package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.enums.DocumentType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentMessages {

    private final MessageSource messageSource;

    public String filename(DocumentType type, Integer code) {
        return message(type, "pdf.filename", String.valueOf(code));
    }

    public String emailSubject(DocumentType type, Integer code) {
        return message(type, "email.subject", String.valueOf(code));
    }

    public String emailBody(DocumentType type, String recipientName, Integer code) {
        return message(type, "email.body", recipientName, String.valueOf(code));
    }

    private String message(DocumentType type, String key, Object... args) {
        return messageSource.getMessage(type.messageKey(key), args, LocaleContextHolder.getLocale());
    }
}
