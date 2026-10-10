package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TaxConfigResponse(
        Integer id,
        BigDecimal vatPercentage,
        BigDecimal surchargePercentage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
