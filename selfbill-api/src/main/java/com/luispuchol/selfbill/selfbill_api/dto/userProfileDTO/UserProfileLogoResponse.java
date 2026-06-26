package com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileLogoResponse {

    private byte[] data;
    private String contentType;
}
