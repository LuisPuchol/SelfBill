package com.luispuchol.selfbill.selfbill_api.settings.profile;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.USER_PROFILE_ALREADY_EXISTS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.USER_PROFILE_INVALID_LOGO_FORMAT;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.USER_PROFILE_LOGO_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.USER_PROFILE_NOT_FOUND;

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

import com.luispuchol.selfbill.selfbill_api.shared.ApiErrors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user-profile")
@Tag(name = "User Profile", description = "Fiscal data of the single user, printed on documents, and the mail account they are sent from")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get user profile")
    @ApiErrors(USER_PROFILE_NOT_FOUND)
    @GetMapping
    public UserProfileResponse getUserProfile() {
        return userProfileService.getUserProfile();
    }

    @Operation(summary = "Create user profile", description = "Only one profile can exist. The mail password is optional and can be set later")
    @ApiErrors(USER_PROFILE_ALREADY_EXISTS)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createUserProfile(@RequestBody @Valid UserProfileRequest request) {
        return userProfileService.createUserProfile(request);
    }

    @Operation(summary = "Update user profile")
    @ApiErrors(USER_PROFILE_NOT_FOUND)
    @PutMapping
    public UserProfileResponse updateUserProfile(@RequestBody @Valid UserProfileRequest request) {
        return userProfileService.updateUserProfile(request);
    }

    @Operation(summary = "Upload profile logo (JPEG or PNG)", description = "Replaces the current logo")
    @ApiErrors({ USER_PROFILE_INVALID_LOGO_FORMAT, USER_PROFILE_NOT_FOUND })
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadLogo(@RequestParam("file") MultipartFile file) {
        userProfileService.uploadLogo(file);
    }

    @Operation(summary = "Get profile logo")
    @ApiResponse(responseCode = "200", description = "The logo, with the content type it was uploaded with", content = {
            @Content(mediaType = MediaType.IMAGE_PNG_VALUE, schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = MediaType.IMAGE_JPEG_VALUE, schema = @Schema(type = "string", format = "binary")) })
    @ApiErrors({ USER_PROFILE_NOT_FOUND, USER_PROFILE_LOGO_NOT_FOUND })
    @GetMapping("/logo")
    public ResponseEntity<byte[]> getLogo() {
        UserProfileLogo logo = userProfileService.getLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .body(logo.data());
    }
}
