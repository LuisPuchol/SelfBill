package com.luispuchol.selfbill.selfbill_api.mail;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("selfbill.smtp")
public record SmtpProperties(Duration timeout) {
}
