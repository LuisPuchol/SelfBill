package com.luispuchol.selfbill.selfbill_api.settings.profile;

import org.mapstruct.Context;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserProfileMapper {

    @Mapping(target = "hasLogo", expression = "java(userProfile != null && userProfile.getLogoData() != null)")
    @Mapping(target = "hasMailPassword", expression = "java(userProfile != null && userProfile.getMailPasswordEncrypted() != null)")
    @Mapping(target = "mailPasswordUnreadable", expression = "java(userProfile != null && userProfile.getMailPasswordEncrypted() != null && !encryption.canDecrypt(userProfile.getMailPasswordEncrypted()))")
    UserProfileResponse toResponse(UserProfile userProfile, @Context CredentialEncryptionService encryption);

    @Mapping(target = "logoData", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "mailPasswordEncrypted", ignore = true)
    UserProfile toEntity(UserProfileRequest request);

    @InheritConfiguration
    void updateEntity(@MappingTarget UserProfile userProfile, UserProfileRequest request);
}
