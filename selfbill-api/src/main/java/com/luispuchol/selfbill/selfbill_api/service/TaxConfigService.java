package com.luispuchol.selfbill.selfbill_api.service;

import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.entity.TaxConfig;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.TaxConfigMapper;
import com.luispuchol.selfbill.selfbill_api.repository.TaxConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxConfigService implements ITaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxConfigMapper taxConfigMapper;

    @Transactional(readOnly = true)
    @Override
    public TaxConfigResponse getTaxConfig() {
        return taxConfigMapper.toResponse(getExisting());
    }

    @Transactional
    @Override
    public TaxConfigResponse createTaxConfig(TaxConfigRequest request) {
        if (taxConfigRepository.count() > 0) {
            throw new BusinessException(ErrorCode.TAX_CONFIG_ALREADY_EXISTS);
        }
        TaxConfig saved = taxConfigRepository.save(taxConfigMapper.toEntity(request));
        return taxConfigMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public TaxConfigResponse updateTaxConfig(TaxConfigRequest request) {
        TaxConfig existing = getExisting();
        taxConfigMapper.updateEntity(existing, request);
        return taxConfigMapper.toResponse(taxConfigRepository.save(existing));
    }

    @Transactional(readOnly = true)
    @Override
    public TaxConfig getTaxConfigEntity() {
        return getExisting();
    }

    private TaxConfig getExisting() {
        return taxConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_CONFIG_NOT_FOUND));
    }
}
