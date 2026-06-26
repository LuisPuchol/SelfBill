package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.web.multipart.MultipartFile;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileLogoResponse;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileRequest;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileResponse;

public interface IUserProfileService {
    UserProfileResponse getUserProfile();
    UserProfileResponse saveUserProfile(UserProfileRequest request);
    void uploadLogo(MultipartFile file);
    UserProfileLogoResponse getLogo();
}
