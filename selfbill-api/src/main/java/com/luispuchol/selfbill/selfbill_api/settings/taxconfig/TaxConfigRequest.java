package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TaxConfigRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal vatPercentage,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal surchargePercentage) {
}
