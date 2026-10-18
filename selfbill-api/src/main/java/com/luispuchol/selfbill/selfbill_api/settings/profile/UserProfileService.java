package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private static final List<String> ALLOWED_LOGO_TYPES = List.of("image/jpeg", "image/png");

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final CredentialEncryptionService credentialEncryptionService;

    public UserProfileResponse getUserProfile() {
        return userProfileMapper.toResponse(findProfile());
    }

    @Transactional
    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        if (userProfileRepository.count() > 0) {
            throw new BusinessException(ErrorCode.USER_PROFILE_ALREADY_EXISTS);
        }

        UserProfile profile = userProfileMapper.toEntity(request);
        applySmtpDefaults(profile, request);
        applyMailPassword(profile, request.mailPassword());
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UserProfileRequest request) {
        UserProfile profile = findProfile();
        userProfileMapper.updateEntity(profile, request);
        applySmtpDefaults(profile, request);
        applyMailPassword(profile, request.mailPassword());
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public void uploadLogo(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_LOGO_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.USER_PROFILE_INVALID_LOGO_FORMAT);
        }
        UserProfile profile = findProfile();
        try {
            profile.setLogoData(file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        profile.setLogoContentType(contentType);
        userProfileRepository.save(profile);
    }

    public UserProfileLogo getLogo() {
        UserProfile profile = findProfile();
        if (profile.getLogoData() == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_LOGO_NOT_FOUND);
        }
        return new UserProfileLogo(profile.getLogoData(), profile.getLogoContentType());
    }

    public boolean hasProfile() {
        return userProfileRepository.count() > 0;
    }

    public SmtpAccount getSmtpAccount() {
        UserProfile profile = userProfileRepository.findFirstBy()
                .filter(UserProfileService::isMailConfigured)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED));
        String password;
        try {
            password = credentialEncryptionService.decrypt(profile.getMailPasswordEncrypted());
        } catch (IllegalStateException e) {
            log.warn("The stored mail password cannot be decrypted with this key; treating mail as not configured");
            throw new BusinessException(ErrorCode.MAIL_NOT_CONFIGURED);
        }
        return new SmtpAccount(profile.getEmail(), profile.getSmtpHost(), profile.getSmtpPort(), password);
    }

    private UserProfile findProfile() {
        return userProfileRepository.findFirstBy()
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private static void applySmtpDefaults(UserProfile profile, UserProfileRequest request) {
        if (profile.getEmail() == null) {
            return;
        }
        if (request.smtpHost() == null) {
            String defaultHost = SmtpProviderDefaults.defaultHost(profile.getEmail());
            if (defaultHost != null) {
                profile.setSmtpHost(defaultHost);
            }
        }
        if (request.smtpPort() == null) {
            Integer defaultPort = SmtpProviderDefaults.defaultPort(profile.getEmail());
            if (defaultPort != null) {
                profile.setSmtpPort(defaultPort);
            }
        }
    }

    private void applyMailPassword(UserProfile profile, String password) {
        if (password == null) {
            return;
        }
        profile.setMailPasswordEncrypted(password.isBlank() ? null : credentialEncryptionService.encrypt(password));
    }

    private static boolean isMailConfigured(UserProfile profile) {
        return profile.getSmtpHost() != null && !profile.getSmtpHost().isBlank()
                && profile.getSmtpPort() != null
                && profile.getMailPasswordEncrypted() != null;
    }
}
