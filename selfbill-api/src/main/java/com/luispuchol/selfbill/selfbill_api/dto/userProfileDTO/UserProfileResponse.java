package com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Integer id;
    private String fullName;
    private String fiscalName;
    private String nif;
    private String address;
    private String email;
    private String phone;
    private String smtpHost;
    private Integer smtpPort;
    private boolean hasMailPassword;
    private boolean hasLogo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
