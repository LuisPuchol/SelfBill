package com.luispuchol.selfbill.selfbill_api.billing.document;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.mail.EmailMessage;
import com.luispuchol.selfbill.selfbill_api.mail.EmailService;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentSender {

    private final EmailService emailService;
    private final DocumentMessages documentMessages;

    public void send(DocumentType type, Client recipient, String number, byte[] pdf) {
        String to = recipient.getEmail();
        if (to == null || to.isBlank()) {
            throw new BusinessException(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED, recipient.getId());
        }
        emailService.send(new EmailMessage(to,
                documentMessages.emailSubject(type, number),
                documentMessages.emailBody(type, recipient.getName(), number),
                documentMessages.filename(type, number),
                pdf));
    }
}
