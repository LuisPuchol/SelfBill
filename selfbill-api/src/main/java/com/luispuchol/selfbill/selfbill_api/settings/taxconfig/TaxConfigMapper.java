package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface TaxConfigMapper {

    TaxConfigResponse toResponse(TaxConfig taxConfig);

    TaxConfig toEntity(TaxConfigRequest request);

    void updateEntity(@MappingTarget TaxConfig taxConfig, TaxConfigRequest request);
}
