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
        @Schema(example = "María López")
        @NotBlank(message = "Full name cannot be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Schema(description = "Legal name, printed on documents", example = "Frutas López S.L.")
        @NotBlank(message = "Fiscal name cannot be blank")
        @Size(max = 255, message = "Fiscal name must not exceed 255 characters")
        String fiscalName,

        @Schema(example = "B87654321")
        @NotBlank(message = "NIF cannot be blank")
        @Size(max = 20, message = "NIF must not exceed 20 characters")
        String nif,

        @Schema(example = "Mercavalencia, nave 3")
        @NotBlank(message = "Address cannot be blank")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Schema(description = "Mail account documents are sent from", example = "facturas@frutaslopez.es")
        @NotBlank(message = "Email cannot be blank")
        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
        String email,

        @Schema(example = "961234567")
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Schema(description = "When null, the host of a known provider (Gmail, Outlook, Yahoo, iCloud) is taken from the email")
        @Size(max = 255, message = "SMTP host must not exceed 255 characters")
        String smtpHost,

        @Schema(description = "When null, the port of a known provider is taken from the email", example = "587")
        @Min(value = 1, message = "SMTP port must be between 1 and 65535")
        @Max(value = 65535, message = "SMTP port must be between 1 and 65535")
        Integer smtpPort,

        @Schema(description = """
                Password of the mail account, usually an app password. Required on create. \
                On update, null keeps the stored password and blank clears it""")
        @Size(max = 255, message = "Mail password must not exceed 255 characters")
        String mailPassword) {
}
