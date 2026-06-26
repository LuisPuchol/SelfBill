package com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Fiscal name cannot be blank")
    @Size(max = 255, message = "Fiscal name must not exceed 255 characters")
    private String fiscalName;

    @Size(max = 20, message = "NIF must not exceed 20 characters")
    private String nif;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 50, message = "Email must not exceed 50 characters")
    @Pattern(regexp = "^$|^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", message = "Email format is invalid")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
}
