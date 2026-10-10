package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface TaxConfigMapper {

    TaxConfigResponse toResponse(TaxConfig taxConfig);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TaxConfig toEntity(TaxConfigRequest request);

    @InheritConfiguration
    void updateEntity(@MappingTarget TaxConfig taxConfig, TaxConfigRequest request);
}
