package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.mail.EmailMessage;
import com.luispuchol.selfbill.selfbill_api.mail.EmailService;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

@Component
public class DocumentSender {

    private final EmailService emailService;
    private final DocumentMessages documentMessages;
    private final TransactionTemplate readOnlyTransaction;

    public DocumentSender(EmailService emailService, DocumentMessages documentMessages,
            PlatformTransactionManager transactionManager) {
        this.emailService = emailService;
        this.documentMessages = documentMessages;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    // Loads the document in a transaction of its own and sends it once that transaction is over: every
    // transaction holds the database's write lock, and the mail server can take long to answer
    public void send(DocumentType type, Supplier<OutgoingDocument> loader) {
        EmailMessage message = readOnlyTransaction.execute(status -> compose(type, loader.get()));
        emailService.send(message);
    }

    private EmailMessage compose(DocumentType type, OutgoingDocument document) {
        Client recipient = document.recipient();
        String to = recipient.getEmail();
        if (to == null || to.isBlank()) {
            throw new BusinessException(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED, recipient.getId());
        }
        return new EmailMessage(to,
                documentMessages.emailSubject(type, document.number()),
                documentMessages.emailBody(type, recipient.getName(), document.number()),
                documentMessages.filename(type, document.number()),
                document.pdf());
    }
}
