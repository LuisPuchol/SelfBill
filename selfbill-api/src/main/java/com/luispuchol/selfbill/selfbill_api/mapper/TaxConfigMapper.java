package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;

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
