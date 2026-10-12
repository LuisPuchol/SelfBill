package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TaxConfigRequest(
        @Schema(description = "VAT rate, as a percentage", example = "4")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal vatPercentage,

        @Schema(description = "Equivalence surcharge rate, as a percentage", example = "0.5")
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal surchargePercentage) {
}
