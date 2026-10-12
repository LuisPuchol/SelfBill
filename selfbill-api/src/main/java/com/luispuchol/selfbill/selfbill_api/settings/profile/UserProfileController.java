package com.luispuchol.selfbill.selfbill_api.settings.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-profile")
@Tag(name = "User Profile", description = "User profile management endpoints")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get user profile")
    @GetMapping
    public UserProfileResponse getUserProfile() {
        return userProfileService.getUserProfile();
    }

    @Operation(summary = "Create user profile")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createUserProfile(@RequestBody @Valid UserProfileRequest request) {
        return userProfileService.createUserProfile(request);
    }

    @Operation(summary = "Update user profile")
    @PutMapping
    public UserProfileResponse updateUserProfile(@RequestBody @Valid UserProfileRequest request) {
        return userProfileService.updateUserProfile(request);
    }

    @Operation(summary = "Upload profile logo (JPEG or PNG)")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadLogo(@RequestParam("file") MultipartFile file) {
        userProfileService.uploadLogo(file);
    }

    @Operation(summary = "Get profile logo")
    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        UserProfileLogo logo = userProfileService.getLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .body(logo.data());
    }
}
