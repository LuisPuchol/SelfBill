package com.luispuchol.selfbill.selfbill_api.service;

import java.io.IOException;
import java.util.List;
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
    private final CredentialEncryptionService credentialEncryptionService;

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponse getUserProfile() {
        return toResponseWithPassword(findProfileOrThrow());
    }

    @Transactional
    @Override
    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        if (userProfileRepository.findFirstBy().isPresent()) {
            throw new BusinessException(ErrorCode.USER_PROFILE_ALREADY_EXISTS);
        }
        if (request.getMailPassword() == null || request.getMailPassword().isBlank()) {
            throw new BusinessException(ErrorCode.USER_PROFILE_MAIL_PASSWORD_REQUIRED);
        }

        UserProfile profile = userProfileMapper.toEntity(request);
        applySmtpDefaults(profile, request);
        profile.setMailPasswordEncrypted(credentialEncryptionService.encrypt(request.getMailPassword()));

        return toResponseWithPassword(userProfileRepository.save(profile));
    }

    @Transactional
    @Override
    public UserProfileResponse updateUserProfile(UserProfileRequest request) {
        UserProfile profile = findProfileOrThrow();

        userProfileMapper.updateEntity(profile, request);
        applySmtpDefaults(profile, request);
        applyMailPassword(profile, request);

        return toResponseWithPassword(userProfileRepository.save(profile));
    }

    private UserProfileResponse toResponseWithPassword(UserProfile profile) {
        UserProfileResponse response = userProfileMapper.toResponse(profile);
        if (profile.getMailPasswordEncrypted() != null) {
            response.setMailPassword(credentialEncryptionService.decrypt(profile.getMailPasswordEncrypted()));
        }
        return response;
    }

    private void applySmtpDefaults(UserProfile profile, UserProfileRequest request) {
        if (request.getSmtpHost() == null && profile.getEmail() != null) {
            String defaultHost = SmtpProviderDefaults.defaultHost(profile.getEmail());
            if (defaultHost != null) {
                profile.setSmtpHost(defaultHost);
            }
        }
        if (request.getSmtpPort() == null && profile.getEmail() != null) {
            Integer defaultPort = SmtpProviderDefaults.defaultPort(profile.getEmail());
            if (defaultPort != null) {
                profile.setSmtpPort(defaultPort);
            }
        }
    }

    private void applyMailPassword(UserProfile profile, UserProfileRequest request) {
        String password = request.getMailPassword();
        profile.setMailPasswordEncrypted(
                password == null || password.isBlank() ? null : credentialEncryptionService.encrypt(password));
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

    @Transactional(readOnly = true)
    @Override
    public UserProfileResponse getConfiguredSenderProfile() {
        UserProfile profile = userProfileRepository.findFirstBy()
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED));
        if (!isMailConfigured(profile)) {
            throw new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED);
        }
        return toResponseWithPassword(profile);
    }

    private boolean isMailConfigured(UserProfile profile) {
        return profile.getSmtpHost() != null && !profile.getSmtpHost().isBlank()
                && profile.getSmtpPort() != null
                && profile.getMailPasswordEncrypted() != null;
    }
}
