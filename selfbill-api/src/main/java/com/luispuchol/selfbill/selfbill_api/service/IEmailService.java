package com.luispuchol.selfbill.selfbill_api.service;

public interface IEmailService {

    void sendEmail(MailSender sender, String to, String subject, String body, byte[] attachment,
            String attachmentFilename);
}
