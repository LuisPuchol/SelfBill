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

    @Test
    void canDecryptTellsWhetherThisKeyReadsTheValue() {
        CredentialEncryptionService mine = new CredentialEncryptionService(new CredentialProperties(dir.resolve("a.key")));
        CredentialEncryptionService other = new CredentialEncryptionService(new CredentialProperties(dir.resolve("b.key")));
        String encrypted = mine.encrypt("secret");

        assertThat(mine.canDecrypt(encrypted)).isTrue();
        assertThat(other.canDecrypt(encrypted)).isFalse();
        assertThat(mine.canDecrypt("not base64 at all!")).isFalse();
        assertThat(mine.canDecrypt("AAAA")).isFalse();
    }
}
