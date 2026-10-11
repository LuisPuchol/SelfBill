package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

@DisplayName("Credential properties defaults")
class CredentialPropertiesDefaultTest {

    @Test
    @DisplayName("Production keeps the encryption key under the user's home")
    void keyFileDefaultsToTheUserHome() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(PropertiesConfiguration.class)
                .run(context -> assertThat(context.getBean(CredentialProperties.class).keyFile())
                        .isEqualTo(Path.of(System.getProperty("user.home"), ".selfbill", "secret.key")));
    }

    @Configuration
    @EnableConfigurationProperties(CredentialProperties.class)
    static class PropertiesConfiguration {
    }
}
