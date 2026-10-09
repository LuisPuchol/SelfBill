package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luispuchol.selfbill.selfbill_api.dto.userprofile.UserProfileRequest;
import com.luispuchol.selfbill.selfbill_api.dto.userprofile.UserProfileResponse;
import com.luispuchol.selfbill.selfbill_api.entity.UserProfile;

@Mapper
public interface UserProfileMapper {

    @Mapping(target = "hasLogo", expression = "java(userProfile.getLogoData() != null)")
    @Mapping(target = "mailPassword", ignore = true)
    UserProfileResponse toResponse(UserProfile userProfile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "logoData", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    @Mapping(target = "mailPasswordEncrypted", ignore = true)
    UserProfile toEntity(UserProfileRequest request);

    @InheritConfiguration
    void updateEntity(@MappingTarget UserProfile userProfile, UserProfileRequest request);
}
