package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaxConfigMapper {

    TaxConfigResponse toResponse(TaxConfig taxConfig);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TaxConfig toEntity(TaxConfigRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget TaxConfig taxConfig, TaxConfigRequest request);
}
