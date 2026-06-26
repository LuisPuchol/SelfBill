package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileRequest;
import com.luispuchol.selfbill.selfbill_api.dto.userProfileDTO.UserProfileResponse;
import com.luispuchol.selfbill.selfbill_api.entity.UserProfile;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserProfileMapper {

    @Mapping(target = "hasLogo", expression = "java(userProfile.getLogoData() != null)")
    UserProfileResponse toResponse(UserProfile userProfile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "logoData", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    UserProfile toEntity(UserProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "logoData", ignore = true)
    @Mapping(target = "logoContentType", ignore = true)
    void updateEntity(@MappingTarget UserProfile userProfile, UserProfileRequest request);
}
