package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("User profile")
class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CredentialProperties credentialProperties;
    @Autowired
    private CredentialEncryptionService credentialEncryptionService;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM user_profile");
    }

    @Test
    @DisplayName("Creating without a mail password stores none")
    void createsWithoutPassword() {
        UserProfileResponse created = userProfileService.createUserProfile(request(null));

        assertThat(created.hasMailPassword()).isFalse();
        assertThat(userProfileService.getUserProfile().hasMailPassword()).isFalse();
    }

    @Test
    @DisplayName("Creating with a blank mail password stores none")
    void createsWithBlankPassword() {
        UserProfileResponse created = userProfileService.createUserProfile(request("  "));

        assertThat(created.hasMailPassword()).isFalse();
    }

    @Test
    @DisplayName("hasMailPassword follows the stored password: null keeps it, blank clears it")
    void hasMailPasswordFollowsTheStoredOne() {
        assertThat(userProfileService.createUserProfile(request("secret")).hasMailPassword()).isTrue();

        assertThat(userProfileService.updateUserProfile(request(null)).hasMailPassword()).isTrue();
        assertThat(userProfileService.getUserProfile().hasMailPassword()).isTrue();

        assertThat(userProfileService.updateUserProfile(request("")).hasMailPassword()).isFalse();
        assertThat(userProfileService.getUserProfile().hasMailPassword()).isFalse();
    }

    @Test
    @DisplayName("A profile whose stored password cannot be decrypted still loads")
    void readsAProfileWhosePasswordCannotBeDecrypted() {
        userProfileService.createUserProfile(request("secret"));
        storeForeignPassword();

        assertThat(userProfileService.getUserProfile().hasMailPassword()).isTrue();
    }

    @Test
    @DisplayName("An undecryptable password counts as mail not configured")
    void anUndecryptablePasswordIsNotConfigured() {
        userProfileService.createUserProfile(request("secret", "smtp.example.com", 587));
        assertThat(userProfileService.getSmtpAccount().password()).isEqualTo("secret");
        storeForeignPassword();

        assertThatThrownBy(() -> userProfileService.getSmtpAccount())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MAIL_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("Tests keep the encryption key under target/, not in the user's home")
    void testsUseTheConfiguredKeyFile() {
        assertThat(credentialProperties.keyFile()).isEqualTo(Path.of("target/test-secret.key"));
    }

    /** A well-formed value (12-byte IV plus ciphertext) that this key did not produce. */
    private void storeForeignPassword() {
        byte[] foreign = new byte[12 + 32];
        new SecureRandom().nextBytes(foreign);
        jdbc.update("UPDATE user_profile SET mail_password_encrypted = ?", Base64.getEncoder().encodeToString(foreign));
        assertThatThrownBy(() -> credentialEncryptionService.decrypt(
                jdbc.queryForObject("SELECT mail_password_encrypted FROM user_profile", String.class)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static UserProfileRequest request(String mailPassword) {
        return request(mailPassword, null, null);
    }

    private static UserProfileRequest request(String mailPassword, String smtpHost, Integer smtpPort) {
        return UserProfileRequest.builder()
                .fullName("Luis")
                .fiscalName("Luis SL")
                .nif("B00000001")
                .address("Calle Mayor 1")
                .email("luis@example.com")
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .mailPassword(mailPassword)
                .build();
    }
}
