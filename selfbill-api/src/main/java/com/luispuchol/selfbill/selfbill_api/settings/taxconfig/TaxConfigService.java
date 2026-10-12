package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxConfigMapper taxConfigMapper;
    private final Clock clock;

    public TaxConfigResponse getTaxConfig() {
        return taxConfigMapper.toResponse(getInForceOn(LocalDate.now(clock)));
    }

    @Transactional
    public TaxConfigResponse createTaxConfig(TaxConfigRequest request) {
        if (taxConfigRepository.existsByValidFrom(request.validFrom())) {
            throw new BusinessException(ErrorCode.TAX_CONFIG_ALREADY_EXISTS, request.validFrom());
        }
        return taxConfigMapper.toResponse(taxConfigRepository.save(taxConfigMapper.toEntity(request)));
    }

    @Transactional
    public TaxConfigResponse updateTaxConfig(TaxConfigRequest request) {
        TaxConfig taxConfig = taxConfigRepository.findByValidFrom(request.validFrom())
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_CONFIG_NOT_FOUND, request.validFrom()));
        taxConfigMapper.updateEntity(taxConfig, request);
        return taxConfigMapper.toResponse(taxConfigRepository.save(taxConfig));
    }

    public TaxConfig getInForceOn(LocalDate date) {
        return taxConfigRepository.findFirstByValidFromLessThanEqualOrderByValidFromDesc(date)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_CONFIG_NOT_FOUND, date));
    }
}
