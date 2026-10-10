package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CredentialEncryptionServiceTest {

    @TempDir
    Path dir;

    @Test
    void createsTheKeyAtTheConfiguredPathAndReusesIt() {
        Path keyFile = dir.resolve("nested").resolve("secret.key");
        String encrypted = new CredentialEncryptionService(new CredentialProperties(keyFile)).encrypt("secret");

        assertThat(keyFile).exists();
        assertThat(new CredentialEncryptionService(new CredentialProperties(keyFile)).decrypt(encrypted))
                .isEqualTo("secret");
    }
}
