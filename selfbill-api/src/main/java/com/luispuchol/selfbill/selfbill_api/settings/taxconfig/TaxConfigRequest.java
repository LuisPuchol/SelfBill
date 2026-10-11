package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TaxConfigRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal vatPercentage,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal surchargePercentage) {
}
