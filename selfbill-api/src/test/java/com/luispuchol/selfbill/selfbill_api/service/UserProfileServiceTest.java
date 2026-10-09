package com.luispuchol.selfbill.selfbill_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.luispuchol.selfbill.selfbill_api.dto.userprofile.UserProfileRequest;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("User profile")
class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM user_profile");
        userProfileService.createUserProfile(request("secret"));
    }

    @Test
    @DisplayName("Updating without a mail password keeps the stored one")
    void nullPasswordKeepsStoredOne() {
        userProfileService.updateUserProfile(request(null));

        assertThat(userProfileService.getUserProfile().getMailPassword()).isEqualTo("secret");
    }

    @Test
    @DisplayName("Updating with a blank mail password clears it")
    void blankPasswordClearsIt() {
        userProfileService.updateUserProfile(request(""));

        assertThat(userProfileService.getUserProfile().getMailPassword()).isNull();
    }

    private static UserProfileRequest request(String mailPassword) {
        return UserProfileRequest.builder()
                .fullName("Luis")
                .fiscalName("Luis SL")
                .nif("B00000001")
                .address("Calle Mayor 1")
                .email("luis@gmail.com")
                .mailPassword(mailPassword)
                .build();
    }
}
