package com.luispuchol.selfbill.selfbill_api.service;

import java.util.Locale;
import java.util.Properties;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.UserProfile;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.repository.InvoiceRepository;
import com.luispuchol.selfbill.selfbill_api.repository.UserProfileRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final InvoiceRepository invoiceRepository;
    private final UserProfileRepository userProfileRepository;
    private final IPdfService pdfService;
    private final MessageSource messageSource;
    private final CredentialEncryptionService credentialEncryptionService;

    @Override
    public void sendInvoiceEmail(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, invoiceId));

        String clientEmail = invoice.getClient().getEmail();
        if (clientEmail == null || clientEmail.isBlank()) {
            throw new BusinessException(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED, invoice.getClient().getId());
        }

        UserProfile senderProfile = userProfileRepository.findFirstBy()
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED));
        if (!isMailConfigured(senderProfile)) {
            throw new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED);
        }

        Locale locale = LocaleContextHolder.getLocale();
        Object[] codeArg = { invoice.getCode() };

        String subject = messageSource.getMessage("invoice.email.subject", codeArg, locale);
        String body = messageSource.getMessage("invoice.email.body",
                new Object[] { invoice.getClient().getName(), invoice.getCode() }, locale);
        String pdfFilename = messageSource.getMessage("invoice.pdf.filename", codeArg, locale);

        byte[] pdfBytes = pdfService.generateInvoicePdf(invoiceId);

        sendEmail(senderProfile, clientEmail, subject, body, pdfBytes, pdfFilename);
    }

    private boolean isMailConfigured(UserProfile profile) {
        return profile.getEmail() != null && !profile.getEmail().isBlank()
                && profile.getMailPasswordEncrypted() != null
                && profile.getSmtpHost() != null && !profile.getSmtpHost().isBlank()
                && profile.getSmtpPort() != null;
    }

    private void sendEmail(UserProfile senderProfile, String to, String subject, String body, byte[] attachment,
            String attachmentFilename) {
        try {
            JavaMailSenderImpl mailSender = buildMailSender(senderProfile);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderProfile.getEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachment));
            mailSender.send(message);
        } catch (MailException | MessagingException _) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, to);
        }
    }

    private JavaMailSenderImpl buildMailSender(UserProfile senderProfile) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(senderProfile.getSmtpHost());
        mailSender.setPort(senderProfile.getSmtpPort());
        mailSender.setUsername(senderProfile.getEmail());
        mailSender.setPassword(credentialEncryptionService.decrypt(senderProfile.getMailPasswordEncrypted()));

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }
}
