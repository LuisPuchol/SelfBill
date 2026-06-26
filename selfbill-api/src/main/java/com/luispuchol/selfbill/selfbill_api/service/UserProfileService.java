package com.luispuchol.selfbill.selfbill_api.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileLogoResponse;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileRequest;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileResponse;
import com.luispuchol.selfbill.selfbill_api.entity.UserProfile;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.UserProfileMapper;
import com.luispuchol.selfbill.selfbill_api.repository.UserProfileRepository;

@Service
@RequiredArgsConstructor
public class UserProfileService implements IUserProfileService {

    private static final List<String> ALLOWED_LOGO_TYPES = List.of("image/jpeg", "image/png");

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponse getUserProfile() {
        return userProfileMapper.toResponse(findProfileOrThrow());
    }

    @Transactional
    @Override
    public UserProfileResponse saveUserProfile(UserProfileRequest request) {
        Optional<UserProfile> existing = userProfileRepository.findFirstBy();
        UserProfile profile;
        if (existing.isPresent()) {
            profile = existing.get();
            userProfileMapper.updateEntity(profile, request);
        } else {
            profile = userProfileMapper.toEntity(request);
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    @Override
    public void uploadLogo(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_LOGO_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.USER_PROFILE_INVALID_LOGO_FORMAT);
        }
        UserProfile profile = findProfileOrThrow();
        try {
            profile.setLogoData(file.getBytes());
            profile.setLogoContentType(contentType);
            userProfileRepository.save(profile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public UserProfileLogoResponse getLogo() {
        UserProfile profile = findProfileOrThrow();
        if (profile.getLogoData() == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_LOGO_NOT_FOUND);
        }
        return UserProfileLogoResponse.builder()
                .data(profile.getLogoData())
                .contentType(profile.getLogoContentType())
                .build();
    }

    private UserProfile findProfileOrThrow() {
        return userProfileRepository.findFirstBy()
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }
}
