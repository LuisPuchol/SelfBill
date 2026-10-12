package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record TaxConfigResponse(
        Integer id,
        BigDecimal vatPercentage,
        BigDecimal surchargePercentage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
