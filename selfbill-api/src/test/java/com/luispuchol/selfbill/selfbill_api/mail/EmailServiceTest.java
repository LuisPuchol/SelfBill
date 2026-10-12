package com.luispuchol.selfbill.selfbill_api.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.settings.profile.SmtpAccount;

@DisplayName("Email service")
class EmailServiceTest {

    @Test
    @DisplayName("Port 465 speaks SSL from the start; other ports upgrade with STARTTLS")
    void port465UsesImplicitSsl() {
        Properties smtps = mailProperties(465);
        Properties submission = mailProperties(587);

        assertThat(smtps).containsEntry("mail.smtp.ssl.enable", "true")
                .doesNotContainKey("mail.smtp.starttls.enable");
        assertThat(submission).containsEntry("mail.smtp.starttls.enable", "true")
                .doesNotContainKey("mail.smtp.ssl.enable");
    }

    private static Properties mailProperties(int port) {
        SmtpAccount account = new SmtpAccount("luis@example.com", "smtp.example.com", port, "secret");
        return EmailService.buildMailSender(account, Duration.ofSeconds(30)).getJavaMailProperties();
    }
}
