package com.luispuchol.selfbill.selfbill_api.service;

import java.util.Properties;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements IEmailService {

    @Override
    public void sendEmail(MailSender sender, String to, String subject, String body, byte[] attachment,
            String attachmentFilename) {
        try {
            JavaMailSenderImpl mailSender = buildMailSender(sender);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender.fromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachment));
            mailSender.send(message);
        } catch (MailException | MessagingException _) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, to);
        }
    }

    private JavaMailSenderImpl buildMailSender(MailSender sender) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(sender.smtpHost());
        mailSender.setPort(sender.smtpPort());
        mailSender.setUsername(sender.fromEmail());
        mailSender.setPassword(sender.smtpPassword());

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.put("mail.smtp.auth", "true");
        mailProperties.put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }
}
