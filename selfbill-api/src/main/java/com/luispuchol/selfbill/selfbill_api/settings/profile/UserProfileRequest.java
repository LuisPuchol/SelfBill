package com.luispuchol.selfbill.selfbill_api.settings.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserProfileRequest(
        @NotBlank(message = "Full name cannot be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Fiscal name cannot be blank")
        @Size(max = 255, message = "Fiscal name must not exceed 255 characters")
        String fiscalName,

        @NotBlank(message = "NIF cannot be blank")
        @Size(max = 20, message = "NIF must not exceed 20 characters")
        String nif,

        @NotBlank(message = "Address cannot be blank")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @NotBlank(message = "Email cannot be blank")
        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
        String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Size(max = 255, message = "SMTP host must not exceed 255 characters")
        String smtpHost,

        @Min(value = 1, message = "SMTP port must be between 1 and 65535")
        @Max(value = 65535, message = "SMTP port must be between 1 and 65535")
        Integer smtpPort,

        @Schema(description = "On update, null keeps the stored password and blank clears it")
        @Size(max = 255, message = "Mail password must not exceed 255 characters")
        String mailPassword) {
}
