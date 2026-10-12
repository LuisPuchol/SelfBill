package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "Stored encrypted, returned decrypted")
        String mailPassword,

        @Schema(description = "Whether a logo has been uploaded")
        boolean hasLogo,

        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
