package com.luispuchol.selfbill.selfbill_api.mail;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentMessages;
import com.luispuchol.selfbill.selfbill_api.billing.document.DocumentType;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.settings.profile.SmtpAccount;
import com.luispuchol.selfbill.selfbill_api.settings.profile.UserProfileService;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final UserProfileService userProfileService;
    private final DocumentMessages documentMessages;

    public void sendDocument(DocumentType type, Client recipient, String number, byte[] pdf) {
        String to = recipient.getEmail();
        if (to == null || to.isBlank()) {
            throw new BusinessException(ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED, recipient.getId());
        }

        SmtpAccount account = userProfileService.getSmtpAccount();
        try {
            JavaMailSenderImpl mailSender = buildMailSender(account);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(account.email());
            helper.setTo(to);
            helper.setSubject(documentMessages.emailSubject(type, number));
            helper.setText(documentMessages.emailBody(type, recipient.getName(), number));
            helper.addAttachment(documentMessages.filename(type, number), new ByteArrayResource(pdf));
            mailSender.send(message);
        } catch (MailException | MessagingException _) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, to);
        }
    }

    private static JavaMailSenderImpl buildMailSender(SmtpAccount account) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(account.host());
        mailSender.setPort(account.port());
        mailSender.setUsername(account.email());
        mailSender.setPassword(account.password());
        mailSender.getJavaMailProperties().put("mail.smtp.auth", "true");
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }
}
