package com.luispuchol.selfbill.selfbill_api.service;

public record MailSender(String fromEmail, String smtpHost, Integer smtpPort, String smtpPassword) {
}
