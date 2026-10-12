package com.luispuchol.selfbill.selfbill_api.mail;

import java.nio.charset.StandardCharsets;

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

    private final UserProfileService userProfileService;

    public void send(EmailMessage message) {
        SmtpAccount account = userProfileService.getSmtpAccount();
        try {
            JavaMailSenderImpl mailSender = buildMailSender(account);
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
