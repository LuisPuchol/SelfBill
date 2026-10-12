package com.luispuchol.selfbill.selfbill_api.mail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    private static final int SMTPS_PORT = 465;

    private final UserProfileService userProfileService;
    private final SmtpProperties smtpProperties;

    public void send(EmailMessage message) {
        SmtpAccount account = userProfileService.getSmtpAccount();
        try {
            JavaMailSenderImpl mailSender = buildMailSender(account, smtpProperties.timeout());
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(account.email());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body());
            helper.addAttachment(message.attachmentName(), new ByteArrayResource(message.attachment()));
            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, e, message.to());
        }
    }

    static JavaMailSenderImpl buildMailSender(SmtpAccount account, Duration timeout) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(account.host());
        mailSender.setPort(account.port());
        mailSender.setUsername(account.email());
        mailSender.setPassword(account.password());

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        // 465 speaks SSL from the first byte; the other ports start in plain text and upgrade
        properties.put(account.port() == SMTPS_PORT ? "mail.smtp.ssl.enable" : "mail.smtp.starttls.enable", "true");
        // Jakarta Mail waits forever by default
        String timeoutMillis = String.valueOf(timeout.toMillis());
        properties.put("mail.smtp.connectiontimeout", timeoutMillis);
        properties.put("mail.smtp.timeout", timeoutMillis);
        properties.put("mail.smtp.writetimeout", timeoutMillis);
        return mailSender;
    }
}
