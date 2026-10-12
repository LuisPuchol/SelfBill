package com.luispuchol.selfbill.selfbill_api.mail;

public record EmailMessage(String to, String subject, String body, String attachmentName, byte[] attachment) {
}
