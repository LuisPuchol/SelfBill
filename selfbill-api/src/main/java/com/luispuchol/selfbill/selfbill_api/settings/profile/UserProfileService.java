package com.luispuchol.selfbill.selfbill_api.settings.profile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Optional;

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

    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final CredentialEncryptionService credentialEncryptionService;

    public UserProfileResponse getUserProfile() {
        return toResponse(findProfile());
    }

    @Transactional
    public UserProfileResponse createUserProfile(UserProfileRequest request) {
        if (userProfileRepository.count() > 0) {
            throw new BusinessException(ErrorCode.USER_PROFILE_ALREADY_EXISTS);
        }

        UserProfile profile = userProfileMapper.toEntity(request);
        applySmtpDefaults(profile, request, null);
        applyMailPassword(profile, request.mailPassword());
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UserProfileRequest request) {
        UserProfile profile = findProfile();
        String previousEmail = profile.getEmail();
        userProfileMapper.updateEntity(profile, request);
        applySmtpDefaults(profile, request, previousEmail);
        applyMailPassword(profile, request.mailPassword());
        return toResponse(userProfileRepository.save(profile));
    }

    @Transactional
    public void uploadLogo(MultipartFile file) {
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // The declared content type comes from the file's extension: only the bytes say what it is
        String contentType = sniffImageType(data);
        if (contentType == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_INVALID_LOGO_FORMAT);
        }
        UserProfile profile = findProfile();
        profile.setLogoData(data);
        profile.setLogoContentType(contentType);
        userProfileRepository.save(profile);
    }

    private static String sniffImageType(byte[] data) {
        if (startsWith(data, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (startsWith(data, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        return data.length >= signature.length && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
    }

    public UserProfileLogo getLogo() {
        UserProfile profile = findProfile();
        if (profile.getLogoData() == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_LOGO_NOT_FOUND);
        }
        return new UserProfileLogo(profile.getLogoData(), profile.getLogoContentType());
    }

    public Optional<UserProfileLogo> findLogo() {
        return userProfileRepository.findFirstBy()
                .filter(profile -> profile.getLogoData() != null)
                .map(profile -> new UserProfileLogo(profile.getLogoData(), profile.getLogoContentType()));
    }

    public boolean hasProfile() {
        return userProfileRepository.count() > 0;
    }

    public Optional<IssuerData> findIssuer() {
        return userProfileRepository.findFirstBy()
                .map(profile -> new IssuerData(profile.getFiscalName(), profile.getNif(), profile.getAddress()));
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

    private UserProfileResponse toResponse(UserProfile profile) {
        return userProfileMapper.toResponse(profile, credentialEncryptionService);
    }

    /**
     * Fills the server and port the request left empty from the email's provider. When the email moved to
     * another provider, the previous provider's defaults that the form sent back count as left empty too:
     * they are stale, not a choice. Anything else the user typed is kept.
     */
    private static void applySmtpDefaults(UserProfile profile, UserProfileRequest request, String previousEmail) {
        if (profile.getEmail() == null) {
            return;
        }
        boolean providerChanged = previousEmail != null && !SmtpProviderDefaults.sameProvider(previousEmail, profile.getEmail());
        boolean hostGiven = request.smtpHost() != null
                && !(providerChanged && request.smtpHost().equals(SmtpProviderDefaults.defaultHost(previousEmail)));
        boolean portGiven = request.smtpPort() != null
                && !(providerChanged && request.smtpPort().equals(SmtpProviderDefaults.defaultPort(previousEmail)));
        if (!hostGiven) {
            String defaultHost = SmtpProviderDefaults.defaultHost(profile.getEmail());
            if (defaultHost != null || providerChanged) {
                profile.setSmtpHost(defaultHost);
            }
        }
        if (!portGiven) {
            Integer defaultPort = SmtpProviderDefaults.defaultPort(profile.getEmail());
            if (defaultPort != null || providerChanged) {
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
