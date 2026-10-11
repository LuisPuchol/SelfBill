package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        Integer id,
        String fullName,
        String fiscalName,
        String nif,
        String address,
        String email,
        String phone,
        String smtpHost,
        Integer smtpPort,
        String mailPassword,
        boolean hasLogo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
