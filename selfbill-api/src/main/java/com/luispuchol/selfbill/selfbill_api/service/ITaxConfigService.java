package com.luispuchol.selfbill.selfbill_api.service;

import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;

public interface ITaxConfigService {

    TaxConfigResponse getTaxConfig();

    TaxConfigResponse createTaxConfig(TaxConfigRequest request);

    TaxConfigResponse updateTaxConfig(TaxConfigRequest request);

    TaxConfig getTaxConfigEntity();
}
