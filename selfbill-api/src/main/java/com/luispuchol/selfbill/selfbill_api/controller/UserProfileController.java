package com.luispuchol.selfbill.selfbill_api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileLogoResponse;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileRequest;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileResponse;
import com.luispuchol.selfbill.selfbill_api.service.IUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-profile")
@Tag(name = "User Profile", description = "User profile management endpoints")
@Validated
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserProfileService userProfileService;

    @Operation(summary = "Get user profile")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserProfile() {
        return ResponseEntity.ok(userProfileService.getUserProfile());
    }

    @Operation(summary = "Create or update user profile")
    @PutMapping
    public ResponseEntity<UserProfileResponse> saveUserProfile(
            @RequestBody @Valid UserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.saveUserProfile(request));
    }

    @Operation(summary = "Upload profile logo (JPEG or PNG)")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadLogo(
            @RequestParam("file") MultipartFile file) {
        userProfileService.uploadLogo(file);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get profile logo")
    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        UserProfileLogoResponse logo = userProfileService.getLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.getContentType()))
                .body(logo.getData());
    }
}
