package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("selfbill.credentials")
public record CredentialProperties(Path keyFile) {
}
