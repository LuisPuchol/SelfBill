package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxconfig.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.TaxConfigMapper;
import com.luispuchol.selfbill.selfbill_api.repository.TaxConfigRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxConfigMapper taxConfigMapper;

    public TaxConfigResponse getTaxConfig() {
        return taxConfigMapper.toResponse(getCurrent());
    }

    @Transactional
    public TaxConfigResponse createTaxConfig(TaxConfigRequest request) {
        if (taxConfigRepository.count() > 0) {
            throw new BusinessException(ErrorCode.TAX_CONFIG_ALREADY_EXISTS);
        }
        return taxConfigMapper.toResponse(taxConfigRepository.save(taxConfigMapper.toEntity(request)));
    }

    @Transactional
    public TaxConfigResponse updateTaxConfig(TaxConfigRequest request) {
        TaxConfig taxConfig = getCurrent();
        taxConfigMapper.updateEntity(taxConfig, request);
        return taxConfigMapper.toResponse(taxConfigRepository.save(taxConfig));
    }

    public TaxConfig getCurrent() {
        return taxConfigRepository.findFirstBy()
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_CONFIG_NOT_FOUND));
    }
}
