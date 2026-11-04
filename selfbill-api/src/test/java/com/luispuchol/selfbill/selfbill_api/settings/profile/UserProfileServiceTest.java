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
    @DisplayName("Switching provider replaces its defaults")
    void switchingProviderReplacesItsDefaults() {
        userProfileService.createUserProfile(requestFor("a@gmail.com", null, null));
        assertThat(userProfileService.getUserProfile().smtpHost()).isEqualTo("smtp.gmail.com");

        UserProfileResponse updated = userProfileService.updateUserProfile(
                requestFor("a@outlook.com", "smtp.gmail.com", 587));

        assertThat(updated.smtpHost()).isEqualTo("smtp.office365.com");
        assertThat(updated.smtpPort()).isEqualTo(587);
    }

    @Test
    @DisplayName("Switching to an unknown provider empties the old defaults")
    void switchingToUnknownProviderEmptiesTheOldDefaults() {
        userProfileService.createUserProfile(requestFor("a@gmail.com", null, null));

        UserProfileResponse updated = userProfileService.updateUserProfile(
                requestFor("a@empresa.es", "smtp.gmail.com", 587));

        assertThat(updated.smtpHost()).isNull();
        assertThat(updated.smtpPort()).isNull();
    }

    @Test
    @DisplayName("A custom server is kept when the email changes, and any server when it does not")
    void customServerIsKept() {
        userProfileService.createUserProfile(requestFor("a@gmail.com", "mail.empresa.es", 465));

        UserProfileResponse switched = userProfileService.updateUserProfile(
                requestFor("a@outlook.com", "mail.empresa.es", 465));
        assertThat(switched.smtpHost()).isEqualTo("mail.empresa.es");
        assertThat(switched.smtpPort()).isEqualTo(465);

        UserProfileResponse same = userProfileService.updateUserProfile(
                requestFor("a@outlook.com", "smtp.gmail.com", 25));
        assertThat(same.smtpHost()).isEqualTo("smtp.gmail.com");
        assertThat(same.smtpPort()).isEqualTo(25);
    }

    @Test
    @DisplayName("A custom server keeps its port when the email changes, even if the port is a default one")
    void customServerKeepsItsPortWhenTheEmailChanges() {
        userProfileService.createUserProfile(requestFor("a@gmail.com", null, null));

        UserProfileResponse updated = userProfileService.updateUserProfile(
                requestFor("a@empresa.es", "mail.empresa.es", 587));

        assertThat(updated.smtpHost()).isEqualTo("mail.empresa.es");
        assertThat(updated.smtpPort()).isEqualTo(587);
    }

    @Test
    @DisplayName("An unreadable stored password is reported")
    void unreadablePasswordIsReported() {
        assertThat(userProfileService.createUserProfile(request(null)).mailPasswordUnreadable()).isFalse();

        assertThat(userProfileService.updateUserProfile(request("secret")).mailPasswordUnreadable()).isFalse();
        assertThat(userProfileService.getUserProfile().mailPasswordUnreadable()).isFalse();

        storeForeignPassword();
        UserProfileResponse loaded = userProfileService.getUserProfile();
        assertThat(loaded.hasMailPassword()).isTrue();
        assertThat(loaded.mailPasswordUnreadable()).isTrue();
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
        return requestFor("luis@example.com", mailPassword, smtpHost, smtpPort);
    }

    private static UserProfileRequest requestFor(String email, String smtpHost, Integer smtpPort) {
        return requestFor(email, null, smtpHost, smtpPort);
    }

    private static UserProfileRequest requestFor(String email, String mailPassword, String smtpHost, Integer smtpPort) {
        return UserProfileRequest.builder()
                .fullName("Luis")
                .fiscalName("Luis SL")
                .nif("B00000001")
                .address("Calle Mayor 1")
                .email(email)
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .mailPassword(mailPassword)
                .build();
    }
}
